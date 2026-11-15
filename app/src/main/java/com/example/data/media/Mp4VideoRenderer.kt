package com.example.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.local.FileStorageManager
import com.example.domain.model.StudioSettings
import com.example.domain.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.min

class Mp4VideoRenderer(private val fileStorageManager: FileStorageManager) {

    data class CapabilityInfo(
        val hasAvcEncoder: Boolean,
        val hasAacEncoder: Boolean,
        val encoderName: String
    )

    fun inspectCapabilities(): CapabilityInfo {
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        val avc = codecs.firstOrNull {
            it.isEncoder && it.supportedTypes.any { t -> t.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) }
        }
        val aac = codecs.firstOrNull {
            it.isEncoder && it.supportedTypes.any { t -> t.equals(MediaFormat.MIMETYPE_AUDIO_AAC, true) }
        }
        return CapabilityInfo(
            hasAvcEncoder = avc != null,
            hasAacEncoder = aac != null,
            encoderName = avc?.name ?: "Indisponível"
        )
    }

    suspend fun renderProjectToMp4(
        project: VideoProject,
        settings: StudioSettings,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        if (project.scenes.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Adicione pelo menos uma cena antes de renderizar."))
        }
        val totalSeconds = project.scenes.sumOf { it.durationSeconds.coerceAtLeast(1) }
        if (totalSeconds > 1800) {
            return@withContext Result.failure(IllegalStateException("O vídeo ultrapassa o limite de 30 minutos (1800s)."))
        }
        val missingImages = project.scenes.count { it.imagePath.isNullOrBlank() || !File(it.imagePath).exists() }
        if (missingImages > 0) {
            return@withContext Result.failure(
                IllegalStateException("Faltam imagens em $missingImages cena(s). Gere ou importe todas as imagens antes de renderizar.")
            )
        }

        val width = 1280
        val height = 720
        val fps = 15
        val outputDir = fileStorageManager.getProjectOutputDir(project.id)
        val outputFile = File(outputDir, "${fileStorageManager.safeFileName(project.title)}.mp4")
        if (outputFile.exists()) outputFile.delete()

        onProgress(0.02f, "Preparando renderizador MP4 (1280×720)...")

        // Optional watermark PNG
        val watermarkLogo: Bitmap? = settings.watermarkLogoPath
            ?.let { File(it) }
            ?.takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.absolutePath) }

        val tempAudioAac = if (!project.audioPath.isNullOrBlank() && File(project.audioPath).exists()) {
            onProgress(0.08f, "Codificando trilha de narração AAC...")
            transcodeAudioToAacFile(File(project.audioPath), File(outputDir, "narration_tmp.m4a"), totalSeconds)
        } else {
            null
        }

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var videoEncoder: MediaCodec? = null
        var audioExtractor: MediaExtractor? = null
        var muxerStarted = false

        try {
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 3_200_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }

            videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            videoEncoder.start()

            var audioTrackIndex = -1
            if (tempAudioAac != null && tempAudioAac.exists()) {
                audioExtractor = MediaExtractor().apply { setDataSource(tempAudioAac.absolutePath) }
                for (i in 0 until audioExtractor.trackCount) {
                    val fmt = audioExtractor.getTrackFormat(i)
                    if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                        audioExtractor.selectTrack(i)
                        audioTrackIndex = muxer.addTrack(fmt)
                        break
                    }
                }
            }

            val totalFrames = totalSeconds * fps
            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(frameBitmap)
            val argbBuffer = IntArray(width * height)
            val bufferInfo = MediaCodec.BufferInfo()

            var videoTrackIndex = -1
            var globalFrameIndex = 0
            var inputDone = false
            var outputDone = false

            // Flatten scene timeline
            val sceneFrames = mutableListOf<Triple<Int, Int, Int>>() // (sceneIndex, localFrame, sceneTotalFrames)
            project.scenes.forEachIndexed { idx, scene ->
                val count = (scene.durationSeconds.coerceAtLeast(1) * fps)
                for (f in 0 until count) {
                    sceneFrames.add(Triple(idx, f, count))
                }
            }

            // Cache decoded scene bitmap one at a time to save RAM
            var cachedSceneIdx = -1
            var cachedSceneBitmap: Bitmap? = null

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = videoEncoder.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        if (globalFrameIndex >= sceneFrames.size) {
                            videoEncoder.queueInputBuffer(
                                inIdx, 0, 0,
                                (globalFrameIndex.toLong() * 1_000_000L) / fps,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val (sceneIdx, localFrame, sceneTotalFrames) = sceneFrames[globalFrameIndex]
                            if (sceneIdx != cachedSceneIdx) {
                                cachedSceneBitmap?.recycle()
                                val imgFile = File(project.scenes[sceneIdx].imagePath!!)
                                cachedSceneBitmap = decodeScaledBitmap(imgFile, width, height)
                                cachedSceneIdx = sceneIdx
                            }

                            val progress = 0.15f + (0.78f * (globalFrameIndex.toFloat() / totalFrames.coerceAtLeast(1)))
                            if (globalFrameIndex % fps == 0) {
                                onProgress(
                                    progress,
                                    "Renderizando cena ${sceneIdx + 1}/${project.scenes.size}..."
                                )
                            }

                            val scene = project.scenes[sceneIdx]
                            val kenBurnsProgress = localFrame.toFloat() / sceneTotalFrames.coerceAtLeast(1)
                            drawCompositedFrame(
                                canvas = canvas,
                                width = width,
                                height = height,
                                sceneBitmap = cachedSceneBitmap,
                                kenBurnsT = kenBurnsProgress,
                                reversePan = sceneIdx % 2 == 1,
                                subtitle = scene.narration.ifBlank { scene.title },
                                watermarkText = settings.watermarkText.ifBlank { settings.channelName },
                                watermarkLogo = watermarkLogo,
                                showIllustrationBadge = project.illustrationBadge && project.pageModel.isFactual
                            )

                            val image = videoEncoder.getInputImage(inIdx)
                            if (image != null) {
                                frameBitmap.getPixels(argbBuffer, 0, width, 0, 0, width, height)
                                writeArgbToYuv420Image(argbBuffer, width, height, image)
                            }
                            val ptsUs = (globalFrameIndex.toLong() * 1_000_000L) / fps
                            videoEncoder.queueInputBuffer(inIdx, 0, width * height * 3 / 2, ptsUs, 0)
                            globalFrameIndex++
                        }
                    }
                }

                val outIdx = videoEncoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) throw IllegalStateException("Formato mudou após início do Muxer.")
                        videoTrackIndex = muxer.addTrack(videoEncoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outIdx >= 0 -> {
                        val encodedData = videoEncoder.getOutputBuffer(outIdx)
                        if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        videoEncoder.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            cachedSceneBitmap?.recycle()
            frameBitmap.recycle()

            // Copy audio samples into muxer if available
            if (muxerStarted && audioExtractor != null && audioTrackIndex >= 0) {
                onProgress(0.95f, "Combinando áudio e vídeo...")
                val maxAudioUs = totalSeconds.toLong() * 1_000_000L
                val audioBuf = ByteBuffer.allocate(256 * 1024)
                val audioInfo = MediaCodec.BufferInfo()
                while (true) {
                    val sampleSize = audioExtractor.readSampleData(audioBuf, 0)
                    if (sampleSize < 0) break
                    val pts = audioExtractor.sampleTime
                    if (pts > maxAudioUs) break
                    audioInfo.offset = 0
                    audioInfo.size = sampleSize
                    audioInfo.presentationTimeUs = pts
                    @Suppress("WrongConstant")
                    audioInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioTrackIndex, audioBuf, audioInfo)
                    audioExtractor.advance()
                }
            }

            onProgress(1f, "Vídeo MP4 pronto!")
            Result.success(outputFile)
        } catch (e: Exception) {
            if (outputFile.exists()) outputFile.delete()
            Result.failure(e)
        } finally {
            runCatching { videoEncoder?.stop() }
            runCatching { videoEncoder?.release() }
            runCatching { audioExtractor?.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
            watermarkLogo?.recycle()
            tempAudioAac?.delete()
        }
    }

    private fun decodeScaledBitmap(file: File, targetW: Int, targetH: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var sample = 1
        while (opts.outWidth / sample > targetW * 2 && opts.outHeight / sample > targetH * 2) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
        if (raw.width == targetW && raw.height == targetH) return raw
        val scaled = Bitmap.createScaledBitmap(raw, targetW, targetH, true)
        if (scaled !== raw) raw.recycle()
        return scaled
    }

    private fun drawCompositedFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        sceneBitmap: Bitmap?,
        kenBurnsT: Float,
        reversePan: Boolean,
        subtitle: String,
        watermarkText: String,
        watermarkLogo: Bitmap?,
        showIllustrationBadge: Boolean
    ) {
        canvas.drawColor(Color.BLACK)

        if (sceneBitmap != null) {
            // Smooth Ken Burns zoom 1.00 -> 1.08 + subtle horizontal pan
            val zoom = 1.0f + 0.08f * kenBurnsT
            val cropW = sceneBitmap.width / zoom
            val cropH = sceneBitmap.height / zoom
            val maxShiftX = (sceneBitmap.width - cropW).coerceAtLeast(0f)
            val panFraction = if (reversePan) 1f - kenBurnsT else kenBurnsT
            val left = maxShiftX * panFraction
            val top = (sceneBitmap.height - cropH) * 0.5f
            val srcRect = Rect(
                left.toInt(),
                top.toInt(),
                (left + cropW).toInt().coerceAtMost(sceneBitmap.width),
                (top + cropH).toInt().coerceAtMost(sceneBitmap.height)
            )
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(sceneBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        // Top-right "ILUSTRAÇÃO" pill for factual formats
        if (showIllustrationBadge) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(145, 10, 12, 18)
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(225, 255, 255, 255)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val label = "ILUSTRAÇÃO"
            val tw = textPaint.measureText(label)
            val rect = RectF(width - tw - 54f, 22f, width - 22f, 58f)
            canvas.drawRoundRect(rect, 10f, 10f, badgePaint)
            canvas.drawText(label, rect.left + 16f, rect.top + 25f, textPaint)
        }

        // Bottom subtitle bar
        val cleanSub = subtitle.replace(Regex("\\s+"), " ").trim().take(180)
        if (cleanSub.isNotEmpty()) {
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(4f, 0f, 2f, Color.BLACK)
            }
            val maxSubW = width - 180
            val layout = StaticLayout.Builder
                .obtain(cleanSub, 0, cleanSub.length, subPaint, maxSubW)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(3)
                .build()
            val boxTop = (height - layout.height - 64).toFloat()
            val boxRect = RectF(72f, boxTop - 16f, width - 72f, (height - 44).toFloat())
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(165, 10, 12, 18)
            }
            canvas.drawRoundRect(boxRect, 14f, 14f, boxPaint)
            canvas.save()
            canvas.translate(90f, boxTop)
            layout.draw(canvas)
            canvas.restore()
        }

        // Channel watermark text or logo at bottom-right
        if (watermarkLogo != null) {
            val targetLogoW = 120f
            val ratio = targetLogoW / watermarkLogo.width.toFloat()
            val targetLogoH = watermarkLogo.height * ratio
            val dst = RectF(
                width - targetLogoW - 26f,
                height - targetLogoH - 22f,
                width - 26f,
                height - 22f
            )
            val alphaPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 215 }
            canvas.drawBitmap(watermarkLogo, null, dst, alphaPaint)
        } else if (watermarkText.isNotBlank()) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(195, 255, 255, 255)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tw = wmPaint.measureText(watermarkText)
            val wmBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(110, 0, 0, 0) }
            val wmRect = RectF(width - tw - 48f, height - 54f, width - 20f, height - 18f)
            canvas.drawRoundRect(wmRect, 8f, 8f, wmBg)
            canvas.drawText(watermarkText, wmRect.left + 14f, wmRect.top + 25f, wmPaint)
        }
    }

    private fun writeArgbToYuv420Image(argb: IntArray, width: Int, height: Int, image: Image) {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        for (j in 0 until height) {
            val yRowOffset = j * yRowStride
            val srcRowOffset = j * width
            val uvRowOffset = (j shr 1) * uvRowStride
            for (i in 0 until width) {
                val c = argb[srcRowOffset + i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                val yVal = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuf.put(yRowOffset + i * yPixelStride, yVal.coerceIn(0, 255).toByte())

                if ((j and 1) == 0 && (i and 1) == 0) {
                    val uVal = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val vVal = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    val uvOffset = uvRowOffset + (i shr 1) * uvPixelStride
                    uBuf.put(uvOffset, uVal.coerceIn(0, 255).toByte())
                    vBuf.put(uvOffset, vVal.coerceIn(0, 255).toByte())
                }
            }
        }
    }

    private fun transcodeAudioToAacFile(inputFile: File, outputFile: File, maxSeconds: Int): File? {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        return try {
            extractor.setDataSource(inputFile.absolutePath)
            var trackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    inputFormat = fmt
                    break
                }
            }
            if (trackIndex < 0 || inputFormat == null) return null
            extractor.selectTrack(trackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null
            val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // If input is already AAC in M4A, we can mux directly
            if (mime.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) && inputFile.extension.equals("m4a", true)) {
                inputFile.copyTo(outputFile, overwrite = true)
                return outputFile
            }

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val outSampleRate = sampleRate.coerceIn(8000, 48000)
            val outChannels = min(channelCount, 2).coerceAtLeast(1)
            val encFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, outSampleRate, outChannels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            if (outputFile.exists()) outputFile.delete()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val decInfo = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            var decInputDone = false
            var decOutputDone = false
            var encOutputDone = false
            var muxTrack = -1
            val maxUs = maxSeconds.toLong() * 1_000_000L
            var totalPcmBytes = 0L

            while (!encOutputDone) {
                if (!decInputDone) {
                    val inIdx = decoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        val size = extractor.readSampleData(buf, 0)
                        val pts = extractor.sampleTime
                        if (size < 0 || pts > maxUs) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            decInputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, size, pts, 0)
                            extractor.advance()
                        }
                    }
                }

                if (!decOutputDone) {
                    val outIdx = decoder.dequeueOutputBuffer(decInfo, 5_000L)
                    if (outIdx >= 0) {
                        val pcmBuf = decoder.getOutputBuffer(outIdx)
                        if (pcmBuf != null && decInfo.size > 0) {
                            val bytes = ByteArray(decInfo.size)
                            pcmBuf.position(decInfo.offset)
                            pcmBuf.get(bytes)

                            var offset = 0
                            while (offset < bytes.size) {
                                val encInIdx = encoder.dequeueInputBuffer(10_000L)
                                if (encInIdx >= 0) {
                                    val encInBuf = encoder.getInputBuffer(encInIdx)!!
                                    encInBuf.clear()
                                    val chunk = min(encInBuf.remaining(), bytes.size - offset)
                                    encInBuf.put(bytes, offset, chunk)
                                    val ptsUs = (totalPcmBytes * 1_000_000L) / (outSampleRate * outChannels * 2L)
                                    encoder.queueInputBuffer(encInIdx, 0, chunk, ptsUs, 0)
                                    totalPcmBytes += chunk
                                    offset += chunk
                                }
                            }
                        }
                        decoder.releaseOutputBuffer(outIdx, false)
                        if ((decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            decOutputDone = true
                            val encInIdx = encoder.dequeueInputBuffer(10_000L)
                            if (encInIdx >= 0) {
                                val ptsUs = (totalPcmBytes * 1_000_000L) / (outSampleRate * outChannels * 2L)
                                encoder.queueInputBuffer(encInIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            }
                        }
                    }
                }

                while (true) {
                    val encOutIdx = encoder.dequeueOutputBuffer(encInfo, 2_000L)
                    if (encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            muxTrack = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (encOutIdx >= 0) {
                        val encOutBuf = encoder.getOutputBuffer(encOutIdx)
                        if (encOutBuf != null && (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && encInfo.size > 0 && muxerStarted) {
                            encOutBuf.position(encInfo.offset)
                            encOutBuf.limit(encInfo.offset + encInfo.size)
                            muxer.writeSampleData(muxTrack, encOutBuf, encInfo)
                        }
                        encoder.releaseOutputBuffer(encOutIdx, false)
                        if ((encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encOutputDone = true
                            break
                        }
                    } else {
                        break
                    }
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) outputFile else null
        } catch (_: Exception) {
            null
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            if (muxerStarted) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }
}

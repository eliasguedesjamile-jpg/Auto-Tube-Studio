package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileStorageManager(private val context: Context) {

    private val projectsRoot: File
        get() = File(context.filesDir, "projects").apply { mkdirs() }

    private val dataRoot: File
        get() = File(context.filesDir, "data").apply { mkdirs() }

    fun getProjectDir(projectId: String): File =
        File(projectsRoot, safeId(projectId)).apply { mkdirs() }

    fun getProjectImagesDir(projectId: String): File =
        File(getProjectDir(projectId), "images").apply { mkdirs() }

    fun getProjectAudioDir(projectId: String): File =
        File(getProjectDir(projectId), "audio").apply { mkdirs() }

    fun getProjectEffectsDir(projectId: String): File =
        File(getProjectDir(projectId), "effects").apply { mkdirs() }

    fun getProjectOutputDir(projectId: String): File =
        File(getProjectDir(projectId), "saida").apply { mkdirs() }

    fun getWatermarkFile(): File = File(dataRoot, "watermark.png")

    suspend fun saveSceneBitmap(projectId: String, sceneId: String, bitmap: Bitmap): File =
        withContext(Dispatchers.IO) {
            val file = File(getProjectImagesDir(projectId), "${safeId(sceneId)}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            file
        }

    suspend fun importSceneImageFromUri(projectId: String, sceneId: String, uri: Uri): File =
        withContext(Dispatchers.IO) {
            val file = File(getProjectImagesDir(projectId), "${safeId(sceneId)}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val raw = BitmapFactory.decodeStream(input)
                    ?: throw IllegalStateException("Não foi possível decodificar a imagem selecionada.")
                val scaled = scaleToLandscape16x9(raw, 1280, 720)
                FileOutputStream(file).use { out ->
                    scaled.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                if (scaled !== raw) raw.recycle()
                scaled.recycle()
            } ?: throw IllegalStateException("Não foi possível abrir o arquivo de imagem.")
            file
        }

    suspend fun importWatermarkPngFromUri(uri: Uri): File = withContext(Dispatchers.IO) {
        val file = getWatermarkFile()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
                ?: throw IllegalStateException("Imagem de logotipo inválida.")
            val maxWidth = 420
            val finalBmp = if (bitmap.width > maxWidth) {
                val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
                Bitmap.createScaledBitmap(bitmap, maxWidth, (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
            } else {
                bitmap
            }
            FileOutputStream(file).use { out ->
                finalBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (finalBmp !== bitmap) bitmap.recycle()
            finalBmp.recycle()
        } ?: throw IllegalStateException("Não foi possível ler o logotipo selecionado.")
        file
    }

    suspend fun copyAudioUriToProject(
        projectId: String,
        uri: Uri,
        subFolder: String,
        targetNamePrefix: String
    ): Pair<File, String> = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(uri) ?: "$targetNamePrefix.mp3"
        val ext = displayName.substringAfterLast('.', "mp3")
            .lowercase()
            .takeIf { it in setOf("mp3", "wav", "m4a", "aac", "ogg") } ?: "mp3"
        val dir = if (subFolder == "effects") getProjectEffectsDir(projectId) else getProjectAudioDir(projectId)
        val outFile = File(dir, "${safeId(targetNamePrefix)}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Não foi possível copiar o arquivo de áudio.")
        outFile to displayName
    }

    suspend fun writeSrtFile(projectId: String, projectTitle: String, srtContent: String): File =
        withContext(Dispatchers.IO) {
            val outFile = File(getProjectOutputDir(projectId), "${safeFileName(projectTitle)}.srt")
            outFile.writeText(srtContent, Charsets.UTF_8)
            outFile
        }

    suspend fun saveVideoToDeviceGallery(videoFile: File, title: String): Uri? =
        withContext(Dispatchers.IO) {
            if (!videoFile.exists()) return@withContext null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "${safeFileName(title)}.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AutoTubeStudio")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        videoFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } else {
                null
            }
        }

    fun deleteProjectFolder(projectId: String) {
        getProjectDir(projectId).deleteRecursively()
    }

    fun shareFile(file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openVideoFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun totalStorageUsedMb(): Double {
        val bytes = walkSize(projectsRoot) + walkSize(dataRoot)
        return bytes / (1024.0 * 1024.0)
    }

    private fun walkSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { walkSize(it) } ?: 0L
    }

    private fun resolveDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    private fun scaleToLandscape16x9(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val srcRatio = src.width.toFloat() / src.height.toFloat()
        val dstRatio = targetW.toFloat() / targetH.toFloat()
        val drawRect = if (srcRatio > dstRatio) {
            val scaledW = targetH * srcRatio
            val left = (targetW - scaledW) / 2f
            android.graphics.RectF(left, 0f, left + scaledW, targetH.toFloat())
        } else {
            val scaledH = targetW / srcRatio
            val top = (targetH - scaledH) / 2f
            android.graphics.RectF(0f, top, targetW.toFloat(), top + scaledH)
        }
        canvas.drawBitmap(src, null, drawRect, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        return output
    }

    fun safeFileName(raw: String): String =
        raw.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "video-auto-tube" }
            .take(48)

    private fun safeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "item" }
}

package com.example.data.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.domain.model.AndroidTtsVoiceInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Locale
import java.util.UUID

class AndroidTtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var initDeferred: CompletableDeferred<Boolean>? = null

    suspend fun ensureInitialized(): Boolean = withContext(Dispatchers.Main) {
        if (tts != null && initDeferred?.isCompleted == true && initDeferred?.await() == true) {
            return@withContext true
        }
        val deferred = CompletableDeferred<Boolean>()
        initDeferred = deferred
        tts?.shutdown()
        tts = TextToSpeech(context.applicationContext) { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) {
                val ptBr = Locale("pt", "BR")
                val res = tts?.setLanguage(ptBr)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
            }
            if (!deferred.isCompleted) deferred.complete(ok)
        }
        runCatching { withTimeout(8_000L) { deferred.await() } }.getOrDefault(false)
    }

    suspend fun listAvailableVoices(): List<AndroidTtsVoiceInfo> = withContext(Dispatchers.Main) {
        if (!ensureInitialized()) return@withContext emptyList()
        val voices = tts?.voices.orEmpty()
        voices
            .sortedWith(
                compareByDescending<android.speech.tts.Voice> {
                    it.locale.language.equals("pt", ignoreCase = true)
                }.thenByDescending {
                    it.locale.country.equals("BR", ignoreCase = true)
                }.thenBy { it.isNetworkConnectionRequired }
                    .thenBy { it.name }
            )
            .take(40)
            .map { v ->
                AndroidTtsVoiceInfo(
                    name = v.name,
                    localeLabel = "${v.locale.displayLanguage} (${v.locale.country.ifBlank { v.locale.language }})",
                    isNetworkRequired = v.isNetworkConnectionRequired,
                    qualityLabel = when {
                        v.quality >= android.speech.tts.Voice.QUALITY_VERY_HIGH -> "Alta"
                        v.quality >= android.speech.tts.Voice.QUALITY_NORMAL -> "Normal"
                        else -> "Básica"
                    }
                )
            }
    }

    suspend fun speakPreview(text: String, voiceName: String): Result<Unit> = withContext(Dispatchers.Main) {
        if (!ensureInitialized()) {
            return@withContext Result.failure(IllegalStateException("Motor TTS do Android indisponível neste aparelho."))
        }
        applyVoice(voiceName)
        val utteranceId = "preview-${UUID.randomUUID()}"
        val code = tts?.speak(text.take(400), TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (code == TextToSpeech.SUCCESS) Result.success(Unit)
        else Result.failure(IllegalStateException("Falha ao reproduzir prévia de voz."))
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    suspend fun synthesizeToWavFile(
        text: String,
        voiceName: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.Main) {
        if (text.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Não há texto de narração para sintetizar."))
        }
        if (!ensureInitialized()) {
            return@withContext Result.failure(IllegalStateException("Motor TTS do Android indisponível."))
        }
        applyVoice(voiceName)
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val done = CompletableDeferred<Result<File>>()
        val utteranceId = "synth-${UUID.randomUUID()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId && !done.isCompleted) {
                    if (outputFile.exists() && outputFile.length() > 44) {
                        done.complete(Result.success(outputFile))
                    } else {
                        done.complete(Result.failure(IllegalStateException("Arquivo WAV gerado está vazio.")))
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId && !done.isCompleted) {
                    done.complete(Result.failure(IllegalStateException("Erro ao sintetizar áudio TTS.")))
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId && !done.isCompleted) {
                    done.complete(Result.failure(IllegalStateException("Erro TTS código $errorCode.")))
                }
            }
        })

        // TextToSpeech has a ~4000-char limit per synthesizeToFile call
        val boundedText = text.take(3900)
        val params = Bundle()
        val enqueueResult = tts?.synthesizeToFile(boundedText, params, outputFile, utteranceId)
        if (enqueueResult != TextToSpeech.SUCCESS) {
            return@withContext Result.failure(IllegalStateException("Não foi possível iniciar a síntese offline."))
        }

        runCatching { withTimeout(60_000L) { done.await() } }
            .getOrElse { Result.failure(IllegalStateException("Tempo esgotado na síntese de narração.")) }
    }

    private fun applyVoice(voiceName: String) {
        if (voiceName.isBlank()) return
        val match = tts?.voices?.firstOrNull { it.name == voiceName }
        if (match != null) {
            tts?.voice = match
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

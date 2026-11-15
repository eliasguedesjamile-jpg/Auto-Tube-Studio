package com.example.data.media

import com.example.domain.model.ElevenSubscription
import com.example.domain.model.ElevenVoice
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ElevenSubscriptionDto(
    val tier: String? = null,
    @Json(name = "character_count") val characterCount: Int? = null,
    @Json(name = "character_limit") val characterLimit: Int? = null
)

@JsonClass(generateAdapter = true)
data class ElevenVoiceDto(
    @Json(name = "voice_id") val voiceId: String,
    val name: String,
    val category: String? = null,
    @Json(name = "preview_url") val previewUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ElevenVoicesResponseDto(
    val voices: List<ElevenVoiceDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ElevenTtsRequestDto(
    val text: String,
    @Json(name = "model_id") val modelId: String
)

class ElevenLabsClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val subAdapter = moshi.adapter(ElevenSubscriptionDto::class.java)
    private val voicesAdapter = moshi.adapter(ElevenVoicesResponseDto::class.java)
    private val ttsRequestAdapter = moshi.adapter(ElevenTtsRequestDto::class.java)

    suspend fun fetchSubscription(apiKey: String): Result<ElevenSubscription> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Informe sua chave API da ElevenLabs nos Ajustes."))
        }
        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/user/subscription")
            .header("xi-api-key", apiKey.trim())
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("ElevenLabs retornou HTTP ${response.code}: ${raw.take(200)}")
                }
                val dto = subAdapter.fromJson(raw)
                    ?: throw IllegalStateException("Resposta inválida da ElevenLabs.")
                ElevenSubscription(
                    tier = dto.tier ?: "free",
                    characterCount = dto.characterCount ?: 0,
                    characterLimit = dto.characterLimit ?: 0
                )
            }
        }
    }

    suspend fun fetchVoices(apiKey: String): Result<List<ElevenVoice>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Informe sua chave API da ElevenLabs nos Ajustes."))
        }
        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/voices")
            .header("xi-api-key", apiKey.trim())
            .get()
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("ElevenLabs retornou HTTP ${response.code}: ${raw.take(200)}")
                }
                val dto = voicesAdapter.fromJson(raw)
                    ?: throw IllegalStateException("Lista de vozes inválida.")
                dto.voices.map {
                    ElevenVoice(
                        voiceId = it.voiceId,
                        name = it.name,
                        category = it.category ?: "custom",
                        previewUrl = it.previewUrl
                    )
                }
            }
        }
    }

    suspend fun synthesizeToMp3(
        apiKey: String,
        voiceId: String,
        modelId: String,
        text: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || voiceId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Configure a chave e selecione uma voz ElevenLabs."))
        }
        if (text.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Texto de narração vazio."))
        }
        val payload = ttsRequestAdapter.toJson(
            ElevenTtsRequestDto(
                text = text.take(4500),
                modelId = modelId.ifBlank { "eleven_multilingual_v2" }
            )
        )
        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/${voiceId.trim()}")
            .header("xi-api-key", apiKey.trim())
            .header("Accept", "audio/mpeg")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string().orEmpty()
                    throw IllegalStateException("Falha ElevenLabs HTTP ${response.code}: ${err.take(200)}")
                }
                outputFile.parentFile?.mkdirs()
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Áudio vazio retornado pela ElevenLabs.")
                outputFile
            }
        }
    }
}

package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SceneItem(
    val id: String,
    val number: Int,
    val title: String,
    val narration: String,
    val visualPrompt: String,
    val durationSeconds: Int = 8,
    val imagePath: String? = null,
    val isPlaceholderImage: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ChapterItem(
    val title: String,
    val startScene: Int
)

@JsonClass(generateAdapter = true)
data class SourceItem(
    val id: String,
    val title: String,
    val publisher: String,
    val url: String,
    val publishedAt: String = "",
    val supports: String
)

@JsonClass(generateAdapter = true)
data class SoundEffectItem(
    val id: String,
    val label: String,
    val filename: String,
    val filePath: String,
    val startSeconds: Float = 0f,
    val volumePercent: Int = 25,
    val sourceLibrary: String = "",
    val author: String = "",
    val licenseUrl: String = "",
    val licenseTerms: String = "",
    val commercialUseConfirmed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PublicationMetadata(
    val titles: List<String> = emptyList(),
    val description: String = "",
    val tags: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val socialCaption: String = ""
)

data class VideoProject(
    val id: String,
    val title: String,
    val pageTypeId: String,
    val targetDurationMinutes: Int,
    val fullScript: String,
    val logline: String = "",
    val chapters: List<ChapterItem> = emptyList(),
    val aiNotes: List<String> = emptyList(),
    val scenes: List<SceneItem> = emptyList(),
    val sources: List<SourceItem> = emptyList(),
    val soundEffects: List<SoundEffectItem> = emptyList(),
    val publication: PublicationMetadata = PublicationMetadata(),
    val illustrationBadge: Boolean = true,
    val audioPath: String? = null,
    val audioSourceLabel: String? = null,
    val renderedVideoPath: String? = null,
    val renderedAtMillis: Long? = null,
    val seriesId: String? = null,
    val episodeNumber: Int? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val totalSceneDurationSeconds: Int
        get() = scenes.sumOf { it.durationSeconds.coerceAtLeast(1) }

    val pageModel: PageModel
        get() = PageModelsCatalog.findById(pageTypeId)
}

@JsonClass(generateAdapter = true)
data class SeriesEpisode(
    val id: String,
    val number: Int,
    val title: String,
    val fullScript: String,
    val linkedProjectId: String? = null
)

data class SeriesProject(
    val id: String,
    val name: String,
    val pageTypeId: String,
    val episodeDurationMinutes: Int,
    val bible: String,
    val episodes: List<SeriesEpisode>,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

enum class NarrationProvider {
    ANDROID_TTS,
    ELEVENLABS,
    USER_AUDIO
}

data class StudioSettings(
    val channelName: String = "",
    val watermarkText: String = "",
    val watermarkLogoPath: String? = null,
    val imageModelId: String = "gemini-nano-banana-2.1",
    val narrationProvider: NarrationProvider = NarrationProvider.ANDROID_TTS,
    val androidTtsVoiceName: String = "",
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "",
    val elevenLabsVoiceName: String = "",
    val elevenLabsModel: String = "eleven_multilingual_v2",
    val licenseNote: String = ""
)

data class AiAnalysisResult(
    val logline: String,
    val chapters: List<ChapterItem>,
    val notes: List<String>,
    val scenes: List<SceneItem>,
    val isPlaceholder: Boolean
)

sealed class AiResult<out T> {
    data class Success<T>(val data: T, val isDemoPlaceholder: Boolean = false, val notice: String? = null) : AiResult<T>()
    data class Error(val message: String) : AiResult<Nothing>()
}

data class ElevenVoice(
    val voiceId: String,
    val name: String,
    val category: String,
    val previewUrl: String?
)

data class ElevenSubscription(
    val tier: String,
    val characterCount: Int,
    val characterLimit: Int
)

data class AndroidTtsVoiceInfo(
    val name: String,
    val localeLabel: String,
    val isNetworkRequired: Boolean,
    val qualityLabel: String
)

data class DiagnosticCheck(
    val title: String,
    val detail: String,
    val ok: Boolean
)

data class RenderProgress(
    val isRendering: Boolean = false,
    val progressFraction: Float = 0f,
    val stageLabel: String = ""
)

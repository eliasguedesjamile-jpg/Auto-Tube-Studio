package com.example.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.ai.AiStudioRepository
import com.example.data.ai.PlaceholderAiStudioRepository
import com.example.data.local.FileStorageManager
import com.example.data.local.SettingsDataStore
import com.example.data.local.StudioDao
import com.example.data.local.StudioDatabase
import com.example.data.local.StudioJsonMapper
import com.example.data.media.AndroidTtsEngine
import com.example.data.media.ElevenLabsClient
import com.example.data.media.Mp4VideoRenderer
import com.example.data.media.SubtitleExporter
import com.example.domain.model.AiAnalysisResult
import com.example.domain.model.AiResult
import com.example.domain.model.AndroidTtsVoiceInfo
import com.example.domain.model.DiagnosticCheck
import com.example.domain.model.ElevenSubscription
import com.example.domain.model.ElevenVoice
import com.example.domain.model.PublicationMetadata
import com.example.domain.model.SceneItem
import com.example.domain.model.SeriesProject
import com.example.domain.model.SoundEffectItem
import com.example.domain.model.StudioSettings
import com.example.domain.model.VideoProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Locale
import java.util.UUID

class StudioRepository(private val context: Context) {

    private val dao: StudioDao = StudioDatabase.getInstance(context).studioDao()
    private val settingsStore = SettingsDataStore(context)
    val fileStorage = FileStorageManager(context)
    val aiRepository: AiStudioRepository = PlaceholderAiStudioRepository(fileStorage)
    val androidTts = AndroidTtsEngine(context)
    val elevenLabs = ElevenLabsClient()
    val mp4Renderer = Mp4VideoRenderer(fileStorage)

    private var activeMediaPlayer: MediaPlayer? = null

    val projectsFlow: Flow<List<VideoProject>> = dao.observeProjects()
        .map { list -> list.map { StudioJsonMapper.toDomain(it) } }

    val seriesFlow: Flow<List<SeriesProject>> = dao.observeSeries()
        .map { list -> list.map { StudioJsonMapper.toDomain(it) } }

    val settingsFlow: Flow<StudioSettings> = settingsStore.settingsFlow

    suspend fun getProject(id: String): VideoProject? =
        dao.getProjectById(id)?.let { StudioJsonMapper.toDomain(it) }

    suspend fun saveProject(project: VideoProject): VideoProject {
        val updated = project.copy(updatedAtMillis = System.currentTimeMillis())
        dao.upsertProject(StudioJsonMapper.toEntity(updated))
        return updated
    }

    suspend fun createProject(
        title: String,
        pageTypeId: String,
        durationMinutes: Int,
        fullScript: String = "",
        illustrationBadge: Boolean = true,
        seriesId: String? = null,
        episodeNumber: Int? = null
    ): VideoProject {
        val id = UUID.randomUUID().toString()
        val cleanTitle = title.trim().ifBlank { "Novo Vídeo" }
        val project = VideoProject(
            id = id,
            title = cleanTitle,
            pageTypeId = pageTypeId,
            targetDurationMinutes = durationMinutes.coerceIn(1, 30),
            fullScript = fullScript,
            illustrationBadge = illustrationBadge,
            seriesId = seriesId,
            episodeNumber = episodeNumber
        )
        fileStorage.getProjectDir(id)
        dao.upsertProject(StudioJsonMapper.toEntity(project))
        return project
    }

    suspend fun deleteProject(projectId: String) {
        dao.deleteProjectById(projectId)
        fileStorage.deleteProjectFolder(projectId)
    }

    suspend fun saveSeries(series: SeriesProject): SeriesProject {
        val updated = series.copy(updatedAtMillis = System.currentTimeMillis())
        dao.upsertSeries(StudioJsonMapper.toEntity(updated))
        return updated
    }

    suspend fun deleteSeries(seriesId: String) {
        dao.deleteSeriesById(seriesId)
    }

    suspend fun updateSettings(transform: (StudioSettings) -> StudioSettings) {
        settingsStore.updateSettings(transform)
    }

    suspend fun analyzeScript(project: VideoProject): AiResult<AiAnalysisResult> {
        return aiRepository.analyzeScriptToScenes(
            projectTitle = project.title,
            pageTypeId = project.pageTypeId,
            targetDurationMinutes = project.targetDurationMinutes,
            fullScript = project.fullScript
        )
    }

    suspend fun generateSceneImage(
        project: VideoProject,
        scene: SceneItem,
        continuityImagePath: String?
    ): AiResult<String> {
        return aiRepository.generateSceneImage(
            projectId = project.id,
            scene = scene,
            pageTypeId = project.pageTypeId,
            continuityImagePath = continuityImagePath
        )
    }

    suspend fun generatePublicationCopy(project: VideoProject): AiResult<PublicationMetadata> {
        return aiRepository.generatePublicationCopy(
            projectTitle = project.title,
            pageTypeId = project.pageTypeId,
            logline = project.logline,
            fullScript = project.fullScript
        )
    }

    suspend fun importScenePhoto(projectId: String, sceneId: String, uri: Uri): File {
        return fileStorage.importSceneImageFromUri(projectId, sceneId, uri)
    }

    suspend fun importWatermarkLogo(uri: Uri): File {
        val file = fileStorage.importWatermarkPngFromUri(uri)
        updateSettings { it.copy(watermarkLogoPath = file.absolutePath) }
        return file
    }

    suspend fun importProjectAudio(projectId: String, uri: Uri): Pair<File, String> {
        return fileStorage.copyAudioUriToProject(projectId, uri, "audio", "narracao_importada")
    }

    suspend fun importSfxAudio(projectId: String, sfxId: String, uri: Uri): Pair<File, String> {
        return fileStorage.copyAudioUriToProject(projectId, uri, "effects", "sfx_$sfxId")
    }

    suspend fun exportSrt(project: VideoProject): File {
        val content = SubtitleExporter.generateSrt(project)
        return fileStorage.writeSrtFile(project.id, project.title, content)
    }

    suspend fun renderProjectMp4(
        project: VideoProject,
        onProgress: (Float, String) -> Unit
    ): Result<File> {
        val settings = settingsFlow.first()
        val result = mp4Renderer.renderProjectToMp4(project, settings, onProgress)
        if (result.isSuccess) {
            val file = result.getOrThrow()
            fileStorage.saveVideoToDeviceGallery(file, project.title)
        }
        return result
    }

    suspend fun listAndroidTtsVoices(): List<AndroidTtsVoiceInfo> =
        androidTts.listAvailableVoices()

    suspend fun previewAndroidTts(text: String, voiceName: String): Result<Unit> =
        androidTts.speakPreview(text, voiceName)

    suspend fun synthesizeProjectWithAndroidTts(
        project: VideoProject,
        voiceName: String
    ): Result<File> {
        val fullNarration = project.scenes
            .mapNotNull { it.narration.trim().takeIf(String::isNotEmpty) }
            .joinToString("\n\n")
            .ifBlank { project.fullScript.trim() }
        val outFile = File(fileStorage.getProjectAudioDir(project.id), "narracao_android_tts.wav")
        return androidTts.synthesizeToWavFile(fullNarration, voiceName, outFile)
    }

    suspend fun authenticateElevenLabs(apiKey: String): Result<ElevenSubscription> =
        elevenLabs.fetchSubscription(apiKey)

    suspend fun fetchElevenLabsVoices(apiKey: String): Result<List<ElevenVoice>> =
        elevenLabs.fetchVoices(apiKey)

    suspend fun synthesizeProjectWithElevenLabs(
        project: VideoProject,
        apiKey: String,
        voiceId: String,
        modelId: String
    ): Result<File> {
        val fullNarration = project.scenes
            .mapNotNull { it.narration.trim().takeIf(String::isNotEmpty) }
            .joinToString("\n\n")
            .ifBlank { project.fullScript.trim() }
        val outFile = File(fileStorage.getProjectAudioDir(project.id), "narracao_elevenlabs.mp3")
        return elevenLabs.synthesizeToMp3(apiKey, voiceId, modelId, fullNarration, outFile)
    }

    fun playAudioFile(path: String, onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stopAudioPlayback()
        try {
            val player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener {
                    stopAudioPlayback()
                    onComplete()
                }
                start()
            }
            activeMediaPlayer = player
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Não foi possível reproduzir o áudio.")
        }
    }

    fun playAudioUrl(url: String, onComplete: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stopAudioPlayback()
        try {
            val player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    stopAudioPlayback()
                    onComplete()
                }
                setOnErrorListener { _, _, _ ->
                    stopAudioPlayback()
                    onError("Erro ao reproduzir amostra de áudio.")
                    true
                }
                prepareAsync()
            }
            activeMediaPlayer = player
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Erro ao abrir URL de áudio.")
        }
    }

    fun stopAudioPlayback() {
        runCatching {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
        }
        activeMediaPlayer = null
    }

    suspend fun runDiagnostics(): List<DiagnosticCheck> {
        val ttsReady = androidTts.ensureInitialized()
        val voices = if (ttsReady) androidTts.listAvailableVoices() else emptyList()
        val ptVoices = voices.count { it.localeLabel.contains("portugu", ignoreCase = true) }
        val codecs = mp4Renderer.inspectCapabilities()
        val usedMb = fileStorage.totalStorageUsedMb()
        val settings = settingsFlow.first()

        return listOf(
            DiagnosticCheck(
                title = "Renderizador MP4 nativo (H.264 + AAC)",
                detail = if (codecs.hasAvcEncoder && codecs.hasAacEncoder)
                    "Ativo (${codecs.encoderName}) — sem FFmpeg externo"
                else
                    "Codificador de mídia limitado neste dispositivo",
                ok = codecs.hasAvcEncoder && codecs.hasAacEncoder
            ),
            DiagnosticCheck(
                title = "Narração offline Android TTS",
                detail = if (ttsReady)
                    "${voices.size} vozes instaladas ($ptVoices em português)"
                else
                    "Motor TTS não respondeu; use áudio próprio ou ElevenLabs",
                ok = ttsReady
            ),
            DiagnosticCheck(
                title = "Firebase AI Logic (Gemini)",
                detail = if (aiRepository.isFirebaseConnected)
                    "Conectado"
                else
                    "Modo Demonstração (Firebase não ativado; geração usa amostras rotuladas)",
                ok = aiRepository.isFirebaseConnected
            ),
            DiagnosticCheck(
                title = "ElevenLabs (opcional)",
                detail = if (settings.elevenLabsApiKey.isNotBlank())
                    "Chave configurada localmente"
                else
                    "Não configurada (opcional para vozes em nuvem)",
                ok = settings.elevenLabsApiKey.isNotBlank()
            ),
            DiagnosticCheck(
                title = "Armazenamento local do estúdio",
                detail = String.format(Locale.US, "%.1f MB utilizados em projetos e mídias", usedMb),
                ok = true
            )
        )
    }

    fun release() {
        stopAudioPlayback()
        androidTts.release()
    }
}

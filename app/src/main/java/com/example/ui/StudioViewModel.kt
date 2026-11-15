package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.StudioRepository
import com.example.domain.model.AiResult
import com.example.domain.model.AndroidTtsVoiceInfo
import com.example.domain.model.DiagnosticCheck
import com.example.domain.model.ElevenSubscription
import com.example.domain.model.ElevenVoice
import com.example.domain.model.NarrationProvider
import com.example.domain.model.PageModelsCatalog
import com.example.domain.model.PublicationMetadata
import com.example.domain.model.RenderProgress
import com.example.domain.model.SceneItem
import com.example.domain.model.SeriesEpisode
import com.example.domain.model.SeriesProject
import com.example.domain.model.SoundEffectItem
import com.example.domain.model.SourceItem
import com.example.domain.model.StudioSettings
import com.example.domain.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class StudioUiState(
    val projects: List<VideoProject> = emptyList(),
    val seriesList: List<SeriesProject> = emptyList(),
    val settings: StudioSettings = StudioSettings(),
    val activeProjectId: String? = null,
    val selectedSceneId: String? = null,
    val isFirebaseAiConnected: Boolean = false,
    val aiDemoNotice: String? = null,
    val isAnalyzingScript: Boolean = false,
    val isGeneratingImage: Boolean = false,
    val batchGenerationProgress: String? = null,
    val isSynthesizingVoice: Boolean = false,
    val androidVoices: List<AndroidTtsVoiceInfo> = emptyList(),
    val elevenVoices: List<ElevenVoice> = emptyList(),
    val elevenSubscription: ElevenSubscription? = null,
    val diagnostics: List<DiagnosticCheck> = emptyList(),
    val renderProgress: RenderProgress = RenderProgress(),
    val statusBanner: String? = null,
    val isErrorBanner: Boolean = false
) {
    val activeProject: VideoProject?
        get() = projects.firstOrNull { it.id == activeProjectId } ?: projects.firstOrNull()

    val selectedScene: SceneItem?
        get() {
            val proj = activeProject ?: return null
            return proj.scenes.firstOrNull { it.id == selectedSceneId } ?: proj.scenes.firstOrNull()
        }
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudioRepository(application.applicationContext)

    private val _transient = MutableStateFlow(StudioUiState())

    val uiState: StateFlow<StudioUiState> = combine(
        repository.projectsFlow,
        repository.seriesFlow,
        repository.settingsFlow,
        _transient
    ) { projects, series, settings, transient ->
        val resolvedActiveId = transient.activeProjectId
            ?.takeIf { id -> projects.any { it.id == id } }
            ?: projects.firstOrNull()?.id
        transient.copy(
            projects = projects,
            seriesList = series,
            settings = settings,
            activeProjectId = resolvedActiveId,
            isFirebaseAiConnected = repository.aiRepository.isFirebaseConnected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = StudioUiState()
    )

    init {
        refreshDiagnostics()
        loadAndroidVoices()
    }

    fun clearBanner() {
        _transient.update { it.copy(statusBanner = null, isErrorBanner = false) }
    }

    private fun postStatus(message: String, isError: Boolean = false) {
        _transient.update { it.copy(statusBanner = message, isErrorBanner = isError) }
    }

    fun selectProject(projectId: String) {
        _transient.update { it.copy(activeProjectId = projectId, selectedSceneId = null) }
    }

    fun selectScene(sceneId: String) {
        _transient.update { it.copy(selectedSceneId = sceneId) }
    }

    fun createNewProject(
        title: String,
        pageTypeId: String,
        durationMinutes: Int,
        fullScript: String,
        illustrationBadge: Boolean,
        onCreated: (VideoProject) -> Unit = {}
    ) {
        viewModelScope.launch {
            val created = repository.createProject(
                title = title,
                pageTypeId = pageTypeId,
                durationMinutes = durationMinutes,
                fullScript = fullScript,
                illustrationBadge = illustrationBadge
            )
            _transient.update {
                it.copy(
                    activeProjectId = created.id,
                    selectedSceneId = null,
                    statusBanner = "Projeto \"${created.title}\" criado."
                )
            }
            onCreated(created)
        }
    }

    fun createSampleTestProject(onCreated: (VideoProject) -> Unit = {}) {
        viewModelScope.launch {
            val sampleScript = """
                Em 1925, uma expedição partiu rumo ao interior desconhecido do Mato Grosso em busca de vestígios de uma antiga cidade perdida.

                Os registros deixados em cartas e diários mostram o cotidiano duro da travessia, os rios caudalosos e a determinação de quem acreditava na descoberta.

                Mesmo sem um desfecho definitivo, a história continua inspirando documentários, mapas e pesquisas quase um século depois.
            """.trimIndent()
            val created = repository.createProject(
                title = "Vídeo Teste — Expedição 1925",
                pageTypeId = "vidas-doc",
                durationMinutes = 1,
                fullScript = sampleScript,
                illustrationBadge = true
            )
            _transient.update {
                it.copy(
                    activeProjectId = created.id,
                    statusBanner = "Projeto de teste criado! Analise o roteiro e gere as cenas."
                )
            }
            onCreated(created)
        }
    }

    fun updateActiveProject(transform: (VideoProject) -> VideoProject) {
        val current = uiState.value.activeProject ?: return
        viewModelScope.launch {
            repository.saveProject(transform(current))
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            postStatus("Projeto excluído.")
        }
    }

    fun analyzeActiveProjectScript() {
        val project = uiState.value.activeProject ?: return
        if (project.fullScript.isBlank()) {
            postStatus("Escreva ou cole o roteiro completo antes de analisar.", isError = true)
            return
        }
        viewModelScope.launch {
            _transient.update { it.copy(isAnalyzingScript = true) }
            when (val res = repository.analyzeScript(project)) {
                is AiResult.Success -> {
                    val existingImages = project.scenes.associate { it.id to Pair(it.imagePath, it.isPlaceholderImage) }
                    val mergedScenes = res.data.scenes.map { scene ->
                        val prev = existingImages[scene.id]
                        if (prev != null && !prev.first.isNullOrBlank()) {
                            scene.copy(imagePath = prev.first, isPlaceholderImage = prev.second)
                        } else {
                            scene
                        }
                    }
                    val updated = project.copy(
                        logline = res.data.logline,
                        chapters = res.data.chapters,
                        aiNotes = res.data.notes,
                        scenes = mergedScenes
                    )
                    repository.saveProject(updated)
                    _transient.update {
                        it.copy(
                            isAnalyzingScript = false,
                            selectedSceneId = mergedScenes.firstOrNull()?.id,
                            aiDemoNotice = res.notice,
                            statusBanner = "${mergedScenes.size} cenas preparadas no roteiro."
                        )
                    }
                }
                is AiResult.Error -> {
                    _transient.update { it.copy(isAnalyzingScript = false) }
                    postStatus(res.message, isError = true)
                }
            }
        }
    }

    fun addManualScene() {
        val project = uiState.value.activeProject ?: return
        val nextNum = project.scenes.size + 1
        val newScene = SceneItem(
            id = "scene-${nextNum.toString().padStart(3, '0')}-${UUID.randomUUID().toString().take(4)}",
            number = nextNum,
            title = "Cena ${nextNum.toString().padStart(2, '0')}",
            narration = "",
            visualPrompt = PageModelsCatalog.findById(project.pageTypeId).visualGuidance,
            durationSeconds = 8
        )
        viewModelScope.launch {
            val updated = project.copy(scenes = project.scenes + newScene)
            repository.saveProject(updated)
            _transient.update { it.copy(selectedSceneId = newScene.id) }
        }
    }

    fun updateScene(updatedScene: SceneItem) {
        val project = uiState.value.activeProject ?: return
        val newScenes = project.scenes.map { if (it.id == updatedScene.id) updatedScene else it }
        viewModelScope.launch {
            repository.saveProject(project.copy(scenes = newScenes))
        }
    }

    fun moveScene(sceneId: String, delta: Int) {
        val project = uiState.value.activeProject ?: return
        val idx = project.scenes.indexOfFirst { it.id == sceneId }
        val target = idx + delta
        if (idx < 0 || target !in project.scenes.indices) return
        val mutable = project.scenes.toMutableList()
        val item = mutable.removeAt(idx)
        mutable.add(target, item)
        val renumbered = mutable.mapIndexed { i, sc -> sc.copy(number = i + 1) }
        viewModelScope.launch {
            repository.saveProject(project.copy(scenes = renumbered))
        }
    }

    fun deleteScene(sceneId: String) {
        val project = uiState.value.activeProject ?: return
        val renumbered = project.scenes
            .filterNot { it.id == sceneId }
            .mapIndexed { i, sc -> sc.copy(number = i + 1) }
        viewModelScope.launch {
            repository.saveProject(project.copy(scenes = renumbered))
        }
    }

    fun generateImageForScene(sceneId: String) {
        val project = uiState.value.activeProject ?: return
        val idx = project.scenes.indexOfFirst { it.id == sceneId }
        if (idx < 0) return
        val scene = project.scenes[idx]
        val prevImage = project.scenes.take(idx).lastOrNull { !it.imagePath.isNullOrBlank() }?.imagePath

        viewModelScope.launch {
            _transient.update { it.copy(isGeneratingImage = true, selectedSceneId = scene.id) }
            when (val res = repository.generateSceneImage(project, scene, prevImage)) {
                is AiResult.Success -> {
                    val updatedScenes = project.scenes.map {
                        if (it.id == scene.id) {
                            it.copy(imagePath = res.data, isPlaceholderImage = res.isDemoPlaceholder)
                        } else it
                    }
                    repository.saveProject(project.copy(scenes = updatedScenes))
                    _transient.update {
                        it.copy(
                            isGeneratingImage = false,
                            aiDemoNotice = res.notice,
                            statusBanner = "Imagem da ${scene.title} atualizada."
                        )
                    }
                }
                is AiResult.Error -> {
                    _transient.update { it.copy(isGeneratingImage = false) }
                    postStatus(res.message, isError = true)
                }
            }
        }
    }

    fun generateAllPendingImages() {
        val project = uiState.value.activeProject ?: return
        val pending = project.scenes.filter { it.imagePath.isNullOrBlank() || !File(it.imagePath).exists() }
        if (pending.isEmpty()) {
            postStatus("Todas as cenas já possuem imagem.")
            return
        }
        viewModelScope.launch {
            var currentProj = project
            var noticeText: String? = null
            pending.forEachIndexed { idx, scene ->
                _transient.update {
                    it.copy(
                        isGeneratingImage = true,
                        batchGenerationProgress = "Gerando cena ${idx + 1} de ${pending.size}..."
                    )
                }
                val prevImage = currentProj.scenes
                    .takeWhile { it.id != scene.id }
                    .lastOrNull { !it.imagePath.isNullOrBlank() }?.imagePath
                when (val res = repository.generateSceneImage(currentProj, scene, prevImage)) {
                    is AiResult.Success -> {
                        noticeText = res.notice
                        val updatedScenes = currentProj.scenes.map {
                            if (it.id == scene.id) it.copy(imagePath = res.data, isPlaceholderImage = res.isDemoPlaceholder)
                            else it
                        }
                        currentProj = repository.saveProject(currentProj.copy(scenes = updatedScenes))
                    }
                    is AiResult.Error -> {
                        _transient.update { it.copy(isGeneratingImage = false, batchGenerationProgress = null) }
                        postStatus(res.message, isError = true)
                        return@launch
                    }
                }
            }
            _transient.update {
                it.copy(
                    isGeneratingImage = false,
                    batchGenerationProgress = null,
                    aiDemoNotice = noticeText,
                    statusBanner = "${pending.size} imagens preparadas para o projeto."
                )
            }
        }
    }

    fun importPhotoForScene(sceneId: String, uri: Uri) {
        val project = uiState.value.activeProject ?: return
        viewModelScope.launch {
            runCatching {
                val file = repository.importScenePhoto(project.id, sceneId, uri)
                val updatedScenes = project.scenes.map {
                    if (it.id == sceneId) it.copy(imagePath = file.absolutePath, isPlaceholderImage = false)
                    else it
                }
                repository.saveProject(project.copy(scenes = updatedScenes))
            }.onSuccess {
                postStatus("Imagem importada da galeria com sucesso.")
            }.onFailure {
                postStatus(it.localizedMessage ?: "Erro ao importar imagem.", isError = true)
            }
        }
    }

    fun addSource(title: String, publisher: String, url: String, supports: String) {
        val project = uiState.value.activeProject ?: return
        if (title.isBlank() && url.isBlank()) {
            postStatus("Informe o título ou URL da fonte.", isError = true)
            return
        }
        val item = SourceItem(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            publisher = publisher.trim(),
            url = url.trim(),
            supports = supports.trim()
        )
        viewModelScope.launch {
            repository.saveProject(project.copy(sources = project.sources + item))
            postStatus("Fonte registrada no documentário.")
        }
    }

    fun deleteSource(sourceId: String) {
        val project = uiState.value.activeProject ?: return
        viewModelScope.launch {
            repository.saveProject(project.copy(sources = project.sources.filterNot { it.id == sourceId }))
        }
    }

    fun generatePublicationCopy() {
        val project = uiState.value.activeProject ?: return
        viewModelScope.launch {
            when (val res = repository.generatePublicationCopy(project)) {
                is AiResult.Success -> {
                    repository.saveProject(project.copy(publication = res.data))
                    _transient.update {
                        it.copy(
                            aiDemoNotice = res.notice,
                            statusBanner = "Textos de publicação atualizados."
                        )
                    }
                }
                is AiResult.Error -> postStatus(res.message, isError = true)
            }
        }
    }

    fun updatePublication(pub: PublicationMetadata) {
        updateActiveProject { it.copy(publication = pub) }
        postStatus("Metadados de publicação salvos.")
    }

    // Voice & Narration Center
    fun setNarrationProvider(provider: NarrationProvider) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(narrationProvider = provider) }
        }
    }

    fun loadAndroidVoices() {
        viewModelScope.launch {
            val voices = repository.listAndroidTtsVoices()
            _transient.update { it.copy(androidVoices = voices) }
        }
    }

    fun selectAndroidVoice(voiceName: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(androidTtsVoiceName = voiceName) }
            postStatus("Voz Android selecionada.")
        }
    }

    fun previewAndroidVoice(text: String) {
        val voiceName = uiState.value.settings.androidTtsVoiceName
        viewModelScope.launch {
            repository.previewAndroidTts(text, voiceName)
                .onFailure { postStatus(it.localizedMessage ?: "Erro no teste de voz.", isError = true) }
        }
    }

    fun synthesizeProjectNarrationAndroid() {
        val project = uiState.value.activeProject ?: run {
            postStatus("Selecione ou crie um projeto antes de gerar a narração.", isError = true)
            return
        }
        val voiceName = uiState.value.settings.androidTtsVoiceName
        viewModelScope.launch {
            _transient.update { it.copy(isSynthesizingVoice = true) }
            repository.synthesizeProjectWithAndroidTts(project, voiceName)
                .onSuccess { file ->
                    repository.saveProject(
                        project.copy(
                            audioPath = file.absolutePath,
                            audioSourceLabel = "Android TTS Offline"
                        )
                    )
                    _transient.update {
                        it.copy(
                            isSynthesizingVoice = false,
                            statusBanner = "Narração WAV gerada e anexada ao projeto!"
                        )
                    }
                }
                .onFailure { err ->
                    _transient.update { it.copy(isSynthesizingVoice = false) }
                    postStatus(err.localizedMessage ?: "Falha na síntese TTS.", isError = true)
                }
        }
    }

    fun authenticateAndLoadElevenLabs(apiKeyInput: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(elevenLabsApiKey = apiKeyInput.trim()) }
            val subRes = repository.authenticateElevenLabs(apiKeyInput)
            if (subRes.isFailure) {
                postStatus(subRes.exceptionOrNull()?.localizedMessage ?: "Falha na autenticação ElevenLabs.", isError = true)
                return@launch
            }
            val voicesRes = repository.fetchElevenLabsVoices(apiKeyInput)
            _transient.update {
                it.copy(
                    elevenSubscription = subRes.getOrNull(),
                    elevenVoices = voicesRes.getOrDefault(emptyList()),
                    statusBanner = "ElevenLabs conectado! ${voicesRes.getOrNull()?.size ?: 0} vozes carregadas."
                )
            }
        }
    }

    fun selectElevenVoice(voice: ElevenVoice) {
        viewModelScope.launch {
            repository.updateSettings {
                it.copy(
                    elevenLabsVoiceId = voice.voiceId,
                    elevenLabsVoiceName = voice.name,
                    narrationProvider = NarrationProvider.ELEVENLABS
                )
            }
            postStatus("Voz ElevenLabs \"${voice.name}\" selecionada.")
        }
    }

    fun previewElevenVoiceUrl(previewUrl: String?) {
        if (previewUrl.isNullOrBlank()) {
            postStatus("Esta voz não possui prévia de áudio disponível.", isError = true)
            return
        }
        repository.playAudioUrl(previewUrl, onError = { msg -> postStatus(msg, isError = true) })
    }

    fun synthesizeProjectNarrationElevenLabs() {
        val project = uiState.value.activeProject ?: run {
            postStatus("Selecione um projeto ativo primeiro.", isError = true)
            return
        }
        val settings = uiState.value.settings
        viewModelScope.launch {
            _transient.update { it.copy(isSynthesizingVoice = true) }
            repository.synthesizeProjectWithElevenLabs(
                project = project,
                apiKey = settings.elevenLabsApiKey,
                voiceId = settings.elevenLabsVoiceId,
                modelId = settings.elevenLabsModel
            ).onSuccess { file ->
                repository.saveProject(
                    project.copy(
                        audioPath = file.absolutePath,
                        audioSourceLabel = "ElevenLabs (${settings.elevenLabsVoiceName.ifBlank { settings.elevenLabsVoiceId }})"
                    )
                )
                _transient.update {
                    it.copy(
                        isSynthesizingVoice = false,
                        statusBanner = "Narração ElevenLabs MP3 gerada e vinculada!"
                    )
                }
            }.onFailure { err ->
                _transient.update { it.copy(isSynthesizingVoice = false) }
                postStatus(err.localizedMessage ?: "Erro na síntese ElevenLabs.", isError = true)
            }
        }
    }

    fun importProjectAudioUri(uri: Uri) {
        val project = uiState.value.activeProject ?: return
        viewModelScope.launch {
            runCatching {
                val (file, label) = repository.importProjectAudio(project.id, uri)
                repository.saveProject(
                    project.copy(
                        audioPath = file.absolutePath,
                        audioSourceLabel = "Arquivo: $label"
                    )
                )
            }.onSuccess {
                postStatus("Áudio de narração anexado ao projeto.")
            }.onFailure {
                postStatus(it.localizedMessage ?: "Erro ao importar áudio.", isError = true)
            }
        }
    }

    fun playActiveProjectAudio() {
        val path = uiState.value.activeProject?.audioPath ?: return
        repository.playAudioFile(path, onError = { postStatus(it, isError = true) })
    }

    fun stopAudio() {
        repository.stopAudioPlayback()
        repository.androidTts.stopSpeaking()
    }

    // Sound Effects (SFX)
    fun addSoundEffectToActiveProject(
        uri: Uri,
        label: String,
        startSeconds: Float,
        volumePercent: Int,
        sourceLibrary: String,
        author: String,
        licenseUrl: String,
        licenseTerms: String,
        commercialConfirmed: Boolean
    ) {
        val project = uiState.value.activeProject ?: run {
            postStatus("Selecione ou crie um projeto antes de adicionar efeitos sonoros.", isError = true)
            return
        }
        if (!commercialConfirmed) {
            postStatus("Confirme que a licença permite uso comercial e monetização.", isError = true)
            return
        }
        val sfxId = UUID.randomUUID().toString()
        viewModelScope.launch {
            runCatching {
                val (file, displayName) = repository.importSfxAudio(project.id, sfxId, uri)
                val item = SoundEffectItem(
                    id = sfxId,
                    label = label.trim().ifBlank { displayName },
                    filename = displayName,
                    filePath = file.absolutePath,
                    startSeconds = startSeconds.coerceAtLeast(0f),
                    volumePercent = volumePercent.coerceIn(2, 100),
                    sourceLibrary = sourceLibrary.trim(),
                    author = author.trim(),
                    licenseUrl = licenseUrl.trim(),
                    licenseTerms = licenseTerms.trim(),
                    commercialUseConfirmed = commercialConfirmed
                )
                repository.saveProject(project.copy(soundEffects = project.soundEffects + item))
            }.onSuccess {
                postStatus("Efeito sonoro licenciado adicionado ao projeto.")
            }.onFailure {
                postStatus(it.localizedMessage ?: "Erro ao anexar efeito sonoro.", isError = true)
            }
        }
    }

    fun playSfx(path: String) {
        repository.playAudioFile(path, onError = { postStatus(it, isError = true) })
    }

    fun deleteSfx(sfxId: String) {
        val project = uiState.value.activeProject ?: return
        viewModelScope.launch {
            repository.saveProject(project.copy(soundEffects = project.soundEffects.filterNot { it.id == sfxId }))
        }
    }

    // Series Builder
    fun createSeriesAndProjects(
        seriesName: String,
        pageTypeId: String,
        episodeDurationMinutes: Int,
        bible: String,
        episodes: List<SeriesEpisode>,
        onCompleted: () -> Unit
    ) {
        if (seriesName.isBlank() || episodes.isEmpty()) {
            postStatus("Informe o nome da série e adicione pelo menos um episódio.", isError = true)
            return
        }
        viewModelScope.launch {
            val seriesId = UUID.randomUUID().toString()
            val linkedEpisodes = episodes.mapIndexed { idx, ep ->
                val epTitle = ep.title.trim().ifBlank { "Episódio ${idx + 1}" }
                val combinedScript = buildString {
                    if (bible.isNotBlank()) {
                        appendLine("[Bíblia da Série: ${bible.trim()}]")
                        appendLine()
                    }
                    append(ep.fullScript.trim())
                }
                val createdProj = repository.createProject(
                    title = "$seriesName — $epTitle",
                    pageTypeId = pageTypeId,
                    durationMinutes = episodeDurationMinutes,
                    fullScript = combinedScript,
                    illustrationBadge = pageTypeId != "vidas-ficcao",
                    seriesId = seriesId,
                    episodeNumber = idx + 1
                )
                ep.copy(number = idx + 1, title = epTitle, linkedProjectId = createdProj.id)
            }
            val series = SeriesProject(
                id = seriesId,
                name = seriesName.trim(),
                pageTypeId = pageTypeId,
                episodeDurationMinutes = episodeDurationMinutes,
                bible = bible.trim(),
                episodes = linkedEpisodes
            )
            repository.saveSeries(series)
            _transient.update {
                it.copy(
                    activeProjectId = linkedEpisodes.firstOrNull()?.linkedProjectId,
                    statusBanner = "Série \"${series.name}\" criada com ${linkedEpisodes.size} episódio(s)!"
                )
            }
            onCompleted()
        }
    }

    fun deleteSeries(seriesId: String) {
        viewModelScope.launch {
            repository.deleteSeries(seriesId)
            postStatus("Série removida do catálogo.")
        }
    }

    // SRT & MP4 Rendering
    fun exportAndShareSrt() {
        val project = uiState.value.activeProject ?: return
        if (project.scenes.isEmpty()) {
            postStatus("Prepare as cenas antes de exportar legendas SRT.", isError = true)
            return
        }
        viewModelScope.launch {
            runCatching {
                val srtFile = repository.exportSrt(project)
                repository.fileStorage.shareFile(srtFile, "application/x-subrip", "Exportar legendas SRT")
            }.onFailure {
                postStatus(it.localizedMessage ?: "Erro ao exportar SRT.", isError = true)
            }
        }
    }

    fun renderActiveProjectMp4() {
        val project = uiState.value.activeProject ?: return
        viewModelScope.launch {
            _transient.update {
                it.copy(renderProgress = RenderProgress(isRendering = true, progressFraction = 0.02f, stageLabel = "Iniciando..."))
            }
            val res = repository.renderProjectMp4(project) { progress, label ->
                _transient.update {
                    it.copy(renderProgress = RenderProgress(isRendering = true, progressFraction = progress, stageLabel = label))
                }
            }
            res.onSuccess { outFile ->
                repository.saveProject(
                    project.copy(
                        renderedVideoPath = outFile.absolutePath,
                        renderedAtMillis = System.currentTimeMillis()
                    )
                )
                _transient.update {
                    it.copy(
                        renderProgress = RenderProgress(isRendering = false, progressFraction = 1f, stageLabel = "Concluído!"),
                        statusBanner = "MP4 salvo em Filmes/AutoTubeStudio e pronto para compartilhar!"
                    )
                }
            }.onFailure { err ->
                _transient.update {
                    it.copy(renderProgress = RenderProgress(isRendering = false))
                }
                postStatus(err.localizedMessage ?: "Falha na renderização MP4.", isError = true)
            }
        }
    }

    fun shareRenderedMp4() {
        val path = uiState.value.activeProject?.renderedVideoPath ?: return
        val file = File(path)
        if (!file.exists()) {
            postStatus("Arquivo MP4 não encontrado; renderize novamente.", isError = true)
            return
        }
        repository.fileStorage.shareFile(file, "video/mp4", "Compartilhar vídeo MP4")
    }

    fun openRenderedMp4() {
        val path = uiState.value.activeProject?.renderedVideoPath ?: return
        val file = File(path)
        if (!file.exists()) {
            postStatus("Arquivo MP4 não encontrado.", isError = true)
            return
        }
        runCatching { repository.fileStorage.openVideoFile(file) }
            .onFailure { postStatus("Nenhum player MP4 encontrado; use Compartilhar.", isError = true) }
    }

    // Settings & Brand
    fun saveBrandAndSettings(
        channelName: String,
        watermarkText: String,
        imageModelId: String,
        elevenApiKey: String,
        elevenVoiceId: String,
        elevenModel: String,
        licenseNote: String
    ) {
        viewModelScope.launch {
            repository.updateSettings {
                it.copy(
                    channelName = channelName.trim(),
                    watermarkText = watermarkText.trim(),
                    imageModelId = imageModelId,
                    elevenLabsApiKey = elevenApiKey.trim(),
                    elevenLabsVoiceId = elevenVoiceId.trim(),
                    elevenLabsModel = elevenModel,
                    licenseNote = licenseNote.trim()
                )
            }
            postStatus("Ajustes e identidade visual salvos!")
        }
    }

    fun importWatermarkLogo(uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.importWatermarkLogo(uri) }
                .onSuccess { postStatus("Logotipo PNG salvo como marca d'água!") }
                .onFailure { postStatus(it.localizedMessage ?: "Erro ao importar logotipo.", isError = true) }
        }
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val checks = repository.runDiagnostics()
            _transient.update { it.copy(diagnostics = checks) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}

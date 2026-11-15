package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.PageModelsCatalog
import com.example.domain.model.PublicationMetadata
import com.example.domain.model.SceneItem
import com.example.domain.model.VideoProject
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.components.FirebaseDemoNoticeBanner
import com.example.ui.components.PageModelPickerDialog
import com.example.ui.components.Scene16x9PreviewBox
import com.example.ui.components.StudioEyebrow
import com.example.ui.components.StudioPanelCard
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioPanelElevated
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioWorkspaceScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    forceNewMode: Boolean,
    onDoneNewMode: () -> Unit,
    onNavigateToVoice: () -> Unit
) {
    val activeProject = if (forceNewMode) null else uiState.activeProject
    var showCreateForm by remember(forceNewMode, activeProject?.id) {
        mutableStateOf(forceNewMode || activeProject == null)
    }

    if (showCreateForm || activeProject == null) {
        NewProjectCreationView(
            hasExistingProjects = uiState.projects.isNotEmpty(),
            onCancel = {
                showCreateForm = false
                onDoneNewMode()
            },
            onCreate = { title, pageTypeId, duration, script, illustration ->
                viewModel.createNewProject(
                    title = title,
                    pageTypeId = pageTypeId,
                    durationMinutes = duration,
                    fullScript = script,
                    illustrationBadge = illustration
                ) {
                    showCreateForm = false
                    onDoneNewMode()
                }
            }
        )
        return
    }

    var workspaceTab by remember { mutableIntStateOf(0) } // 0=Roteiro/Cenas, 1=Fontes/Pub, 2=Exportar MP4
    var showModelPicker by remember { mutableStateOf(false) }
    var editingScene by remember { mutableStateOf<SceneItem?>(null) }
    var animateKenBurns by remember { mutableStateOf(true) }
    var pendingPhotoSceneId by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val sceneId = pendingPhotoSceneId ?: uiState.selectedScene?.id
        if (uri != null && sceneId != null) {
            viewModel.importPhotoForScene(sceneId, uri)
        }
        pendingPhotoSceneId = null
    }

    if (showModelPicker) {
        PageModelPickerDialog(
            selectedId = activeProject.pageTypeId,
            onSelect = { model ->
                viewModel.updateActiveProject { it.copy(pageTypeId = model.id) }
            },
            onDismiss = { showModelPicker = false }
        )
    }

    editingScene?.let { sceneToEdit ->
        EditSceneDialog(
            scene = sceneToEdit,
            onSave = { updated ->
                viewModel.updateScene(updated)
                editingScene = null
            },
            onDismiss = { editingScene = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("studio_workspace_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Active project header + switch to New
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StudioEyebrow("PROJETO ATIVO · ${activeProject.pageModel.name}")
                    Text(
                        text = activeProject.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = { showCreateForm = true },
                    modifier = Modifier.testTag("studio_new_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Novo")
                }
            }
        }

        // 16:9 Live Preview Canvas
        item {
            StudioPanelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioEyebrow("PRÉVIA DA CENA · 16:9")
                    FilterChip(
                        selected = animateKenBurns,
                        onClick = { animateKenBurns = !animateKenBurns },
                        label = { Text(if (animateKenBurns) "Ken Burns: ON" else "Ken Burns: OFF") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Scene16x9PreviewBox(
                    scene = uiState.selectedScene,
                    pageModel = activeProject.pageModel,
                    watermarkText = uiState.settings.watermarkText.ifBlank { uiState.settings.channelName },
                    watermarkLogoPath = uiState.settings.watermarkLogoPath,
                    showIllustrationBadge = activeProject.illustrationBadge,
                    animateKenBurns = animateKenBurns
                )
                Spacer(Modifier.height(10.dp))

                if (uiState.isGeneratingImage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = uiState.batchGenerationProgress ?: "Preparando imagem da cena...",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioGold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val target = uiState.selectedScene ?: activeProject.scenes.firstOrNull()
                            if (target != null) {
                                viewModel.generateImageForScene(target.id)
                            }
                        },
                        enabled = !uiState.isGeneratingImage && activeProject.scenes.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_scene_image_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (uiState.isFirebaseAiConnected) "Gerar Gemini" else "Gerar amostra")
                    }

                    OutlinedButton(
                        onClick = {
                            val target = uiState.selectedScene ?: activeProject.scenes.firstOrNull()
                            if (target != null) {
                                pendingPhotoSceneId = target.id
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        enabled = activeProject.scenes.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_scene_photo_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Foto galeria")
                    }
                }
            }
        }

        // Workspace Sub-tabs
        item {
            val tabLabels = listOf("Roteiro & Cenas", "Fontes & Pub.", "Renderizar")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabLabels.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = workspaceTab == idx,
                        onClick = { workspaceTab = idx },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.testTag("studio_subtab_$idx")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        when (workspaceTab) {
            0 -> {
                if (!uiState.isFirebaseAiConnected) {
                    item {
                        FirebaseDemoNoticeBanner(customText = uiState.aiDemoNotice)
                    }
                }
                item {
                    ScriptAndMetadataEditorCard(
                        project = activeProject,
                        isAnalyzing = uiState.isAnalyzingScript,
                        onOpenModelPicker = { showModelPicker = true },
                        onSaveMetadataAndScript = { title, duration, script, illustration ->
                            viewModel.updateActiveProject {
                                it.copy(
                                    title = title.trim().ifBlank { it.title },
                                    targetDurationMinutes = duration.coerceIn(1, 30),
                                    fullScript = script,
                                    illustrationBadge = illustration
                                )
                            }
                        },
                        onAnalyzeScript = { viewModel.analyzeActiveProjectScript() }
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            StudioEyebrow("MONTAGEM DO ROTEIRO")
                            Text(
                                text = "Cenas (${activeProject.scenes.size}) · ${activeProject.totalSceneDurationSeconds}s",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.generateAllPendingImages() },
                                enabled = !uiState.isGeneratingImage && activeProject.scenes.isNotEmpty(),
                                modifier = Modifier.testTag("generate_all_pending_button")
                            ) {
                                Text("Gerar todas")
                            }
                            Button(
                                onClick = { viewModel.addManualScene() },
                                modifier = Modifier.testTag("add_manual_scene_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cena")
                            }
                        }
                    }
                }
                if (activeProject.scenes.isEmpty()) {
                    item {
                        StudioPanelCard {
                            Text(
                                text = "Nenhuma cena preparada ainda. Cole o roteiro acima e toque em \"Analisar roteiro e preparar cenas\" ou adicione uma cena manualmente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(activeProject.scenes, key = { it.id }) { scene ->
                        SceneCardItem(
                            scene = scene,
                            isSelected = scene.id == uiState.selectedScene?.id,
                            onSelect = { viewModel.selectScene(scene.id) },
                            onEdit = { editingScene = scene },
                            onGenerateImage = { viewModel.generateImageForScene(scene.id) },
                            onPickPhoto = {
                                pendingPhotoSceneId = scene.id
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onMoveUp = { viewModel.moveScene(scene.id, -1) },
                            onMoveDown = { viewModel.moveScene(scene.id, +1) },
                            onDelete = { viewModel.deleteScene(scene.id) }
                        )
                    }
                }
            }
            1 -> {
                item {
                    SourcesAndPublicationSection(
                        project = activeProject,
                        onAddSource = { t, p, u, s -> viewModel.addSource(t, p, u, s) },
                        onDeleteSource = { viewModel.deleteSource(it) },
                        onGeneratePubCopy = { viewModel.generatePublicationCopy() },
                        onSavePub = { viewModel.updatePublication(it) }
                    )
                }
            }
            2 -> {
                item {
                    ProductionAndExportSection(
                        project = activeProject,
                        uiState = uiState,
                        onNavigateToVoice = onNavigateToVoice,
                        onExportSrt = { viewModel.exportAndShareSrt() },
                        onRenderMp4 = { viewModel.renderActiveProjectMp4() },
                        onPlayMp4 = { viewModel.openRenderedMp4() },
                        onShareMp4 = { viewModel.shareRenderedMp4() }
                    )
                }
            }
        }
    }
}

@Composable
private fun NewProjectCreationView(
    hasExistingProjects: Boolean,
    onCancel: () -> Unit,
    onCreate: (title: String, pageTypeId: String, duration: Int, script: String, illustration: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(PageModelsCatalog.findById("vidas-ficcao")) }
    var durationText by remember { mutableStateOf("8") }
    var fullScript by remember { mutableStateOf("") }
    var illustrationBadge by remember { mutableStateOf(true) }
    var showModelPicker by remember { mutableStateOf(false) }

    if (showModelPicker) {
        PageModelPickerDialog(
            selectedId = selectedModel.id,
            onSelect = {
                selectedModel = it
                illustrationBadge = it.isFactual
            },
            onDismiss = { showModelPicker = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("new_project_form"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    StudioEyebrow("NOVO CONTEÚDO")
                    Text("Configurar projeto", style = MaterialTheme.typography.headlineSmall)
                }
                if (hasExistingProjects) {
                    TextButton(onClick = onCancel) {
                        Text("Voltar ao ativo")
                    }
                }
            }
        }

        item {
            StudioPanelCard {
                Text("Tipo de página (27 modelos)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showModelPicker = true }
                        .testTag("new_project_model_selector"),
                    shape = MaterialTheme.shapes.medium,
                    color = StudioPanelElevated,
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(selectedModel.iconEmoji, style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selectedModel.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                selectedModel.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("Alterar", color = StudioGold, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do vídeo") },
                    placeholder = { Text("Digite o título definitivo do vídeo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input")
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Duração prevista (1 a 30 min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_duration_input")
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = fullScript,
                    onValueChange = { fullScript = it },
                    label = { Text("Roteiro completo (${fullScript.length} caracteres)") },
                    placeholder = { Text("Cole ou escreva aqui o roteiro completo do vídeo...") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_script_input")
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { illustrationBadge = !illustrationBadge },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = illustrationBadge,
                        onCheckedChange = { illustrationBadge = it }
                    )
                    Text(
                        text = "Exibir selo discreto \"ILUSTRAÇÃO\" em conteúdos factuais",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val dur = durationText.toIntOrNull()?.coerceIn(1, 30) ?: 8
                        onCreate(title, selectedModel.id, dur, fullScript, illustrationBadge)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("create_project_submit_button")
                ) {
                    Text("Criar projeto no estúdio")
                }
            }
        }
    }
}

@Composable
private fun ScriptAndMetadataEditorCard(
    project: VideoProject,
    isAnalyzing: Boolean,
    onOpenModelPicker: () -> Unit,
    onSaveMetadataAndScript: (String, Int, String, Boolean) -> Unit,
    onAnalyzeScript: () -> Unit
) {
    var title by remember(project.id) { mutableStateOf(project.title) }
    var duration by remember(project.id) { mutableStateOf(project.targetDurationMinutes.toString()) }
    var script by remember(project.id) { mutableStateOf(project.fullScript) }
    var illustration by remember(project.id) { mutableStateOf(project.illustrationBadge) }

    LaunchedEffect(project.title, project.fullScript) {
        if (title != project.title) title = project.title
        if (script != project.fullScript) script = project.fullScript
    }

    StudioPanelCard {
        StudioEyebrow("ROTEIRO E DIREÇÃO")
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenModelPicker),
            shape = MaterialTheme.shapes.small,
            color = StudioPanelElevated,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${project.pageModel.iconEmoji} ${project.pageModel.name}", style = MaterialTheme.typography.titleSmall)
                Text("Trocar modelo", color = StudioGold, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it.filter(Char::isDigit).take(2) },
                label = { Text("Min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = script,
            onValueChange = { script = it },
            label = { Text("Roteiro completo (${script.length} caracteres)") },
            minLines = 5,
            maxLines = 10,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("active_project_script_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { illustration = !illustration },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = illustration, onCheckedChange = { illustration = it })
            Text("Rótulo discreto ILUSTRAÇÃO (formatos factuais)", style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onSaveMetadataAndScript(title, duration.toIntOrNull() ?: 8, script, illustration)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_script_button")
            ) {
                Text("Salvar texto")
            }
            Button(
                onClick = {
                    onSaveMetadataAndScript(title, duration.toIntOrNull() ?: 8, script, illustration)
                    onAnalyzeScript()
                },
                enabled = !isAnalyzing && script.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("analyze_script_button")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Preparar cenas")
            }
        }

        if (project.aiNotes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            project.aiNotes.forEach { note ->
                Text("• $note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SceneCardItem(
    scene: SceneItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onGenerateImage: () -> Unit,
    onPickPhoto: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val hasImage = !scene.imagePath.isNullOrBlank() && File(scene.imagePath).exists()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("scene_card_${scene.id}"),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) StudioPanelElevated else StudioPanel,
        border = BorderStroke(1.dp, if (isSelected) StudioGold else StudioBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cena ${scene.number.toString().padStart(2, '0')} · ${scene.durationSeconds}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioGold
                )
                Row {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Subir cena", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Descer cena", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar cena", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir cena", tint = StudioCrimson, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(scene.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (scene.narration.isNotBlank()) {
                Text(
                    text = scene.narration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasImage) (if (scene.isPlaceholderImage) "Imagem: Amostra Demo" else "Imagem pronta") else "Sem imagem",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasImage) StudioMint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onPickPhoto) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Galeria")
                }
                TextButton(onClick = onGenerateImage) {
                    Text("Gerar")
                }
            }
        }
    }
}

@Composable
private fun EditSceneDialog(
    scene: SceneItem,
    onSave: (SceneItem) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(scene.title) }
    var narration by remember { mutableStateOf(scene.narration) }
    var visualPrompt by remember { mutableStateOf(scene.visualPrompt) }
    var duration by remember { mutableStateOf(scene.durationSeconds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanel,
        title = { Text("Editar Cena ${scene.number}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título da cena") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                    label = { Text("Duração (segundos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = narration,
                    onValueChange = { narration = it },
                    label = { Text("Narração da cena") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = visualPrompt,
                    onValueChange = { visualPrompt = it },
                    label = { Text("Prompt visual 16:9") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    scene.copy(
                        title = title.trim().ifBlank { scene.title },
                        narration = narration,
                        visualPrompt = visualPrompt,
                        durationSeconds = duration.toIntOrNull()?.coerceIn(1, 120) ?: 8
                    )
                )
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SourcesAndPublicationSection(
    project: VideoProject,
    onAddSource: (String, String, String, String) -> Unit,
    onDeleteSource: (String) -> Unit,
    onGeneratePubCopy: () -> Unit,
    onSavePub: (PublicationMetadata) -> Unit
) {
    var srcTitle by remember { mutableStateOf("") }
    var srcPublisher by remember { mutableStateOf("") }
    var srcUrl by remember { mutableStateOf("") }
    var srcSupports by remember { mutableStateOf("") }

    var pubTitles by remember(project.id, project.publication) {
        mutableStateOf(project.publication.titles.joinToString("\n"))
    }
    var pubDesc by remember(project.id, project.publication) {
        mutableStateOf(project.publication.description)
    }
    var pubTags by remember(project.id, project.publication) {
        mutableStateOf(project.publication.tags.joinToString(", "))
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudioPanelCard {
            StudioEyebrow("APURAÇÃO E REFERÊNCIAS")
            Text("Fontes do documentário (${project.sources.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            project.sources.forEach { src ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    color = StudioPanelElevated,
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(src.title.ifBlank { src.url }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${src.publisher} · ${src.supports}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteSource(src.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover fonte", tint = StudioCrimson)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = srcTitle, onValueChange = { srcTitle = it }, label = { Text("Título da fonte") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = srcPublisher, onValueChange = { srcPublisher = it }, label = { Text("Órgão / autor") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = srcUrl, onValueChange = { srcUrl = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = srcSupports, onValueChange = { srcSupports = it }, label = { Text("Informação sustentada") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    onAddSource(srcTitle, srcPublisher, srcUrl, srcSupports)
                    srcTitle = ""; srcPublisher = ""; srcUrl = ""; srcSupports = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ Registrar fonte")
            }
        }

        StudioPanelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    StudioEyebrow("YOUTUBE & REDES")
                    Text("Textos de publicação", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onGeneratePubCopy) {
                    Text("Gerar amostra")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pubTitles,
                onValueChange = { pubTitles = it },
                label = { Text("Opções de título (1 por linha)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pubDesc,
                onValueChange = { pubDesc = it },
                label = { Text("Descrição") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pubTags,
                onValueChange = { pubTags = it },
                label = { Text("Tags separadas por vírgula") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    onSavePub(
                        project.publication.copy(
                            titles = pubTitles.lines().map { it.trim() }.filter { it.isNotEmpty() },
                            description = pubDesc,
                            tags = pubTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar textos de publicação")
            }
        }
    }
}

@Composable
private fun ProductionAndExportSection(
    project: VideoProject,
    uiState: StudioUiState,
    onNavigateToVoice: () -> Unit,
    onExportSrt: () -> Unit,
    onRenderMp4: () -> Unit,
    onPlayMp4: () -> Unit,
    onShareMp4: () -> Unit
) {
    val missingImages = project.scenes.count { it.imagePath.isNullOrBlank() || !File(it.imagePath).exists() }
    val hasAudio = !project.audioPath.isNullOrBlank() && File(project.audioPath).exists()
    val hasMp4 = !project.renderedVideoPath.isNullOrBlank() && File(project.renderedVideoPath).exists()

    StudioPanelCard {
        StudioEyebrow("PRODUÇÃO E RENDERIZAÇÃO MP4")
        Text("Exportar vídeo e legendas", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))

        Text(
            text = "• Cenas prontas: ${project.scenes.size} (${project.totalSceneDurationSeconds}s)\n" +
                "• Imagens faltando: $missingImages\n" +
                "• Narração: ${if (hasAudio) project.audioSourceLabel ?: "Áudio anexado" else "Nenhuma (vídeo será mudo)"}\n" +
                "• Efeitos sonoros: ${project.soundEffects.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToVoice,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Central de voz")
            }
            OutlinedButton(
                onClick = onExportSrt,
                enabled = project.scenes.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_srt_button")
            ) {
                Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Baixar SRT")
            }
        }

        Spacer(Modifier.height(12.dp))
        if (uiState.renderProgress.isRendering) {
            LinearProgressIndicator(
                progress = { uiState.renderProgress.progressFraction },
                modifier = Modifier.fillMaxWidth(),
                color = StudioGold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${(uiState.renderProgress.progressFraction * 100).toInt()}% · ${uiState.renderProgress.stageLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = StudioGold
            )
            Spacer(Modifier.height(10.dp))
        }

        Button(
            onClick = onRenderMp4,
            enabled = !uiState.renderProgress.isRendering && project.scenes.isNotEmpty() && missingImages == 0,
            colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("render_mp4_button")
        ) {
            Icon(Icons.Default.Movie, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Renderizar MP4 (H.264 + AAC)")
        }

        if (missingImages > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Gere ou importe as $missingImages imagem(ns) pendentes na aba Roteiro & Cenas para liberar o MP4.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (hasMp4) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPlayMp4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("play_mp4_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Assistir MP4")
                }
                Button(
                    onClick = onShareMp4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_mp4_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Compartilhar")
                }
            }
        }
    }
}

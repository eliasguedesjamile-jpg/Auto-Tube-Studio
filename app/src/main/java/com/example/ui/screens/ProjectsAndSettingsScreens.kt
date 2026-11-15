package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.components.FirebaseDemoNoticeBanner
import com.example.ui.components.StudioEyebrow
import com.example.ui.components.StudioPanelCard
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioPanelElevated
import java.io.File

@Composable
fun ProjectsLibraryScreen(
    uiState: StudioUiState,
    onOpenProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onShareMp4: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = uiState.projects.filter {
        searchQuery.isBlank() ||
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.pageModel.name.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("projects_library_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioEyebrow("BIBLIOTECA")
                Text("Meus vídeos (${uiState.projects.size})", style = MaterialTheme.typography.headlineSmall)
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Filtrar projetos por título ou modelo...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filtered.isEmpty()) {
            item {
                StudioPanelCard {
                    Text(
                        text = "Nenhum vídeo encontrado nesta lista.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { project ->
                val hasMp4 = !project.renderedVideoPath.isNullOrBlank() && File(project.renderedVideoPath).exists()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProject(project.id) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (project.id == uiState.activeProject?.id) StudioPanelElevated else StudioPanel,
                    border = BorderStroke(1.dp, if (project.id == uiState.activeProject?.id) StudioGold else StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(project.pageModel.iconEmoji, style = MaterialTheme.typography.headlineSmall)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(project.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "${project.pageModel.name} · ${project.scenes.size} cenas · ${if (hasMp4) "MP4 pronto" else "Em edição"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasMp4) StudioMint else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (hasMp4) {
                            IconButton(onClick = { onShareMp4(project.id) }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartilhar MP4", tint = StudioGold)
                            }
                        }
                        IconButton(onClick = { onOpenProject(project.id) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Abrir no estúdio")
                        }
                        IconButton(onClick = { onDeleteProject(project.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir projeto", tint = StudioCrimson)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndHelpScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onNavigateToStudio: () -> Unit,
    onNavigateToVoice: () -> Unit
) {
    var tabIdx by remember { mutableIntStateOf(0) } // 0 = Ajustes/Marca, 1 = Diagnóstico, 2 = Aulas

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importWatermarkLogo(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_and_help_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioEyebrow("CONFIGURAÇÕES E SUPORTE")
                Text("Estúdio, marca e diagnóstico", style = MaterialTheme.typography.headlineSmall)
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = tabIdx == 0,
                    onClick = { tabIdx = 0 },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("settings_tab_brand")
                ) { Text("Marca & API") }
                SegmentedButton(
                    selected = tabIdx == 1,
                    onClick = { tabIdx = 1 },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("settings_tab_diag")
                ) { Text("Diagnóstico") }
                SegmentedButton(
                    selected = tabIdx == 2,
                    onClick = { tabIdx = 2 },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("settings_tab_lessons")
                ) { Text("Aulas") }
            }
        }

        when (tabIdx) {
            0 -> {
                item {
                    var channelName by remember(uiState.settings.channelName) { mutableStateOf(uiState.settings.channelName) }
                    var watermarkText by remember(uiState.settings.watermarkText) { mutableStateOf(uiState.settings.watermarkText) }
                    var elevenKey by remember(uiState.settings.elevenLabsApiKey) { mutableStateOf(uiState.settings.elevenLabsApiKey) }
                    var elevenVoiceId by remember(uiState.settings.elevenLabsVoiceId) { mutableStateOf(uiState.settings.elevenLabsVoiceId) }
                    var elevenModel by remember(uiState.settings.elevenLabsModel) { mutableStateOf(uiState.settings.elevenLabsModel) }
                    var licenseNote by remember(uiState.settings.licenseNote) { mutableStateOf(uiState.settings.licenseNote) }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (!uiState.isFirebaseAiConnected) {
                            FirebaseDemoNoticeBanner()
                        }

                        StudioPanelCard {
                            StudioEyebrow("IDENTIDADE VISUAL DO CANAL")
                            Text("Marca d'água e logotipo", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = channelName,
                                onValueChange = { channelName = it },
                                label = { Text("Nome do canal") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                label = { Text("Marca d'água em texto (@canal)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))

                            val logoFile = uiState.settings.watermarkLogoPath?.let { File(it) }?.takeIf { it.exists() }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StudioPanelElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (logoFile != null) {
                                        AsyncImage(
                                            model = logoFile,
                                            contentDescription = stringResource(R.string.cd_brand_logo),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = StudioGold)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        logoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Carregar logotipo PNG")
                                }
                            }
                        }

                        StudioPanelCard {
                            StudioEyebrow("CONEXÃO ELEVENLABS (OPCIONAL)")
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = elevenKey,
                                onValueChange = { elevenKey = it },
                                label = { Text("Chave API ElevenLabs") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = elevenVoiceId,
                                onValueChange = { elevenVoiceId = it },
                                label = { Text("Voice ID selecionado") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = elevenModel,
                                onValueChange = { elevenModel = it },
                                label = { Text("Modelo de voz ElevenLabs") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        StudioPanelCard {
                            StudioEyebrow("LICENÇA LOCAL")
                            Text("Auto Tube Studio 1.5.8 Android", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = licenseNote,
                                onValueChange = { licenseNote = it },
                                label = { Text("Anotação local (opcional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.saveBrandAndSettings(
                                        channelName = channelName,
                                        watermarkText = watermarkText,
                                        imageModelId = uiState.settings.imageModelId,
                                        elevenApiKey = elevenKey,
                                        elevenVoiceId = elevenVoiceId,
                                        elevenModel = elevenModel,
                                        licenseNote = licenseNote
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_settings_button")
                            ) {
                                Text("Salvar ajustes")
                            }
                        }
                    }
                }
            }
            1 -> {
                item {
                    StudioPanelCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                StudioEyebrow("PASSO 1 · COMEÇAR")
                                Text("Diagnóstico do celular", style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = { viewModel.refreshDiagnostics() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reverificar", tint = StudioGold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        uiState.diagnostics.forEach { check ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.small,
                                color = StudioPanelElevated,
                                border = BorderStroke(1.dp, StudioBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (check.ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (check.ok) StudioMint else StudioAmber
                                    )
                                    Column {
                                        Text(check.title, style = MaterialTheme.typography.titleSmall)
                                        Text(check.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                val lessons = listOf(
                    Triple("01 · Primeiros passos", "Configure a identidade do seu canal, marca d'água e confira o diagnóstico de codecs e TTS do aparelho.", "Abrir Ajustes"),
                    Triple("02 · Criar um vídeo", "Escolha um dos 27 modelos visuais, cole o roteiro completo, prepare as cenas e adicione imagens da galeria ou amostras.", "Ir ao Estúdio"),
                    Triple("03 · Narração pt-BR", "Use as vozes nativas do Android offline em WAV, conecte sua chave ElevenLabs ou importe seu próprio áudio.", "Abrir Central de Voz"),
                    Triple("04 · Renderização MP4", "Exporte legendas SRT ou gere o arquivo MP4 em 16:9 com movimento Ken Burns, legendas e marca d'água direto no celular.", "Ir ao Estúdio")
                )
                items(lessons) { (title, desc, actionLabel) ->
                    StudioPanelCard {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = StudioGold)
                        Spacer(Modifier.height(6.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            if (actionLabel.contains("Voz")) onNavigateToVoice()
                            else if (actionLabel.contains("Ajustes")) tabIdx = 0
                            else onNavigateToStudio()
                        }) {
                            Text(actionLabel)
                        }
                    }
                }
            }
        }
    }
}

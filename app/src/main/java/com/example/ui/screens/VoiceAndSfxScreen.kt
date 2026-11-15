package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.NarrationProvider
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.components.StudioEyebrow
import com.example.ui.components.StudioPanelCard
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioPanelElevated
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAndSfxScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel
) {
    var topTab by remember { mutableIntStateOf(0) } // 0 = Narração, 1 = Efeitos Sonoros (SFX)

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importProjectAudioUri(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("voice_and_sfx_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioEyebrow("ÁUDIO DO ESTÚDIO")
                Text("Central de narração e efeitos", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = uiState.activeProject?.let { "Projeto ativo: ${it.title}" }
                        ?: "Crie ou selecione um projeto para vincular narração e efeitos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = topTab == 0,
                    onClick = { topTab = 0 },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("voice_tab_narration")
                ) { Text("Narração") }
                SegmentedButton(
                    selected = topTab == 1,
                    onClick = { topTab = 1 },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("voice_tab_sfx")
                ) { Text("Efeitos Sonoros") }
            }
        }

        if (topTab == 0) {
            // Active project audio status card
            item {
                val active = uiState.activeProject
                val hasAudio = !active?.audioPath.isNullOrBlank() && File(active!!.audioPath!!).exists()
                StudioPanelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StudioEyebrow("TRILHA DO PROJETO")
                            Text(
                                text = if (hasAudio) active?.audioSourceLabel ?: "Áudio pronto" else "Nenhuma narração gerada",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (hasAudio) StudioMint else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (hasAudio) {
                            Row {
                                IconButton(onClick = { viewModel.playActiveProjectAudio() }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Ouvir narração", tint = StudioGold)
                                }
                                IconButton(onClick = { viewModel.stopAudio() }) {
                                    Icon(Icons.Default.Stop, contentDescription = "Parar reprodução")
                                }
                            }
                        }
                    }
                }
            }

            // Provider Selector
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.settings.narrationProvider == NarrationProvider.ANDROID_TTS,
                        onClick = { viewModel.setNarrationProvider(NarrationProvider.ANDROID_TTS) },
                        shape = MaterialTheme.shapes.small
                    ) { Text("Android TTS") }
                    SegmentedButton(
                        selected = uiState.settings.narrationProvider == NarrationProvider.ELEVENLABS,
                        onClick = { viewModel.setNarrationProvider(NarrationProvider.ELEVENLABS) },
                        shape = MaterialTheme.shapes.small
                    ) { Text("ElevenLabs") }
                    SegmentedButton(
                        selected = uiState.settings.narrationProvider == NarrationProvider.USER_AUDIO,
                        onClick = { viewModel.setNarrationProvider(NarrationProvider.USER_AUDIO) },
                        shape = MaterialTheme.shapes.small
                    ) { Text("Arquivo") }
                }
            }

            when (uiState.settings.narrationProvider) {
                NarrationProvider.ANDROID_TTS -> {
                    item {
                        AndroidTtsSection(
                            uiState = uiState,
                            onRefreshVoices = { viewModel.loadAndroidVoices() },
                            onSelectVoice = { viewModel.selectAndroidVoice(it) },
                            onPreviewText = { viewModel.previewAndroidVoice(it) },
                            onStopPreview = { viewModel.stopAudio() },
                            onSynthesizeProject = { viewModel.synthesizeProjectNarrationAndroid() }
                        )
                    }
                }
                NarrationProvider.ELEVENLABS -> {
                    item {
                        ElevenLabsSection(
                            uiState = uiState,
                            onAuthenticate = { viewModel.authenticateAndLoadElevenLabs(it) },
                            onSelectVoice = { viewModel.selectElevenVoice(it) },
                            onPreviewVoice = { viewModel.previewElevenVoiceUrl(it) },
                            onSynthesizeProject = { viewModel.synthesizeProjectNarrationElevenLabs() }
                        )
                    }
                }
                NarrationProvider.USER_AUDIO -> {
                    item {
                        StudioPanelCard {
                            StudioEyebrow("ÁUDIO PRÓPRIO")
                            Text("Importar gravação externa", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Selecione um arquivo MP3, WAV ou M4A gravado no seu celular ou computador para usar como narração oficial do projeto ativo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { audioPicker.launch(arrayOf("audio/*")) },
                                enabled = uiState.activeProject != null,
                                colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("import_custom_audio_button")
                            ) {
                                Icon(Icons.Default.AudioFile, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Escolher arquivo de áudio")
                            }
                        }
                    }
                }
            }
        } else {
            item {
                SoundEffectsSection(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AndroidTtsSection(
    uiState: StudioUiState,
    onRefreshVoices: () -> Unit,
    onSelectVoice: (String) -> Unit,
    onPreviewText: (String) -> Unit,
    onStopPreview: () -> Unit,
    onSynthesizeProject: () -> Unit
) {
    var testText by remember {
        mutableStateOf("Olá! Esta é uma demonstração de narração offline do Auto Tube Studio.")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioPanelCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StudioEyebrow("MOTOR NATIVO OFFLINE")
                    Text("Vozes do Android (pt-BR)", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onRefreshVoices) { Text("Atualizar") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sintetiza a narração diretamente no aparelho em arquivo WAV, sem enviar texto para a nuvem.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                label = { Text("Texto para testar voz") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onPreviewText(testText) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Testar voz")
                }
                OutlinedButton(onClick = onStopPreview) {
                    Icon(Icons.Default.Stop, contentDescription = "Parar", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSynthesizeProject,
                enabled = !uiState.isSynthesizingVoice && uiState.activeProject != null,
                colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("synthesize_android_tts_button")
            ) {
                if (uiState.isSynthesizingVoice) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Gerando WAV...")
                } else {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gerar narração do projeto (WAV)")
                }
            }
        }

        if (uiState.androidVoices.isNotEmpty()) {
            StudioPanelCard {
                Text("Selecionar voz instalada (${uiState.androidVoices.size})", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                uiState.androidVoices.take(12).forEach { voice ->
                    val selected = voice.name == uiState.settings.androidTtsVoiceName
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelectVoice(voice.name) },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) StudioPanelElevated else StudioPanel,
                        border = BorderStroke(1.dp, if (selected) StudioGold else StudioBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(voice.localeLabel, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${voice.name} · Qualidade ${voice.qualityLabel} · ${if (voice.isNetworkRequired) "Online" else "Offline"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (selected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selecionada", tint = StudioGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ElevenLabsSection(
    uiState: StudioUiState,
    onAuthenticate: (String) -> Unit,
    onSelectVoice: (com.example.domain.model.ElevenVoice) -> Unit,
    onPreviewVoice: (String?) -> Unit,
    onSynthesizeProject: () -> Unit
) {
    var apiKey by remember(uiState.settings.elevenLabsApiKey) {
        mutableStateOf(uiState.settings.elevenLabsApiKey)
    }

    StudioPanelCard {
        StudioEyebrow("VOZES EM NUVEM (OPCIONAL)")
        Text("ElevenLabs", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Chave API ElevenLabs") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onAuthenticate(apiKey) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Cloud, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Autenticar e carregar vozes")
        }

        uiState.elevenSubscription?.let { sub ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Plano: ${sub.tier} · Consumo: ${sub.characterCount}/${sub.characterLimit} caracteres",
                style = MaterialTheme.typography.labelMedium,
                color = StudioMint
            )
        }

        if (uiState.elevenVoices.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            uiState.elevenVoices.take(15).forEach { voice ->
                val selected = voice.voiceId == uiState.settings.elevenLabsVoiceId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onSelectVoice(voice) },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) StudioPanelElevated else StudioPanel,
                    border = BorderStroke(1.dp, if (selected) StudioGold else StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(voice.name, style = MaterialTheme.typography.titleSmall)
                            Text(voice.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!voice.previewUrl.isNullOrBlank()) {
                            IconButton(onClick = { onPreviewVoice(voice.previewUrl) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Ouvir amostra", tint = StudioGold)
                            }
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selecionada", tint = StudioGold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSynthesizeProject,
                enabled = !uiState.isSynthesizingVoice && uiState.activeProject != null && uiState.settings.elevenLabsVoiceId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gerar narração ElevenLabs (MP3)")
            }
        }
    }
}

@Composable
private fun SoundEffectsSection(
    uiState: StudioUiState,
    viewModel: StudioViewModel
) {
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var label by remember { mutableStateOf("") }
    var startSec by remember { mutableStateOf("0") }
    var volumePercent by remember { mutableFloatStateOf(25f) }
    var sourceLib by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var licenseUrl by remember { mutableStateOf("") }
    var licenseTerms by remember { mutableStateOf("") }
    var commercialConfirmed by remember { mutableStateOf(false) }

    val sfxPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pickedUri = uri
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioPanelCard {
            StudioEyebrow("MÓDULO DE EFEITOS SONOROS")
            Text("Efeitos licenciados na linha do tempo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Use somente arquivos cuja licença autorize uso comercial e monetização no YouTube.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { sfxPicker.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (pickedUri != null) "Arquivo selecionado ✓" else "Selecionar efeito (MP3/WAV)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nome / finalidade") },
                placeholder = { Text("Ex.: sino suave para revelar descoberta") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = startSec,
                onValueChange = { startSec = it },
                label = { Text("Momento de entrada (segundos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text("Volume: ${volumePercent.roundToInt()}%", style = MaterialTheme.typography.labelMedium)
            Slider(value = volumePercent, onValueChange = { volumePercent = it }, valueRange = 2f..100f)

            OutlinedTextField(value = sourceLib, onValueChange = { sourceLib = it }, label = { Text("Origem / biblioteca") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Autor / criador") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = licenseUrl, onValueChange = { licenseUrl = it }, label = { Text("URL da licença") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = licenseTerms, onValueChange = { licenseTerms = it }, label = { Text("Condições da licença") }, modifier = Modifier.fillMaxWidth())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { commercialConfirmed = !commercialConfirmed },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = commercialConfirmed, onCheckedChange = { commercialConfirmed = it })
                Text("Confirmo que a licença permite uso comercial e monetização.", style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val uri = pickedUri ?: return@Button
                    viewModel.addSoundEffectToActiveProject(
                        uri = uri,
                        label = label,
                        startSeconds = startSec.toFloatOrNull() ?: 0f,
                        volumePercent = volumePercent.roundToInt(),
                        sourceLibrary = sourceLib,
                        author = author,
                        licenseUrl = licenseUrl,
                        licenseTerms = licenseTerms,
                        commercialConfirmed = commercialConfirmed
                    )
                    pickedUri = null; label = ""; sourceLib = ""; author = ""; licenseUrl = ""; licenseTerms = ""
                },
                enabled = pickedUri != null && commercialConfirmed && uiState.activeProject != null,
                colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Adicionar efeito ao projeto")
            }
        }

        val effects = uiState.activeProject?.soundEffects.orEmpty()
        if (effects.isNotEmpty()) {
            StudioPanelCard {
                Text("Efeitos cadastrados (${effects.size})", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                effects.forEach { sfx ->
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
                                Text(sfx.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Aos ${sfx.startSeconds}s · Vol ${sfx.volumePercent}% · ${sfx.sourceLibrary.ifBlank { sfx.filename }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.playSfx(sfx.filePath) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Ouvir efeito", tint = StudioGold)
                            }
                            IconButton(onClick = { viewModel.deleteSfx(sfx.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir efeito", tint = StudioCrimson)
                            }
                        }
                    }
                }
            }
        }
    }
}

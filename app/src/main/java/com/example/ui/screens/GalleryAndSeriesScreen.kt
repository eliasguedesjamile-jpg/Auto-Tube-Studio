package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.PageModel
import com.example.domain.model.PageModelsCatalog
import com.example.domain.model.SeriesEpisode
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.components.PageModelPickerDialog
import com.example.ui.components.StudioEyebrow
import com.example.ui.components.StudioPanelCard
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioPanelElevated
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryAndSeriesScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onCreateProjectWithModel: (PageModel) -> Unit,
    onSeriesCreatedNavigateToStudio: () -> Unit
) {
    var topTab by remember { mutableIntStateOf(0) } // 0 = 27 Modelos, 1 = Séries
    var inspectedModel by remember { mutableStateOf<PageModel?>(null) }

    inspectedModel?.let { model ->
        AlertDialog(
            onDismissRequest = { inspectedModel = null },
            containerColor = StudioPanel,
            title = {
                Text("${model.iconEmoji} ${model.name}", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(model.group, style = MaterialTheme.typography.labelSmall, color = StudioGold)
                    Text(model.description, style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = StudioPanelElevated,
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Direção visual automática:", style = MaterialTheme.typography.labelMedium, color = StudioGold)
                            Text(model.visualGuidance, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        inspectedModel = null
                        onCreateProjectWithModel(model)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioGold)
                ) {
                    Text("Criar vídeo neste modelo")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedModel = null }) { Text("Fechar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gallery_and_series_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioEyebrow("GALERIA VISUAL & SÉRIES")
                Text("27 modelos e estúdio de séries", style = MaterialTheme.typography.headlineSmall)
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = topTab == 0,
                    onClick = { topTab = 0 },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("tab_page_models")
                ) { Text("27 Modelos") }
                SegmentedButton(
                    selected = topTab == 1,
                    onClick = { topTab = 1 },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("tab_series_studio")
                ) { Text("Série de Vídeos") }
            }
        }

        if (topTab == 0) {
            item {
                var query by remember { mutableStateOf("") }
                var selectedGroup by remember { mutableStateOf("Todos") }
                val groups = listOf("Todos", "Novos formatos", "Galeria tradicional")
                val filtered = PageModelsCatalog.all.filter { model ->
                    val matchesGroup = selectedGroup == "Todos" || model.group == selectedGroup
                    val q = query.trim()
                    val matchesQuery = q.isEmpty() ||
                        model.name.contains(q, ignoreCase = true) ||
                        model.description.contains(q, ignoreCase = true)
                    matchesGroup && matchesQuery
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Buscar modelo: história, ciência, games...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(groups) { grp ->
                            FilterChip(
                                selected = selectedGroup == grp,
                                onClick = { selectedGroup = grp },
                                label = { Text(grp) }
                            )
                        }
                    }

                    filtered.forEach { model ->
                        val bg = Color(0xFF000000L or (model.bgHex and 0x00FFFFFFL))
                        val accent = Color(0xFF000000L or (model.accentHex and 0x00FFFFFFL))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inspectedModel = model }
                                .testTag("model_card_${model.id}"),
                            shape = MaterialTheme.shapes.medium,
                            color = StudioPanel,
                            border = BorderStroke(1.dp, StudioBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(model.iconEmoji, style = MaterialTheme.typography.titleLarge)
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(model.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(
                                        model.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(model.group, style = MaterialTheme.typography.labelSmall, color = accent)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                SeriesBuilderSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSeriesCreated = onSeriesCreatedNavigateToStudio
                )
            }
        }
    }
}

@Composable
private fun SeriesBuilderSection(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onSeriesCreated: () -> Unit
) {
    var seriesName by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(PageModelsCatalog.findById("vidas-ficcao")) }
    var episodeDuration by remember { mutableStateOf("8") }
    var seriesBible by remember { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }

    val episodes = remember {
        mutableStateListOf(
            SeriesEpisode(id = UUID.randomUUID().toString(), number = 1, title = "Episódio 1", fullScript = ""),
            SeriesEpisode(id = UUID.randomUUID().toString(), number = 2, title = "Episódio 2", fullScript = "")
        )
    }

    if (showModelPicker) {
        PageModelPickerDialog(
            selectedId = selectedModel.id,
            onSelect = { selectedModel = it },
            onDismiss = { showModelPicker = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioPanelCard {
            StudioEyebrow("PLANEJAMENTO NARRATIVO")
            Text("Criar série de vídeos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = seriesName,
                onValueChange = { seriesName = it },
                label = { Text("Nome da série") },
                placeholder = { Text("Ex.: Mistérios que desafiam a História") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .weight(2f)
                        .clickable { showModelPicker = true },
                    shape = MaterialTheme.shapes.small,
                    color = StudioPanelElevated,
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Text(
                        text = "${selectedModel.iconEmoji} ${selectedModel.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedTextField(
                    value = episodeDuration,
                    onValueChange = { episodeDuration = it.filter(Char::isDigit).take(2) },
                    label = { Text("Min/ep") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = seriesBible,
                onValueChange = { seriesBible = it },
                label = { Text("Bíblia da série (continuidade e tom)") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Episódios (${episodes.size})", style = MaterialTheme.typography.titleSmall)
                OutlinedButton(
                    onClick = {
                        episodes.add(
                            SeriesEpisode(
                                id = UUID.randomUUID().toString(),
                                number = episodes.size + 1,
                                title = "Episódio ${episodes.size + 1}",
                                fullScript = ""
                            )
                        )
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Episódio")
                }
            }

            Spacer(Modifier.height(6.dp))
            episodes.forEachIndexed { idx, ep ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    color = StudioPanelElevated,
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Episódio ${idx + 1}", style = MaterialTheme.typography.labelMedium, color = StudioGold)
                            if (episodes.size > 1) {
                                IconButton(onClick = { episodes.removeAt(idx) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover episódio", tint = StudioCrimson, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        OutlinedTextField(
                            value = ep.title,
                            onValueChange = { episodes[idx] = ep.copy(title = it) },
                            label = { Text("Título do episódio") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ep.fullScript,
                            onValueChange = { episodes[idx] = ep.copy(fullScript = it) },
                            label = { Text("Roteiro do episódio") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    viewModel.createSeriesAndProjects(
                        seriesName = seriesName,
                        pageTypeId = selectedModel.id,
                        episodeDurationMinutes = episodeDuration.toIntOrNull()?.coerceIn(1, 30) ?: 8,
                        bible = seriesBible,
                        episodes = episodes.toList(),
                        onCompleted = onSeriesCreated
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_series_projects_button")
            ) {
                Text("Criar projetos da série")
            }
        }

        if (uiState.seriesList.isNotEmpty()) {
            StudioPanelCard {
                Text("Séries já criadas (${uiState.seriesList.size})", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                uiState.seriesList.forEach { series ->
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(series.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${PageModelsCatalog.findById(series.pageTypeId).name} · ${series.episodes.size} episódio(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.deleteSeries(series.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover série", tint = StudioCrimson)
                            }
                        }
                    }
                }
            }
        }
    }
}

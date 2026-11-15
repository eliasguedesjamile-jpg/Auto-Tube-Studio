package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.VideoProject
import com.example.ui.StudioUiState
import com.example.ui.components.FirebaseDemoNoticeBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioEyebrow
import com.example.ui.components.StudioPanelCard
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioPanelElevated
import java.io.File

@Composable
fun HomeScreen(
    uiState: StudioUiState,
    onNavigateToStudioNew: () -> Unit,
    onOpenProjectInStudio: (String) -> Unit,
    onCreateTestProject: () -> Unit,
    onNavigateToModels: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card with generated studio art
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, StudioBorder),
                colors = CardDefaults.cardColors(containerColor = StudioPanel)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = stringResource(R.string.cd_hero_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xBB10131B),
                                        Color(0xD910131B),
                                        Color(0xF510131B)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioEyebrow("AUTO TUBE STUDIO · ANDROID")
                            StatusPill(
                                text = if (uiState.isFirebaseAiConnected) "Gemini Ativo" else "Modo Local",
                                ok = uiState.isFirebaseAiConnected
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Da ideia ao vídeo.\nCom identidade própria.",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Roteiros por cenas, 27 direções visuais, narração pt-BR, legendas SRT e MP4 16:9 no seu celular.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onNavigateToStudioNew,
                                colors = ButtonDefaults.buttonColors(containerColor = StudioGold),
                                modifier = Modifier.testTag("home_start_project_button")
                            ) {
                                Text("Novo projeto")
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = onCreateTestProject,
                                border = BorderStroke(1.dp, StudioGold.copy(alpha = 0.6f)),
                                modifier = Modifier.testTag("home_create_test_button")
                            ) {
                                Text("Vídeo teste", color = StudioGold)
                            }
                        }
                    }
                }
            }
        }

        // Notice when Firebase AI Logic is disabled
        if (!uiState.isFirebaseAiConnected) {
            item {
                FirebaseDemoNoticeBanner()
            }
        }

        // Narrative Formats Highlight
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StudioEyebrow("FORMATOS DE CONTEÚDO")
                Text(
                    text = "Escolha sua narrativa",
                    style = MaterialTheme.typography.titleLarge
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FormatHighlightCard(
                            badge = "◈",
                            title = "Vidas e Cotidiano",
                            subtitle = "Histórias humanas, culturas, profissões e modos de vida.",
                            tag = "Ficção · Documentário",
                            accentColor = StudioGold,
                            onClick = onNavigateToModels
                        )
                    }
                    item {
                        FormatHighlightCard(
                            badge = "⌕",
                            title = "True Crime",
                            subtitle = "Casos reais, apuração rigorosa e cronologia investigativa.",
                            tag = "Documentário criminal",
                            accentColor = StudioCrimson,
                            onClick = onNavigateToModels
                        )
                    }
                    item {
                        FormatHighlightCard(
                            badge = "✦",
                            title = "Galeria Visual (27)",
                            subtitle = "História, Ciência, Finanças, Mistérios, Games, Mapas e muito mais.",
                            tag = "27 modelos prontos",
                            accentColor = StudioBlue,
                            onClick = onNavigateToModels
                        )
                    }
                }
            }
        }

        // Quick Tools Row (Aulas, Voz, Diagnóstico)
        item {
            StudioPanelCard {
                StudioEyebrow("FLUXO RÁPIDO DO ESTÚDIO")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickTile(
                        icon = Icons.Default.MovieCreation,
                        label = "Voz & Áudio",
                        tint = StudioMint,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_voice_tile"),
                        onClick = onNavigateToVoice
                    )
                    QuickTile(
                        icon = Icons.Default.School,
                        label = "Aulas (4)",
                        tint = StudioGold,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_lessons_tile"),
                        onClick = onNavigateToSettings
                    )
                    QuickTile(
                        icon = Icons.Default.Speed,
                        label = "Diagnóstico",
                        tint = StudioBlue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_diagnostics_tile"),
                        onClick = onNavigateToSettings
                    )
                }
            }
        }

        // Recent Projects Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    StudioEyebrow("SEUS TRABALHOS")
                    Text("Projetos recentes", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "${uiState.projects.size} no aparelho",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.projects.isEmpty()) {
            item {
                StudioPanelCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StudioGold,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "Nenhum projeto criado ainda",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Toque em \"Novo projeto\" ou crie um \"Vídeo teste\" para experimentar o editor de cenas, TTS offline e renderização MP4.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.projects.take(6), key = { it.id }) { project ->
                RecentProjectRow(
                    project = project,
                    isActive = project.id == uiState.activeProject?.id,
                    onClick = { onOpenProjectInStudio(project.id) }
                )
            }
        }
    }
}

@Composable
private fun FormatHighlightCard(
    badge: String,
    title: String,
    subtitle: String,
    tag: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(235.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = StudioPanel),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(badge, color = accentColor, style = MaterialTheme.typography.titleMedium)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                tag,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun QuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(74.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = StudioPanelElevated,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
fun RecentProjectRow(
    project: VideoProject,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val model = project.pageModel
    val hasMp4 = !project.renderedVideoPath.isNullOrBlank() && File(project.renderedVideoPath).exists()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_row_${project.id}"),
        shape = MaterialTheme.shapes.medium,
        color = if (isActive) StudioPanelElevated else StudioPanel,
        border = BorderStroke(1.dp, if (isActive) StudioGold else StudioBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF000000L or (model.accentHex and 0x00FFFFFFL)).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(model.iconEmoji, style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${model.name} · ${project.scenes.size} cenas · ${project.targetDurationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasMp4) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "MP4 renderizado",
                    tint = StudioMint,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Abrir projeto",
                    tint = StudioGold
                )
            }
        }
    }
}

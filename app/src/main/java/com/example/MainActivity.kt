package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.StudioViewModel
import com.example.ui.screens.GalleryAndSeriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsLibraryScreen
import com.example.ui.screens.SettingsAndHelpScreen
import com.example.ui.screens.StudioWorkspaceScreen
import com.example.ui.screens.VoiceAndSfxScreen
import com.example.ui.theme.AutoTubeStudioTheme
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioGoldDark
import com.example.ui.theme.StudioOnGold
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioSidebar

enum class StudioRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Início", Icons.Filled.Home, Icons.Outlined.Home),
    STUDIO("studio", "Estúdio", Icons.Filled.MovieCreation, Icons.Outlined.MovieCreation),
    VOICE("voice", "Voz", Icons.Filled.Mic, Icons.Outlined.Mic),
    MODELS("models", "Modelos", Icons.Filled.GridView, Icons.Outlined.GridView),
    SETTINGS("settings", "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings),
    PROJECTS("projects", "Meus Vídeos", Icons.Filled.FolderCopy, Icons.Filled.FolderCopy)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoTubeStudioTheme(darkTheme = true) {
                AutoTubeStudioApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTubeStudioApp(
    viewModel: StudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: StudioRoute.HOME.route

    val snackbarHostState = remember { SnackbarHostState() }
    var forceNewProject by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusBanner) {
        val msg = uiState.statusBanner ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    fun navigateTo(route: StudioRoute) {
        navController.navigate(route.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bottomNavItems = listOf(
        StudioRoute.HOME,
        StudioRoute.STUDIO,
        StudioRoute.VOICE,
        StudioRoute.MODELS,
        StudioRoute.SETTINGS
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBg)
    ) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = StudioSidebar,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.size(16.dp))
                    BrandMarkBadge()
                    Spacer(Modifier.size(20.dp))
                    (bottomNavItems + StudioRoute.PROJECTS).forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (dest == StudioRoute.STUDIO) forceNewProject = false
                                navigateTo(dest)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }

            Scaffold(
                containerColor = StudioBg,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = StudioSidebar,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!useRail) {
                                    BrandMarkBadge()
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(
                                    text = "Auto Tube Studio",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { navigateTo(StudioRoute.PROJECTS) },
                                modifier = Modifier.testTag("topbar_projects_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderCopy,
                                    contentDescription = "Biblioteca de vídeos",
                                    tint = if (currentRoute == StudioRoute.PROJECTS.route) StudioGold else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = StudioSidebar
                        ) {
                            bottomNavItems.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (dest == StudioRoute.STUDIO) forceNewProject = false
                                        navigateTo(dest)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = StudioOnGold,
                                        selectedTextColor = StudioGold,
                                        indicatorColor = StudioGold,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = if (uiState.isErrorBanner) StudioCrimson else StudioPanel,
                            contentColor = Color.White
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = StudioRoute.HOME.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(StudioRoute.HOME.route) {
                        HomeScreen(
                            uiState = uiState,
                            onNavigateToStudioNew = {
                                forceNewProject = true
                                navigateTo(StudioRoute.STUDIO)
                            },
                            onOpenProjectInStudio = { projectId ->
                                forceNewProject = false
                                viewModel.selectProject(projectId)
                                navigateTo(StudioRoute.STUDIO)
                            },
                            onCreateTestProject = {
                                viewModel.createSampleTestProject {
                                    forceNewProject = false
                                    navigateTo(StudioRoute.STUDIO)
                                }
                            },
                            onNavigateToModels = { navigateTo(StudioRoute.MODELS) },
                            onNavigateToVoice = { navigateTo(StudioRoute.VOICE) },
                            onNavigateToSettings = { navigateTo(StudioRoute.SETTINGS) }
                        )
                    }

                    composable(StudioRoute.STUDIO.route) {
                        BackHandler { navigateTo(StudioRoute.HOME) }
                        StudioWorkspaceScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            forceNewMode = forceNewProject,
                            onDoneNewMode = { forceNewProject = false },
                            onNavigateToVoice = { navigateTo(StudioRoute.VOICE) }
                        )
                    }

                    composable(StudioRoute.VOICE.route) {
                        BackHandler { navigateTo(StudioRoute.HOME) }
                        VoiceAndSfxScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }

                    composable(StudioRoute.MODELS.route) {
                        BackHandler { navigateTo(StudioRoute.HOME) }
                        GalleryAndSeriesScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onCreateProjectWithModel = { model ->
                                viewModel.createNewProject(
                                    title = "Novo ${model.name}",
                                    pageTypeId = model.id,
                                    durationMinutes = 8,
                                    fullScript = "",
                                    illustrationBadge = model.isFactual
                                ) {
                                    forceNewProject = false
                                    navigateTo(StudioRoute.STUDIO)
                                }
                            },
                            onSeriesCreatedNavigateToStudio = {
                                forceNewProject = false
                                navigateTo(StudioRoute.STUDIO)
                            }
                        )
                    }

                    composable(StudioRoute.PROJECTS.route) {
                        BackHandler { navigateTo(StudioRoute.HOME) }
                        ProjectsLibraryScreen(
                            uiState = uiState,
                            onOpenProject = { projectId ->
                                forceNewProject = false
                                viewModel.selectProject(projectId)
                                navigateTo(StudioRoute.STUDIO)
                            },
                            onDeleteProject = { projectId ->
                                viewModel.deleteProject(projectId)
                            },
                            onShareMp4 = { projectId ->
                                viewModel.selectProject(projectId)
                                viewModel.shareRenderedMp4()
                            }
                        )
                    }

                    composable(StudioRoute.SETTINGS.route) {
                        BackHandler { navigateTo(StudioRoute.HOME) }
                        SettingsAndHelpScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateToStudio = {
                                forceNewProject = false
                                navigateTo(StudioRoute.STUDIO)
                            },
                            onNavigateToVoice = { navigateTo(StudioRoute.VOICE) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandMarkBadge() {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(StudioGold, StudioGoldDark))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "AT",
            color = StudioOnGold,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

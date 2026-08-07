package com.example.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import com.example.R
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch
import com.example.data.ChatKeyManager
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.lazy.LazyRow
import com.example.ui.theme.success
import com.example.ui.theme.warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "settings_home",
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        composable("settings_home") {
            SettingsHomeScreen(navController = navController, viewModel = viewModel)
        }
        composable("settings_general") {
            GeneralSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings_voice") {
            VoiceSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings_system") {
            SystemSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings_memory") {
            MemorySettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings_accessibility") {
            AccessibilitySettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("theme_screen") {
            com.example.ui.settings.ThemeScreen(navController = navController, viewModel = viewModel)
        }
        composable("offline_status_dashboard") {
            OfflineStatusDashboardScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings_shizuku") {
            ShizukuSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(navController: NavController, viewModel: AiraViewModel) {
    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp, // Section Title: 22sp SemiBold
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 8.dp), // Outer Screen Padding: 24dp
            verticalArrangement = Arrangement.spacedBy(18.dp) // Between Cards: 18dp
        ) {
            if (isOfflineBrain) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_offline_mode_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(colorResource(id = R.color.aira_success_light), CircleShape)
                                )
                                Text(
                                    text = "Offline Mode Active",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Private On-Device AI • Voice Ready",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "System",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp, // Labels: 13sp Medium
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp)
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Offline Status Dashboard",
                    subtitle = "Live status, model progress & HW acceleration",
                    icon = Icons.Default.Memory,
                    testTag = "settings_tab_offline_dashboard",
                    onClick = { navController.navigate("offline_status_dashboard") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "General",
                    subtitle = "Theme, FPS, Access",
                    icon = Icons.Default.Palette,
                    testTag = "settings_tab_general",
                    onClick = { navController.navigate("settings_general") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Theme",
                    subtitle = "Cosmic Color Schemes",
                    icon = Icons.Default.Palette,
                    testTag = "settings_tab_theme",
                    onClick = { navController.navigate("theme_screen") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Voice",
                    subtitle = "Wake, Listen, Voice, Sound",
                    icon = Icons.Default.Mic,
                    testTag = "settings_tab_voice",
                    onClick = { navController.navigate("settings_voice") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "AI",
                    subtitle = "Keys, Local AI, Reasoning",
                    icon = Icons.Default.Memory,
                    testTag = "settings_tab_system",
                    onClick = { navController.navigate("settings_system") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Memory",
                    subtitle = "Long-Term, Backup, Restore",
                    icon = Icons.Default.Memory,
                    testTag = "settings_tab_memory",
                    onClick = { navController.navigate("settings_memory") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Accessibility & VoiceOver",
                    subtitle = "Reduce Motion, High Contrast & Live Status",
                    icon = Icons.Default.Palette,
                    testTag = "settings_tab_accessibility",
                    onClick = { navController.navigate("settings_accessibility") }
                )
            }

            item {
                SettingsCategoryItem(
                    title = "Shizuku ADB Integration",
                    subtitle = "System privileged control, status & setup guide",
                    icon = Icons.Default.Lock,
                    testTag = "settings_tab_shizuku",
                    onClick = { navController.navigate("settings_shizuku") }
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Premium dark surface
        shape = RoundedCornerShape(22.dp), // Corner Radius: 22dp
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp, // Card Title: 18sp Medium
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp, // Caption: 14sp
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp) // Icons: 24dp
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .defaultMinSize(minHeight = 64.dp) // Height: 64dp minimum
                .padding(vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val themeIndex by viewModel.themeIndex.collectAsState()
    val lowPerf by viewModel.lowPerformanceMode.collectAsState()

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "General Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp, // Section Title: 22sp
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("general_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp), // Outer Screen Padding: 24dp
            verticalArrangement = Arrangement.spacedBy(28.dp) // Between Sections: 28dp
        ) {
            // CARD 2: Theme Picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Visual Scheme",
                        fontSize = 18.sp, // Card Title: 18sp Medium
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Presets Grid
                    Text(
                        text = "Presets",
                        fontSize = 13.sp, // Labels: 13sp Medium
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            Triple(0, "Premium", com.example.ui.theme.PrimaryBlue)
                        )

                        presets.forEach { (index, label, color) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color.copy(alpha = 0.06f))
                                    .border(
                                        width = if (themeIndex == index) 1.5.dp else 1.dp,
                                        color = if (themeIndex == index) color else color.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.updateThemeIndex(index)
                                    }
                                    .testTag("theme_preset_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = color,
                                    fontSize = 13.sp, // Labels: 13sp Medium
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // CARD 4: Performance
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "30 FPS Battery Saver",
                            fontSize = 18.sp, // Card Title: 18sp
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Reduces refresh rate to save resource consumption on lightweight devices.",
                            fontSize = 14.sp, // Caption: 14sp
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 18.sp
                        )
                    }

                    Switch(
                        checked = lowPerf,
                        onCheckedChange = { viewModel.toggleLowPerformanceMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("low_perf_switch")
                    )
                }
            }

            // CARD 5: Accessibility Service Integration Guidance
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Accessibility Integration",
                        fontSize = 18.sp, // Card Title: 18sp
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "To enable automation triggers and global system tasks, please authorize always-on accessibility options.",
                        fontSize = 14.sp, // Caption: 14sp
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "System Settings > Accessibility > Aira Jarvis",
                        fontSize = 13.sp, // Labels: 13sp
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val wakeWord by viewModel.wakeWord.collectAsState()
    val speakReplies by viewModel.speakReplies.collectAsState()
    val usePiperTts by viewModel.usePiperTts.collectAsState()
    val usePiperTtsOffline by viewModel.usePiperTtsOffline.collectAsState()
    val piperSpeed by viewModel.piperSpeed.collectAsState()
    val piperActiveVoice by viewModel.piperActiveVoice.collectAsState()
    val piperIsModelDownloaded by viewModel.piperIsModelDownloaded.collectAsState()
    val piperDownloadProgress by viewModel.piperDownloadProgress.collectAsState()
    val piperAvailableVoices = viewModel.piperAvailableVoices
    val selectedTtsEngine by viewModel.selectedTtsEngine.collectAsState()
    val selectedSttEngine by viewModel.selectedSttEngine.collectAsState()
    val isLocalMode by viewModel.isLocalMode.collectAsState()
    val modelReadyState by viewModel.modelReadyState.collectAsState()
    val voicePitch by viewModel.voicePitch.collectAsState()

    val googleTtsAvailableLanguages by viewModel.googleTtsAvailableLanguages.collectAsState()
    val googleTtsAvailableVoices by viewModel.googleTtsAvailableVoices.collectAsState()
    val googleTtsSelectedLanguage by viewModel.googleTtsSelectedLanguage.collectAsState()
    val googleTtsSelectedVoice by viewModel.googleTtsSelectedVoice.collectAsState()

    // --- Custom Trigger Training & Testing States ---
    val isTrainingWakeWord by viewModel.isTrainingWakeWord.collectAsState()
    val trainingCurrentStep by viewModel.trainingCurrentStep.collectAsState()
    val trainingWakeWordText by viewModel.trainingWakeWordText.collectAsState()
    val trainingAttempts by viewModel.trainingAttempts.collectAsState()
    val trainingQualityScore by viewModel.trainingQualityScore.collectAsState()
    val isRecordingAttempt by viewModel.isRecordingAttempt.collectAsState()
    val trainingLiveAmplitude by viewModel.trainingLiveAmplitude.collectAsState()

    val isTestingWakeWord by viewModel.isTestingWakeWord.collectAsState()
    val isTestWakeWordTriggered by viewModel.isTestWakeWordTriggered.collectAsState()
    val testTriggerText by viewModel.testTriggerText.collectAsState()

    val trainedWakeWords by viewModel.trainedWakeWords.collectAsState()

    var tempWakeWord by remember(wakeWord) { mutableStateOf(wakeWord) }
    val scrollState = rememberScrollState()

    var isOfflineTtsEnabled by remember { mutableStateOf(viewModel.piperTtsManager.isOfflineTtsEnabled) }
    var isDownloadingAmy by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Voice Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp, // Section Title
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("voice_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp), // Outer Screen Padding: 24dp
            verticalArrangement = Arrangement.spacedBy(28.dp) // Between Sections: 28dp
        ) {
            // CARD 1: Wake Word Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Voice Recognition Trigger",
                        fontSize = 18.sp, // Card Title: 18sp Medium
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Configure the high-precision wake keyword, processed entirely offline locally.",
                        fontSize = 14.sp, // Caption: 14sp
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 18.sp
                    )

                    TextField(
                        value = tempWakeWord,
                        onValueChange = {
                            tempWakeWord = it
                            viewModel.updateWakeWord(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wake_word_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp), // Inputs: 16dp
                        placeholder = { Text("Trigger Keyword", fontSize = 16.sp, fontFamily = FontFamily.SansSerif, color = MaterialTheme.colorScheme.outline) }
                    )

                    // SECTION A: VOICE TRAINING FLOW
                    AnimatedVisibility(
                        visible = isTrainingWakeWord,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Custom Trigger voice Training",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Step $trainingCurrentStep of 3",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "To train, please click 'Record' and speak the word '$trainingWakeWordText' clearly when prompted.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )

                                // Visual Step Badges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 1..3) {
                                        val isDone = trainingAttempts.size >= i
                                        val isActive = trainingCurrentStep == i && !isDone
                                        
                                        val badgeBg = when {
                                            isDone -> MaterialTheme.colorScheme.success.copy(alpha = 0.12f)
                                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                        val badgeBorderColor = when {
                                            isDone -> MaterialTheme.colorScheme.success
                                            isActive -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        }
                                        val badgeTextColor = when {
                                            isDone -> MaterialTheme.colorScheme.success
                                            isActive -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(badgeBg)
                                                .border(1.dp, badgeBorderColor, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 4.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isDone) "✓ Attempt $i" else "Attempt $i",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = badgeTextColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Amplitude Wave Visualizer
                                if (isRecordingAttempt) {
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                            val width = size.width
                                            val height = size.height
                                            val midY = height / 2f
                                            val barCount = 30
                                            val barWidth = 6f
                                            val gap = 12f
                                            val totalBarWidth = barCount * barWidth + (barCount - 1) * gap
                                            val startX = (width - totalBarWidth) / 2f

                                            for (j in 0 until barCount) {
                                                val distanceFromCenter = Math.abs(j - barCount / 2f) / (barCount / 2f)
                                                val factor = 1f - distanceFromCenter
                                                val animOffset = Math.sin((j * 0.25 + System.currentTimeMillis() * 0.007)).toFloat() * 0.2f
                                                val barHeight = ((trainingLiveAmplitude * 1.6f + 0.15f) * height * factor * (1f + animOffset)).coerceIn(8f, height)

                                                val x = startX + j * (barWidth + gap)
                                                drawRoundRect(
                                                    color = primaryColor.copy(alpha = 0.4f + trainingLiveAmplitude * 0.6f),
                                                    topLeft = androidx.compose.ui.geometry.Offset(x, midY - barHeight / 2f),
                                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive State Description Badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = trainingQualityScore,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (trainingQualityScore.contains("Excellent")) MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }

                                // Step Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (isRecordingAttempt) {
                                        Button(
                                            onClick = { viewModel.stopRecordingAttempt() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Filled.Mic, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Stop", fontSize = 13.sp)
                                        }
                                    } else if (trainingAttempts.size < 3) {
                                        Button(
                                            onClick = { viewModel.startRecordingAttempt() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Filled.Mic, contentDescription = "Record", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Record $trainingCurrentStep", fontSize = 13.sp)
                                        }
                                    }

                                    if (trainingAttempts.size >= 3) {
                                        Button(
                                            onClick = { viewModel.saveAndActivateTrainedWakeWord() },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.success)
                                        ) {
                                            // COLOR SYSTEM v1
                                            Text("Save & Active", color = MaterialTheme.colorScheme.surface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.stopWakeWordTraining() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                    ) {
                                        Text("Cancel", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (!isTrainingWakeWord) {
                        Button(
                            onClick = { viewModel.startWakeWordTraining(tempWakeWord) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("train_voice_trigger_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = "Train voice trigger", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record & Train Custom Trigger", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // SECTION B: TESTING PAD MODE
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.01f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = "Test mode",
                                        tint = if (isTestingWakeWord) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Voice Trigger Testing Pad",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = isTestingWakeWord,
                                    onCheckedChange = { viewModel.toggleTestingWakeWord(it) },
                                    modifier = Modifier.testTag("testing_wake_word_switch"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            }

                            AnimatedVisibility(visible = isTestingWakeWord) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isTestWakeWordTriggered) MaterialTheme.colorScheme.success.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                1.dp,
                                                if (isTestWakeWordTriggered) MaterialTheme.colorScheme.success
                                                else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                         ) {
                                            if (isTestWakeWordTriggered) {
                                                Text(
                                                    text = "🔥 TRIGGERED!",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.success
                                                )
                                            }
                                            Text(
                                                text = testTriggerText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION C: CUSTOM PROFILE MANAGEMENT
                    if (trainedWakeWords.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Trained voice Profiles",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            trainedWakeWords.forEach { item ->
                                val isCurrentActive = tempWakeWord.equals(item.word, ignoreCase = true)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.activateTrainedWakeWord(item.id, item.word) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.01f)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrentActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = item.word,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isCurrentActive) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(MaterialTheme.colorScheme.success.copy(alpha = 0.12f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "ACTIVE",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.success
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Stability: ${item.quality} • Trained 3/3 attempts",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteTrainedWakeWord(item.id, item.word) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete profile",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    val usePersistentList by viewModel.usePersistentListening.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Always Listening",
                                fontSize = 16.sp, // Body: 16sp
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Continuously listen for triggers in background.",
                                fontSize = 14.sp, // Caption: 14sp
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 18.sp
                            )
                        }
                        Switch(
                            checked = usePersistentList,
                            onCheckedChange = { viewModel.togglePersistentListening(it) },
                            modifier = Modifier.testTag("persistent_listening_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }

            // CARD: VOICE ENGINE SELECTION
            Card(
                modifier = Modifier.fillMaxWidth().testTag("voice_engine_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice Engine",
                            fontSize = 18.sp, // Card Title: 18sp Medium
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { viewModel.speakText("This is a test of the selected voice engine.") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("test_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Test Voice Icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Test Voice",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Option 1: Auto
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setSelectedTtsEngine(AiraViewModel.TtsEngine.AUTO) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RadioButton(
                                selected = selectedTtsEngine == AiraViewModel.TtsEngine.AUTO,
                                onClick = { viewModel.setSelectedTtsEngine(AiraViewModel.TtsEngine.AUTO) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("engine_auto_radio")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "Use Google TTS when online, fallback to Piper when offline",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.SansSerif,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Option 2: Google TTS Online
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setSelectedTtsEngine(AiraViewModel.TtsEngine.GOOGLE_TTS) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RadioButton(
                                selected = selectedTtsEngine == AiraViewModel.TtsEngine.GOOGLE_TTS,
                                onClick = { viewModel.setSelectedTtsEngine(AiraViewModel.TtsEngine.GOOGLE_TTS) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("engine_google_radio")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google TTS Online",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "Always use system Google TextToSpeech",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.SansSerif,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Option 3: Amy Offline
                        val isAmyDownloaded = isOfflineTtsEnabled || modelReadyState == AiraViewModel.VoiceAssistantState.READY
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = isAmyDownloaded) { viewModel.setSelectedTtsEngine(AiraViewModel.TtsEngine.PIPER_OFFLINE) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RadioButton(
                                selected = selectedTtsEngine == AiraViewModel.TtsEngine.PIPER_OFFLINE,
                                onClick = { if (isAmyDownloaded) viewModel.setSelectedTtsEngine(AiraViewModel.TtsEngine.PIPER_OFFLINE) },
                                enabled = isAmyDownloaded,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledSelectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    disabledUnselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("engine_piper_radio")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Piper ONNX (Amy)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isAmyDownloaded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "Always use offline high-quality natural voice",
                                    fontSize = 13.sp,
                                    color = if (isAmyDownloaded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    fontFamily = FontFamily.SansSerif,
                                    lineHeight = 17.sp
                                )
                                if (!isAmyDownloaded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Download required",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier.testTag("download_required_text")
                                    )
                                }
                            }
                        }

                        if (!isOfflineTtsEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (isDownloadingAmy) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = "Downloading voice files...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isDownloadingAmy = true
                                        coroutineScope.launch {
                                            viewModel.piperTtsManager.downloadAmyModel(
                                                onProgress = {},
                                                onComplete = { success ->
                                                    isDownloadingAmy = false
                                                    if (success) {
                                                        isOfflineTtsEnabled = true
                                                        android.widget.Toast.makeText(viewModel.getApplication(), "Amy voice downloaded successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        android.widget.Toast.makeText(viewModel.getApplication(), "Failed to download Amy voice", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("download_amy_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(text = "Download Amy Voice ~50MB", fontFamily = FontFamily.SansSerif)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val context = viewModel.getApplication<android.app.Application>()
                                val intent = android.content.Intent(context, com.example.PiperTtsTestActivity::class.java).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("jni_diagnostic_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Run System Voice Diagnostic Test", fontFamily = FontFamily.SansSerif)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Speech Pitch",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = String.format("%.2f", voicePitch),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Slider(
                            value = voicePitch,
                            onValueChange = { viewModel.setSpeechPitch(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("speech_pitch_slider")
                        )
                    }
                }
            }

            // CARD: LOCAL MODE TOGGLE
            Card(
                modifier = Modifier.fillMaxWidth().testTag("local_mode_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocalMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(22.dp),
                border = if (isLocalMode) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isLocalMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isLocalMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Local Mode",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isLocalMode) "Processing all voice commands locally via integrated Vosk library" else "Automatic routing (Vosk used offline or when enabled)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                        Switch(
                            checked = isLocalMode,
                            onCheckedChange = { viewModel.toggleLocalMode(it) },
                            modifier = Modifier.testTag("local_mode_toggle_switch")
                        )
                    }
                }
            }

            // CARD: SPEECH RECOGNITION ENGINE (STT)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("speech_recognition_engine_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Voice Input Engine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose the engine used to translate your spoken voice commands into text.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 18.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Option 1: Auto-routing
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setSelectedSttEngine(AiraViewModel.SttEngine.AUTO) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RadioButton(
                                selected = selectedSttEngine == AiraViewModel.SttEngine.AUTO,
                                onClick = { viewModel.setSelectedSttEngine(AiraViewModel.SttEngine.AUTO) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("stt_engine_auto_radio")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Automatic (Online & Offline)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "Uses fast cloud recognition when connected, and automatically switches to private offline recognition when offline.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.SansSerif,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Option 2: Always Offline (Vosk)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setSelectedSttEngine(AiraViewModel.SttEngine.VOSK_OFFLINE) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RadioButton(
                                selected = selectedSttEngine == AiraViewModel.SttEngine.VOSK_OFFLINE,
                                onClick = { viewModel.setSelectedSttEngine(AiraViewModel.SttEngine.VOSK_OFFLINE) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("stt_engine_vosk_radio")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Always Private Offline",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "Processes all voice commands on your phone without sending any voice data over the internet.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.SansSerif,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            // CARD: GOOGLE TTS CUSTOMIZATION (DYNAMIC LANGUAGES & VOICES)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("google_tts_custom_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Google Voice Customization",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Choose preferred language and voice style for speech responses.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 17.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Row 1: Language Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showLanguageDialog = true }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Language",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Language",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (googleTtsSelectedLanguage.isNotEmpty()) {
                                        java.util.Locale.forLanguageTag(googleTtsSelectedLanguage).displayName.ifEmpty { googleTtsSelectedLanguage }
                                    } else "English (United States)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Select Language",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Row 2: Voice Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showVoiceDialog = true }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Model",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Voice Style",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = googleTtsSelectedVoice.substringAfterLast(".").ifEmpty { "Default system voice" },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Select Voice Model",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // CARD: PIPER NEURAL OFFLINE TTS CONTROL CENTER
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Speech Synthesis Engine",
                        fontSize = 18.sp, // Card Title: 18sp Medium
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Use Offline Neural Voice",
                                fontSize = 16.sp, // Body: 16sp
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Process text-to-speech utilizing embedded models.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        Switch(
                            checked = usePiperTts,
                            onCheckedChange = { viewModel.togglePiperTts(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("piper_tts_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Speak Responses Automatically",
                                fontSize = 16.sp, // Body: 16sp
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Speak newly synthesized Jarvis responses instantly upon receipt.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 18.sp
                            )
                        }

                        Switch(
                            checked = speakReplies,
                            onCheckedChange = { viewModel.toggleSpeakReplies(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("speak_replies_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Use Natural Offline Voice",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Speak AI replies using the local Piper Amy engine.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 18.sp
                            )
                        }

                        Switch(
                            checked = usePiperTtsOffline,
                            onCheckedChange = { viewModel.togglePiperTtsOffline(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("use_piper_tts_offline_switch")
                        )
                    }

                    if (usePiperTtsOffline) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Piper Speech Speed",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = String.format("%.2fx", piperSpeed),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                            Slider(
                                value = piperSpeed,
                                onValueChange = { viewModel.setPiperSpeed(it) },
                                valueRange = 0.8f..1.2f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("piper_speed_slider")
                            )
                        }
                    }

                    if (usePiperTts) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(
                            text = "Available Models",
                            fontSize = 13.sp, // Labels: 13sp Medium
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            piperAvailableVoices.forEach { voice ->
                                val isDownloaded = piperIsModelDownloaded[voice.id] == true
                                val isCurrent = piperActiveVoice == voice.id
                                val downloadProg = piperDownloadProgress[voice.id]

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            if (isDownloaded) {
                                                viewModel.updatePiperVoice(voice.id)
                                            } else if (downloadProg == null) {
                                                viewModel.downloadPiperModel(voice.id)
                                            }
                                        }
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = voice.displayName,
                                            fontSize = 16.sp, // Body
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.SansSerif,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (voice.gender == "Female") MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (voice.gender == "Female") "Female" else "Male",
                                                    fontSize = 13.sp, // Labels: 13sp
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = FontFamily.SansSerif,
                                                    color = if (voice.gender == "Female") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            if (!isDownloaded && downloadProg == null) {
                                                Text(
                                                    text = "DOWNLOAD",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontFamily = FontFamily.SansSerif,
                                                    modifier = Modifier
                                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = "${voice.quality.uppercase()} • LATENCY: ${voice.latencyMs}MS",
                                        fontSize = 14.sp, // Caption: 14sp
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = voice.description,
                                        fontSize = 14.sp, // Caption: 14sp
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    if (downloadProg != null) {
                                        Spacer(Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = downloadProg,
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Google TTS Language Model", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(googleTtsAvailableLanguages.size) { index ->
                            val locale = googleTtsAvailableLanguages[index]
                            val isSelected = googleTtsSelectedLanguage == locale.toLanguageTag()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setGoogleTtsLanguage(locale.toLanguageTag())
                                        showLanguageDialog = false
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setGoogleTtsLanguage(locale.toLanguageTag())
                                        showLanguageDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = locale.displayName.ifEmpty { locale.language },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = { Text("Google TTS Voice Model", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    if (googleTtsAvailableVoices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No distinct voice models found. Default voice will be used.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(googleTtsAvailableVoices.size) { index ->
                                val voice = googleTtsAvailableVoices[index]
                                val isSelected = googleTtsSelectedVoice == voice.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            viewModel.setGoogleTtsVoice(voice.name)
                                            showVoiceDialog = false
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setGoogleTtsVoice(voice.name)
                                            showVoiceDialog = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = voice.name.substringAfterLast("."),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                        Text(
                                            text = "Features: " + (if (voice.isNetworkConnectionRequired) "Online" else "Offline / Local"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemSettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val isOffline by viewModel.isOfflineBrain.collectAsState()
    val onlineModel by viewModel.onlineModel.collectAsState()
    val llamaThreads by viewModel.llamaThreads.collectAsState()
    val isDeviceMemoryCapable by viewModel.isDeviceMemoryCapable.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI System Configuration",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp, // Section Title
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("system_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp), // Outer Screen Padding
            verticalArrangement = Arrangement.spacedBy(28.dp) // Spacing between sections
        ) {
            // CARD 1B: API Keys (Accordion Mode)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Secure API Keys",
                                    fontSize = 18.sp, // Card Title
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isExpanded) "Tap to hide API keys" else "Tap to view API keys",
                                    fontSize = 14.sp, // Caption
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Arrow Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    val context = LocalContext.current
                    val keyManager = remember { ChatKeyManager.getInstance(context) }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )

                            Text(
                                text = "Multiple keys help keep online AI services connected smoothly. All keys are encrypted and saved securely on your device.",
                                fontSize = 14.sp, // Caption
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 18.sp
                            )

                            (1..20).forEach { index ->
                                var keyValue by remember { mutableStateOf(keyManager.getKey(index)) }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Gemini Key #${index}",
                                        fontSize = 13.sp, // Labels
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )

                                    TextField(
                                        value = keyValue,
                                        onValueChange = { newValue ->
                                            keyValue = newValue
                                            keyManager.saveKey(index, newValue)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("gemini_api_key_input_$index"),
                                        placeholder = {
                                            Text(
                                                text = "Unconfigured slot",
                                                fontFamily = FontFamily.SansSerif,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 16.sp
                                            )
                                        },
                                        textStyle = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            cursorColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }

                            // Groq API Key Included in Accordion
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )

                            var groqKeyValue by remember { mutableStateOf(keyManager.getGroqKey()) }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Groq AI Endpoint Key",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                TextField(
                                    value = groqKeyValue,
                                    onValueChange = { newValue ->
                                        groqKeyValue = newValue
                                        keyManager.saveGroqKey(newValue)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("groq_api_key_input"),
                                    placeholder = {
                                        Text(
                                            text = "Enter Groq key",
                                            fontFamily = FontFamily.SansSerif,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 16.sp
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // CARD 3: AI Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "AI Intelligence Mode",
                        fontSize = 18.sp, // Card Title
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!isDeviceMemoryCapable) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Disabled on devices with less than 3GB RAM to keep performance fast and stable. Online AI will be used instead.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontFamily = FontFamily.SansSerif,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Offline Private AI Mode",
                                fontSize = 16.sp, // Body
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = if (isDeviceMemoryCapable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (isDeviceMemoryCapable) "Process conversations directly on your phone for complete privacy without using external servers."
                                       else "Not recommended for your device (<3GB RAM). Switch disabled for memory stability.",
                                fontSize = 14.sp, // Caption
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 18.sp
                            )
                        }

                        Switch(
                            checked = isOffline && isDeviceMemoryCapable,
                            enabled = isDeviceMemoryCapable,
                            onCheckedChange = { viewModel.toggleOfflineBrain(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("offline_brain_switch")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )

                    val context = LocalContext.current

                    if (!isOffline) {
                        // Online Mode Active
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Online Engine",
                                fontSize = 16.sp, // Body
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("GeminiApi", "GroqApi").forEach { model ->
                                    val isSelected = onlineModel == when (model) {
                                        "GeminiApi" -> "Gemini API"
                                        "GroqApi" -> "Groq API"
                                        else -> model
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.updateOnlineModel(
                                                    when (model) {
                                                        "GeminiApi" -> "Gemini API"
                                                        "GroqApi" -> "Groq API"
                                                        else -> model
                                                    }
                                                )
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("online_model_btn_${model.replace(" ", "_")}")
                                    ) {
                                        Text(
                                            text = when (model) {
                                                "GeminiApi" -> "Gemini"
                                                "GroqApi" -> "Groq"
                                                else -> model
                                            },
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 13.sp, // Labels
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Offline Mode Active
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Offline AI Status",
                                fontSize = 16.sp, // Body
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Offline Model Status",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = viewModel.getLlamaEngineStatus(),
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active AI Model",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "On-Device Private Model",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Processing Cores",
                                    fontSize = 13.sp, // Labels
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(2, 4, 6, 8).forEach { threadCount ->
                                        val isSelected = llamaThreads == threadCount
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.updateLlamaThreads(threadCount) }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${threadCount} Cores",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 13.sp, // Labels
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.speakText("Llama neural context graph compiled successfully with $llamaThreads active cores.")
                                    android.widget.Toast.makeText(context, "Llama Graph Compiled Successfully", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .height(48.dp)
                                    .testTag("llama_compile_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Test Offline AI Engine",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 16.sp, // Body
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // CARD 4: Emotion & Tone Engine Toggle
            val isEmotionEnabled by viewModel.isEmotionDetectionEnabled.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Emotion & Tone Adaptive AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Detect user tone and mood to dynamically adjust AI voice responses.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 18.sp
                        )
                    }

                    Switch(
                        checked = isEmotionEnabled,
                        onCheckedChange = { viewModel.toggleEmotionDetection(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("emotion_detection_switch")
                    )
                }
            }

            // CARD 5: Temperature Control
            val tempMode by viewModel.temperatureMode.collectAsState()
            val customTempText by viewModel.customTemperatureText.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "AI Creativity Level",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adjust how creative or precise AIRA's answers are. Lower values are more factual, higher values are more creative.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 18.sp
                        )
                    }

                    val options = listOf("Low (0.3)", "Medium (0.6)", "High (0.9)", "Custom")

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setTemperatureMode(option) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (tempMode == option),
                                    onClick = { viewModel.setTemperatureMode(option) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("temp_radio_${option.replace(" ", "_").replace("(", "").replace(")", "").replace(".", "")}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (tempMode == "Custom") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Custom Temperature (0.0 to 1.0)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = customTempText,
                                onValueChange = { newValue ->
                                    viewModel.setCustomTemperatureText(newValue)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_temperature_input"),
                                placeholder = {
                                    Text(
                                        text = "Enter value e.g. 0.75",
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemorySettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val memories by viewModel.memories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var addFactText by remember { mutableStateOf("") }
    var addCategory by remember { mutableStateOf("Personal") }
    var addIsImportant by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<com.example.data.Memory?>(null) }
    var editFactText by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Personal") }
    var editIsImportant by remember { mutableStateOf(false) }

    LaunchedEffect(editingMemory) {
        editingMemory?.let {
            editFactText = it.factText
            editCategory = it.category
            editIsImportant = it.isImportant
        }
    }

    val categories = listOf("All", "Personal", "Work", "Tasks", "Reminders", "Preferences")

    val filteredMemories = remember(memories, selectedCategory, searchQuery) {
        memories.filter { mem ->
            val matchesCat = if (selectedCategory == "All") true else mem.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = if (searchQuery.isBlank()) true else mem.factText.lowercase().contains(searchQuery.lowercase().trim())
            matchesCat && matchesQuery
        }.sortedWith(compareByDescending<com.example.data.Memory> { it.isImportant }.thenByDescending { it.createdAt })
    }

    // Add Memory Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.add_memory_title),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextField(
                        value = addFactText,
                        onValueChange = { addFactText = it },
                        modifier = Modifier.fillMaxWidth().testTag("add_memory_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.memory_fact_label), fontSize = 16.sp, fontFamily = FontFamily.SansSerif) }
                    )

                    Text(stringResource(R.string.category_label), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Personal", "Work", "Tasks", "Reminders", "Preferences").size) { idx ->
                            val cat = listOf("Personal", "Work", "Tasks", "Reminders", "Preferences")[idx]
                            FilterChip(
                                selected = addCategory == cat,
                                onClick = { addCategory = cat },
                                label = { Text(cat, fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.mark_important), fontSize = 14.sp, fontFamily = FontFamily.SansSerif, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = addIsImportant,
                            onCheckedChange = { addIsImportant = it },
                            modifier = Modifier.testTag("add_memory_important_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addFactText.isNotBlank()) {
                            viewModel.addMemoryManual(addFactText, addCategory, addIsImportant)
                            addFactText = ""
                            addIsImportant = false
                            showAddDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Memory added successfully ✅")
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_memory_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save), fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    modifier = Modifier.testTag("cancel_add_memory_btn")
                ) {
                    Text(stringResource(R.string.cancel), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Edit Memory Dialog
    val currentMemory = editingMemory
    if (showEditDialog && currentMemory != null) {
        val memoryItem = currentMemory
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.edit_memory_title),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextField(
                        value = editFactText,
                        onValueChange = { editFactText = it },
                        modifier = Modifier.fillMaxWidth().testTag("edit_memory_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.memory_fact_label), fontSize = 16.sp, fontFamily = FontFamily.SansSerif) }
                    )

                    Text(stringResource(R.string.category_label), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Personal", "Work", "Tasks", "Reminders", "Preferences").size) { idx ->
                            val cat = listOf("Personal", "Work", "Tasks", "Reminders", "Preferences")[idx]
                            FilterChip(
                                selected = editCategory == cat,
                                onClick = { editCategory = cat },
                                label = { Text(cat, fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.mark_important), fontSize = 14.sp, fontFamily = FontFamily.SansSerif, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = editIsImportant,
                            onCheckedChange = { editIsImportant = it },
                            modifier = Modifier.testTag("edit_memory_important_switch")
                        )
                    }

                    Text(
                        text = "Source Layer: ${memoryItem.source.uppercase()}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editFactText.isNotBlank()) {
                            viewModel.updateMemory(
                                id = memoryItem.id,
                                factText = editFactText.trim(),
                                source = memoryItem.source,
                                createdAt = memoryItem.createdAt,
                                category = editCategory,
                                isImportant = editIsImportant
                            )
                            showEditDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Memory updated")
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_edit_memory_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save), fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.deleteMemory(memoryItem.id)
                            showEditDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Deleted")
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("dialog_delete_memory_btn")
                    ) {
                        Text(stringResource(R.string.delete), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = { showEditDialog = false },
                        modifier = Modifier.testTag("dialog_cancel_memory_btn")
                    ) {
                        Text(stringResource(R.string.cancel), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.memory_settings_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("memory_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_memory_fab_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Memory",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup/Restore Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.memory_engine_header),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = stringResource(R.string.memory_engine_desc),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 17.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val msg = viewModel.exportMemoriesToDownloads(context)
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("export_memories_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.export_backup),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val msg = viewModel.importMemoriesFromDownloads(context)
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("import_memories_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.import_backup),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("search_memories_input"),
                placeholder = { Text(stringResource(R.string.search_memories), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Category Chips Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories.size) { index ->
                    val cat = categories[index]
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("filter_chip_$cat")
                    )
                }
            }

            // Memories List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECALLED FACTS (${filteredMemories.size})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold
                )
                if (memories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.clear_all),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { viewModel.clearMemories() }
                    )
                }
            }

            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_memories_placeholder),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMemories.size) { index ->
                        val item = filteredMemories[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        editingMemory = item
                                        showEditDialog = true
                                    },
                                    onLongClick = {
                                        editingMemory = item
                                        showEditDialog = true
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isImportant) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Category Badge
                                        Surface(
                                            color = when (item.category) {
                                                "Work" -> MaterialTheme.colorScheme.secondaryContainer
                                                "Tasks" -> MaterialTheme.colorScheme.tertiaryContainer
                                                "Reminders" -> MaterialTheme.colorScheme.warning.copy(alpha = 0.2f)
                                                "Preferences" -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.category.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        // Source Badge
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.source.uppercase(),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = item.factText,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = if (item.isImportant) FontWeight.SemiBold else FontWeight.Normal
                                    )

                                    Text(
                                        text = android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", item.createdAt).toString(),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Important Star Toggle
                                    IconButton(onClick = {
                                        viewModel.toggleMemoryImportant(item)
                                        coroutineScope.launch {
                                            val stateText = if (!item.isImportant) "Marked as important ⭐" else "Unmarked important"
                                            snackbarHostState.showSnackbar(stateText)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Toggle Important",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(onClick = {
                                        viewModel.deleteMemory(item.id)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Deleted")
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Memory",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val reduceAnimations by viewModel.reduceAnimations.collectAsState()
    val announceStatusChanges by viewModel.announceStatusChanges.collectAsState()
    val highContrastText by viewModel.highContrastText.collectAsState()

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Accessibility & VoiceOver",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("accessibility_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Universal Access & Screen Readers",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Aira is fully optimized for VoiceOver and TalkBack screen readers, featuring high-contrast typography, live status announcements, and touch target enforcement.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 20.sp
                    )
                }
            }

            // Toggle 1: Reduce Animations
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Reduce Animations & Motion",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Disables ambient pulsing canvas animations, rotation loops, and dynamic orb scaling for reduced sensory motion.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Switch(
                        checked = reduceAnimations,
                        onCheckedChange = { viewModel.setReduceAnimations(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("reduce_animations_switch")
                    )
                }
            }

            // Toggle 2: TalkBack Live Status Announcements
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Screen Reader Live Status",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Announces AI status updates ('Listening...', 'Aira Thinking...', 'Offline Mode Active') automatically to screen readers.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Switch(
                        checked = announceStatusChanges,
                        onCheckedChange = { viewModel.setAnnounceStatusChanges(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("announce_status_switch")
                    )
                }
            }

            // Toggle 3: High Contrast Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "High Contrast Mode",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enhances border stroke opacity and text contrast across controls for maximum legibility.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Switch(
                        checked = highContrastText,
                        onCheckedChange = { viewModel.setHighContrastText(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("high_contrast_switch")
                    )
                }
            }

            // Replay Onboarding Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Onboarding Guide",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Replay the step-by-step introduction guide to review wake word, AI modes, themes, and phone controls.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                    Button(
                        onClick = { viewModel.resetOnboarding() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("replay_onboarding_btn")
                    ) {
                        Text("Replay", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuSettingsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val isShizukuRunning by viewModel.isShizukuRunning.collectAsState()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()
    val context = LocalContext.current
    var testResultText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshShizukuStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Shizuku ADB Setup",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("shizuku_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shizuku_status_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isShizukuRunning && isShizukuGranted) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    1.dp,
                    if (isShizukuRunning && isShizukuGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    if (isShizukuRunning && isShizukuGranted) colorResource(id = R.color.aira_success_light)
                                    else colorResource(id = R.color.aira_warning_light),
                                    CircleShape
                                )
                        )
                        Text(
                            text = when {
                                isShizukuRunning && isShizukuGranted -> "Shizuku Connected & Authorized"
                                isShizukuRunning -> "Shizuku Running (Permission Required)"
                                else -> "Shizuku Service Not Running"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            isShizukuRunning && isShizukuGranted -> "AIRA can execute direct privileged system shell commands without requiring accessibility overlays."
                            isShizukuRunning -> "Shizuku service is running in background. Click below to grant ADB permission to AIRA."
                            else -> "Shizuku is not running. AIRA will automatically fall back to AccessibilityService and system dialogs seamlessly."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.refreshShizukuStatus() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.testTag("refresh_shizuku_btn")
                        ) {
                            Text("Refresh Status", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (isShizukuRunning && !isShizukuGranted) {
                            Button(
                                onClick = { viewModel.requestShizukuPermission() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("request_shizuku_perm_btn")
                            ) {
                                Text("Request Permission", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Quick Test Controls Card
            if (isShizukuRunning && isShizukuGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("shizuku_test_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Shizuku Quick Action Test",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    testResultText = com.example.utils.ShizukuManager.toggleWiFi(true)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Wi-Fi ON", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    testResultText = com.example.utils.ShizukuManager.toggleBluetooth(true)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("BT ON", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    testResultText = com.example.utils.ShizukuManager.setBrightness(80)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Bright 80%", fontSize = 11.sp)
                            }
                        }

                        testResultText?.let { res ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = res,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Setup Guide Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shizuku_guide_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Shizuku Step-by-Step Setup Guide",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Follow these instructions when you are ready to enable ADB level system actions:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val steps = listOf(
                        "1. Download Shizuku" to "Install Shizuku from Google Play Store or GitHub releases.",
                        "2. Enable Developer Options" to "Go to Phone Settings > About Phone > Tap 'Build Number' 7 times until Developer Mode is unlocked.",
                        "3. Turn On Wireless Debugging" to "Go to System > Developer Options > Enable 'Wireless Debugging'.",
                        "4. Pair with Shizuku Code" to "Open Shizuku app > Select 'Pairing' > Enter the Wireless Debugging pairing code.",
                        "5. Start Service & Authorize AIRA" to "Start Shizuku service, then return to AIRA settings and tap 'Request Permission'."
                    )

                    steps.forEachIndexed { idx, (title, desc) ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Fallback Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Notice",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Shizuku is completely optional. If not installed or enabled, AIRA operates normally using AccessibilityService and System Settings intents with zero friction.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

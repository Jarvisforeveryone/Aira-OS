package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private var activeViewModel: AiraViewModel? = null

    // Main launcher to handle startup permissions for voice, camera, and calling
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Handle runtime results gracefully inside assistant logic
        results.forEach { (permission, isGranted) ->
            android.util.Log.d("AiraMainActivity", "Permission: $permission, Granted: $isGranted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.utils.MemoryManager.setupCrashGuard(this)
        enableEdgeToEdge()

        runCatching {
            if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
                android.util.Log.i("AiraMainActivity", "SpeechRecognizer not available natively. Offline engine enabled.")
            }
        }.onFailure { e ->
            android.util.Log.w("AiraMainActivity", "Speech recognizer availability check bypassed", e)
        }

        // Request core permissions on startup including notifications on Android 13+ (API 33)
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS
        ).apply {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                add("android.permission.POST_NOTIFICATIONS")
            }
        }.toTypedArray()

        permissionLauncher.launch(permissions)

        // Initialize notification channels
        com.example.service.AiraNotificationManager.initNotificationChannels(this)

        setContent {
            val viewModel: AiraViewModel = viewModel()
            activeViewModel = viewModel
            
            val themeIndex by viewModel.themeIndex.collectAsState()
            val appTheme by viewModel.appTheme.collectAsState()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()

            AiraTheme(themeIndex = themeIndex, appTheme = appTheme) {
                com.example.ui.components.ModelDownloadPopup()

                if (!hasCompletedOnboarding) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinish = { viewModel.setOnboardingCompleted(true) }
                    )
                } else {
                    var selectedTab by remember { mutableIntStateOf(0) }
                    val snackbarHostState = remember { SnackbarHostState() }
                    val globalError by AiraViewModel.globalError.collectAsState()

                    LaunchedEffect(globalError) {
                        globalError?.let { message ->
                            snackbarHostState.showSnackbar(message)
                            AiraViewModel.clearGlobalError()
                        }
                    }

                    val showTtsDialog by viewModel.showTtsDataDialog.collectAsState()
                    val missingTtsLocale by viewModel.missingTtsLanguageLocale.collectAsState()

                    if (showTtsDialog) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissTtsDataDialog() },
                            title = {
                                Text(
                                    text = "Google TTS Voice Data Missing",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.SansSerif
                                )
                            },
                            text = {
                                Text(
                                    text = "Voice data for '$missingTtsLocale' is missing or incomplete on your device. Would you like to install Google TTS voice data now?",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = { viewModel.openInstallTtsDataSettings() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Install Voice Data")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.dismissTtsDataDialog() }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp)
                        )
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            Surface(
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                tonalElevation = 4.dp,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                NavigationBar(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 80.dp)
                                        .testTag("bottom_nav_bar"),
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    NavigationBarItem(
                                        selected = selectedTab == NavRoutes.TAB_ASSISTANT,
                                        onClick = { selectedTab = NavRoutes.TAB_ASSISTANT },
                                        alwaysShowLabel = true,
                                        icon = { 
                                            Icon(
                                                imageVector = if (selectedTab == NavRoutes.TAB_ASSISTANT) Icons.Filled.Home else Icons.Outlined.Home, 
                                                contentDescription = "Assistant Hub", 
                                                modifier = Modifier.size(Dimensions.IconSizeLarge)
                                            ) 
                                        },
                                        label = {
                                            Text(
                                                text = "Home",
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = if (selectedTab == NavRoutes.TAB_ASSISTANT) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = IconColors.TabActive,
                                            selectedTextColor = IconColors.TabActive,
                                            unselectedIconColor = IconColors.TabInactive,
                                            unselectedTextColor = IconColors.TabInactive,
                                            indicatorColor = IconColors.TabActive.copy(alpha = Opacities.ActivePillBg)
                                        ),
                                        modifier = Modifier.testTag("nav_assistant_tab")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == NavRoutes.TAB_COMMANDS,
                                        onClick = { selectedTab = NavRoutes.TAB_COMMANDS },
                                        alwaysShowLabel = true,
                                        icon = { 
                                            Icon(
                                                imageVector = if (selectedTab == NavRoutes.TAB_COMMANDS) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome, 
                                                contentDescription = "Command Deck", 
                                                modifier = Modifier.size(Dimensions.IconSizeLarge)
                                            ) 
                                        },
                                        label = {
                                            Text(
                                                text = "Automation",
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = if (selectedTab == NavRoutes.TAB_COMMANDS) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = IconColors.TabActive,
                                            selectedTextColor = IconColors.TabActive,
                                            unselectedIconColor = IconColors.TabInactive,
                                            unselectedTextColor = IconColors.TabInactive,
                                            indicatorColor = IconColors.TabActive.copy(alpha = Opacities.ActivePillBg)
                                        ),
                                        modifier = Modifier.testTag("nav_commands_tab")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == NavRoutes.TAB_FEEDS,
                                        onClick = { selectedTab = NavRoutes.TAB_FEEDS },
                                        alwaysShowLabel = true,
                                        icon = { 
                                            Icon(
                                                imageVector = if (selectedTab == NavRoutes.TAB_FEEDS) Icons.AutoMirrored.Filled.Article else Icons.AutoMirrored.Outlined.Article, 
                                                contentDescription = "Climate News Feed", 
                                                modifier = Modifier.size(Dimensions.IconSizeLarge)
                                            ) 
                                        },
                                        label = {
                                            Text(
                                                text = "Feeds",
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = if (selectedTab == NavRoutes.TAB_FEEDS) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = IconColors.TabActive,
                                            selectedTextColor = IconColors.TabActive,
                                            unselectedIconColor = IconColors.TabInactive,
                                            unselectedTextColor = IconColors.TabInactive,
                                            indicatorColor = IconColors.TabActive.copy(alpha = Opacities.ActivePillBg)
                                        ),
                                        modifier = Modifier.testTag("nav_feeds_tab")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == NavRoutes.TAB_CONFIG,
                                        onClick = { selectedTab = NavRoutes.TAB_CONFIG },
                                        alwaysShowLabel = true,
                                        icon = { 
                                            Icon(
                                                imageVector = if (selectedTab == NavRoutes.TAB_CONFIG) Icons.Filled.Settings else Icons.Outlined.Settings, 
                                                contentDescription = "Module Configurations", 
                                                modifier = Modifier.size(Dimensions.IconSizeLarge)
                                            ) 
                                        },
                                        label = {
                                            Text(
                                                text = "Settings",
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = if (selectedTab == NavRoutes.TAB_CONFIG) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = IconColors.TabActive,
                                            selectedTextColor = IconColors.TabActive,
                                            unselectedIconColor = IconColors.TabInactive,
                                            unselectedTextColor = IconColors.TabInactive,
                                            indicatorColor = IconColors.TabActive.copy(alpha = Opacities.ActivePillBg)
                                        ),
                                        modifier = Modifier.testTag("nav_config_tab")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    fadeIn().togetherWith(fadeOut())
                                },
                                label = "MainTabTransition"
                            ) { tab ->
                                when (tab) {
                                    0 -> HomeScreen(viewModel = viewModel)
                                    1 -> SystemControlScreen(viewModel = viewModel)
                                    2 -> ExtrasScreen(viewModel = viewModel)
                                    3 -> SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        activeViewModel?.onAppBackgrounded()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        activeViewModel?.onAppTrimMemory(level)
    }
}

// TAB LABELS UPDATED - PLAY STORE READY

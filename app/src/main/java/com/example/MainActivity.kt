package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.AiraTheme

class MainActivity : ComponentActivity() {

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
        enableEdgeToEdge()

        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("OnlineAccuracy")
                .setMessage("Download STT for better accuracy")
                .setPositiveButton("Allow") { dialog, _ ->
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.google.android.googlequicksearchbox"))
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox"))
                            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("DenyAccess") { dialog, _ ->
                    android.widget.Toast.makeText(this, "Offline Mode Active", android.widget.Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
                .show()
        }

        // Request core permissions on startup including notifications on Android 13+ (API 33)
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA
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
            
            val themeIndex by viewModel.themeIndex.collectAsState()
            val customColorHex by viewModel.customColorHex.collectAsState()
            val appTheme by viewModel.appTheme.collectAsState()

            AiraTheme(themeIndex = themeIndex, customColorHex = customColorHex, appTheme = appTheme) {
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
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), // Radius.Navigation: 24dp
                            tonalElevation = 4.dp, // Soft elevation
                            color = MaterialTheme.colorScheme.surface, // Premium dark surface matching status card
                            modifier = Modifier
                                .height(72.dp) // Height: 72dp
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("bottom_nav_bar"),
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Assistant Hub", modifier = Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            text = "Home",
                                            fontFamily = FontFamily.SansSerif, // Never use monospace
                                            fontWeight = if (selectedTab == 0) FontWeight.Medium else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) // Selected tab uses subtle filled background, no glow
                                    ),
                                    modifier = Modifier.testTag("nav_assistant_tab")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "Command Deck", modifier = Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            text = "Automation",
                                            fontFamily = FontFamily.SansSerif, // Never use monospace
                                            fontWeight = if (selectedTab == 1) FontWeight.Medium else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ),
                                    modifier = Modifier.testTag("nav_commands_tab")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Outlined.Article, contentDescription = "Climate News Feed", modifier = Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            text = "Feeds",
                                            fontFamily = FontFamily.SansSerif, // Never use monospace
                                            fontWeight = if (selectedTab == 2) FontWeight.Medium else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ),
                                    modifier = Modifier.testTag("nav_feeds_tab")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Module Configurations", modifier = Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            text = "Settings",
                                            fontFamily = FontFamily.SansSerif, // Never use monospace
                                            fontWeight = if (selectedTab == 3) FontWeight.Medium else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
                        when (selectedTab) {
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

// TAB LABELS UPDATED - PLAY STORE READY

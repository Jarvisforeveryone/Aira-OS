package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.colorResource
import com.example.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    viewModel: AiraViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5
    val context = LocalContext.current

    // Observe ViewModel states for live interaction
    val wakeWord by viewModel.wakeWord.collectAsState()
    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()
    val themeIndex by viewModel.themeIndex.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("onboarding_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step ${currentStep + 1} of $totalSteps",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        color = primaryColor
                    )

                    if (currentStep < totalSteps - 1) {
                        TextButton(
                            onClick = { onFinish() },
                            modifier = Modifier.testTag("onboarding_skip_btn")
                        ) {
                            Text(
                                text = "Skip",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / totalSteps },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = primaryColor,
                    trackColor = primaryColor.copy(alpha = 0.15f)
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                tonalElevation = 8.dp,
                color = surfaceColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("onboarding_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(90.dp))
                    }

                    // Step Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(totalSteps) { index ->
                            val isSelected = index == currentStep
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (isSelected) 24.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) primaryColor else onSurfaceVariant.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }

                    // Next / Finish Button
                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                currentStep++
                            } else {
                                onFinish()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.testTag("onboarding_next_btn")
                    ) {
                        Text(
                            text = if (currentStep == totalSteps - 1) "Get Started" else "Next",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep == totalSteps - 1) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "OnboardingStepTransition"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        0 -> OnboardingWelcomeStep(viewModel)
                        1 -> OnboardingWakeWordStep(viewModel, wakeWord)
                        2 -> OnboardingBrainModeStep(viewModel, isOfflineBrain)
                        3 -> OnboardingThemeStep(viewModel, themeIndex, appTheme)
                        4 -> OnboardingPermissionsStep(viewModel, context)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ==================== STEP 0: WELCOME & CORE FEATURES ====================
@Composable
private fun OnboardingWelcomeStep(viewModel: AiraViewModel) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Custom Visual Canvas Vector Illustration
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    center = Offset(250f, 250f),
                    radius = 500f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            // Draw glowing concentric energy rings
            drawCircle(color = primaryColor.copy(alpha = 0.1f), radius = 130f, center = center)
            drawCircle(color = primaryColor.copy(alpha = 0.2f), radius = 90f, center = center)
            drawCircle(color = primaryColor.copy(alpha = 0.35f), radius = 55f, center = center)

            // Draw sound waveform lines
            val path = Path()
            val startY = center.y
            path.moveTo(40f, startY)
            for (x in 40..size.width.toInt() - 40 step 20) {
                val y = startY + kotlin.math.sin(x * 0.05f) * 18f
                path.lineTo(x.toFloat(), y)
            }
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }

        Surface(
            shape = CircleShape,
            color = primaryColor,
            modifier = Modifier.size(68.dp),
            tonalElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Meet Aira Assistant",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Your intelligent, privacy-focused voice companion for hands-free chat & complete phone automation.",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Core Capabilities",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = primaryColor,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Core Feature Cards
    FeatureHighlightCard(
        icon = Icons.Default.Mic,
        title = "Conversational Voice Chat",
        description = "Natural speech synthesis & continuous voice listening with custom Piper TTS and Vosk engine.",
        tag = "voice_chat_card",
        onClick = { viewModel.speakText("Conversational Voice Chat provides natural offline and online speech interaction.") }
    )

    Spacer(modifier = Modifier.height(10.dp))

    FeatureHighlightCard(
        icon = Icons.Default.Smartphone,
        title = "Direct Device Automation",
        description = "Control Wi-Fi, Bluetooth, Flashlight, Volume, Camera launch, and Phone Calls using plain voice commands.",
        tag = "phone_control_card",
        onClick = { viewModel.speakText("Phone control lets you toggle Wi-Fi, Bluetooth, flashlight, volume, and camera hands-free.") }
    )

    Spacer(modifier = Modifier.height(10.dp))

    FeatureHighlightCard(
        icon = Icons.Default.Security,
        title = "Accessibility & Policy Admin",
        description = "Hands-free screen locking, instant screenshots, quick settings, and screen element clicking.",
        tag = "accessibility_card",
        onClick = { viewModel.speakText("Accessibility and Device Policy Admin unlock hands-free system gestures and lock screen features.") }
    )
}

// ==================== STEP 1: WAKE WORD CUSTOMIZATION ====================
@Composable
private fun OnboardingWakeWordStep(
    viewModel: AiraViewModel,
    currentWakeWord: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var customInput by remember { mutableStateOf(currentWakeWord) }

    // Canvas Illustration for Wake Word
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.2f), MaterialTheme.colorScheme.surfaceVariant)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            for (i in 1..3) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.12f * (4 - i)),
                    radius = 35f * i,
                    center = center
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Text(
                text = "\"${currentWakeWord}\"",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Customize Wake Word",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Set the custom phrase used to wake Aira hands-free. You can change this anytime in Settings.",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Input Field
    OutlinedTextField(
        value = customInput,
        onValueChange = {
            customInput = it
            viewModel.updateWakeWord(it)
        },
        label = { Text("Active Wake Word") },
        leadingIcon = { Icon(Icons.Default.Hearing, contentDescription = null, tint = primaryColor) },
        trailingIcon = {
            IconButton(onClick = { viewModel.speakText("Wake word set to $customInput") }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Test Voice", tint = primaryColor)
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_wakeword_input")
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Quick Presets",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    val presets = listOf("Hey Aira", "OK Aira", "Aira Assistant", "Jarvis", "Computer")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(presets) { _, preset ->
            val isSelected = currentWakeWord.equals(preset, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = {
                    customInput = preset
                    viewModel.updateWakeWord(preset)
                    viewModel.speakText("Wake word updated to $preset")
                },
                label = { Text(preset) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// ==================== STEP 2: ONLINE & OFFLINE AI MODES ====================
@Composable
private fun OnboardingBrainModeStep(
    viewModel: AiraViewModel,
    isOfflineBrain: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Canvas Vector Illustration for AI Modes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        if (isOfflineBrain) colorResource(R.color.aira_success_light).copy(alpha = 0.2f) else primaryColor.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cloud AI Node
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = if (!isOfflineBrain) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    "Cloud Gemini",
                    fontSize = 12.sp,
                    fontWeight = if (!isOfflineBrain) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isOfflineBrain) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text("VS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)

            // Local Offline AI Node
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = if (isOfflineBrain) colorResource(R.color.aira_success_light) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    "Local LLaMA",
                    fontSize = 12.sp,
                    fontWeight = if (isOfflineBrain) FontWeight.Bold else FontWeight.Normal,
                    color = if (isOfflineBrain) colorResource(R.color.aira_success_light) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Dual AI Brain Modes",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Switch dynamically between Cloud AI for deep reasoning and 100% Private Local AI for internet-free speed.",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Selectable Option 1: Online Gemini API
    SelectableBrainCard(
        title = "Online Gemini AI Mode",
        subtitle = "Uses Gemini API for maximum knowledge, news synthesis, complex reasoning, and smart chat.",
        icon = Icons.Default.AutoAwesome,
        accentColor = primaryColor,
        isSelected = !isOfflineBrain,
        tag = "onboarding_select_online_brain",
        onClick = { viewModel.toggleOfflineBrain(false) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Selectable Option 2: Offline Local LLaMA
    SelectableBrainCard(
        title = "100% Offline Local AI Mode",
        subtitle = "Runs LLaMA 3.2 local model directly on your device CPU/NPU. Complete privacy with zero data leaving phone.",
        icon = Icons.Default.PhonelinkRing,
        accentColor = colorResource(R.color.aira_success_light),
        isSelected = isOfflineBrain,
        tag = "onboarding_select_offline_brain",
        onClick = { viewModel.toggleOfflineBrain(true) }
    )
}

// ==================== STEP 3: THEMES & PERSONALIZATION ====================
@Composable
private fun OnboardingThemeStep(
    viewModel: AiraViewModel,
    currentThemeIndex: Int,
    currentAppTheme: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Canvas Vector Illustration for Themes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), MaterialTheme.colorScheme.surfaceVariant)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val previewColors = listOf(Color(0xFF2563EB), Color(0xFF1D4EDB), Color(0xFF3B82F6))
            previewColors.forEachIndexed { idx, col ->
                Surface(
                    shape = CircleShape,
                    color = col,
                    modifier = Modifier
                        .size(if (idx == currentThemeIndex) 48.dp else 36.dp)
                        .border(
                            width = if (idx == currentThemeIndex) 3.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        ),
                    tonalElevation = 4.dp
                ) {}
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Theme & Appearance",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Select your preferred color theme. Changes take effect instantly across the entire application interface.",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Theme Palette Options
    val themeNames = listOf("Premium Blue", "Stripe Blue", "Aether Focus")
    val themeColors = listOf(Color(0xFF2563EB), Color(0xFF1D4EDB), Color(0xFF3B82F6))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        themeNames.forEachIndexed { index, name ->
            val isSelected = currentThemeIndex == index
            Surface(
                onClick = { viewModel.selectTheme(index) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, primaryColor) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_theme_chip_$index")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(themeColors[index])
                        )
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryColor)
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Light vs Dark Mode Switch
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (currentAppTheme == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = primaryColor
            )
            Text(
                text = if (currentAppTheme == "dark") "Dark Mode" else "Light Mode",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }

        Switch(
            checked = currentAppTheme == "dark",
            onCheckedChange = { isDark ->
                val newMode = if (isDark) "dark" else "light"
                viewModel.updateAppTheme(newMode)
            },
            modifier = Modifier.testTag("onboarding_dark_mode_switch")
        )
    }
}

// ==================== STEP 4: PERMISSIONS & SYSTEM SETUP ====================
@Composable
private fun OnboardingPermissionsStep(
    viewModel: AiraViewModel,
    context: android.content.Context
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isAccessibilityActive = viewModel.isAccessibilityServiceConnected()
    val isDeviceAdminActive = viewModel.checkDeviceAdminActive()

    val hasMicPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val hasCallPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(colorResource(R.color.aira_success_light).copy(alpha = 0.2f), primaryColor.copy(alpha = 0.2f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = colorResource(R.color.aira_success_light),
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "System Ready Checklist",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Permissions & System Setup",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Grant permissions below to enable complete hands-free voice automation.",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PermissionCheckRow(
            title = "Microphone Access",
            subtitle = "Required for voice chat & wake word detection",
            isGranted = hasMicPermission,
            tag = "perm_mic_row",
            onAction = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        PermissionCheckRow(
            title = "Phone & Call Control",
            subtitle = "Required for hands-free voice dialing",
            isGranted = hasCallPermission,
            tag = "perm_call_row",
            onAction = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        PermissionCheckRow(
            title = "Camera Access",
            subtitle = "Required for voice photo capture commands",
            isGranted = hasCameraPermission,
            tag = "perm_camera_row",
            onAction = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        PermissionCheckRow(
            title = "Accessibility Command Core",
            subtitle = "Automates system gestures & screen taps",
            isGranted = isAccessibilityActive,
            tag = "perm_accessibility_row",
            onAction = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        )

        PermissionCheckRow(
            title = "Device Policy Admin",
            subtitle = "Enables secure voice screen locking",
            isGranted = isDeviceAdminActive,
            tag = "perm_device_admin_row",
            onAction = {
                val intent = viewModel.getDeviceAdminActivationIntent()
                context.startActivity(intent)
            }
        )
    }
}

// Helper Composable for Feature Highlight Cards
@Composable
private fun FeatureHighlightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// Helper Composable for AI Mode Cards
@Composable
private fun SelectableBrainCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, accentColor) else null,
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) accentColor else accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else accentColor
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
            )
        }
    }
}

// Helper Composable for Permission Check Rows
@Composable
private fun PermissionCheckRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    tag: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) colorResource(R.color.aira_success_light) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isGranted) "Enabled" else "Configure",
                    fontSize = 12.sp,
                    color = if (isGranted) colorResource(R.color.aira_success_light) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

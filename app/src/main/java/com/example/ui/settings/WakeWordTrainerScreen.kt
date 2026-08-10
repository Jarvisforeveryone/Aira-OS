package com.example.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.TrainedWakeWord
import com.example.ui.AiraViewModel
import com.example.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WakeWordTrainerScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Permission state
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Microphone permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required to train wake words.", Toast.LENGTH_LONG).show()
        }
    }

    // ViewModel States
    val currentWakeWord by viewModel.wakeWord.collectAsState()
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

    var customPhraseInput by remember(trainingWakeWordText, currentWakeWord) {
        mutableStateOf(if (trainingWakeWordText.isNotBlank()) trainingWakeWordText else currentWakeWord)
    }

    val presetPhrases = remember {
        listOf("Hey AIRA", "Jarvis", "Computer", "Hey Assistant", "Aira Wake")
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Wake Word Trainer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Record voice samples to improve detection accuracy",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("wake_word_trainer_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
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
                .verticalScroll(scrollState)
                .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.GapMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapLarge)
        ) {

            // 1. MICROPHONE PERMISSION HEADER CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mic_permission_status_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasMicPermission)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                border = BorderStroke(
                    1.dp,
                    if (hasMicPermission)
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.GapLarge),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasMicPermission)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasMicPermission) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Microphone Status",
                            tint = if (hasMicPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Dimens.IconStandard)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hasMicPermission) "Microphone Ready" else "Microphone Access Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(Dimens.GapSmall))
                            Surface(
                                color = if (hasMicPermission) colorResource(id = R.color.aira_success_light).copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                            ) {
                                    Text(
                                        text = if (hasMicPermission) "Granted" else "Action Needed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasMicPermission) colorResource(id = R.color.aira_success_light) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .padding(horizontal = Dimens.GapTiny, vertical = 2.dp)
                                            .testTag(if (hasMicPermission) "mic_permission_granted_badge" else "mic_permission_needed_badge")
                                    )
                            }
                        }
                        Text(
                            text = if (hasMicPermission)
                                "Microphone permission is active for multi-sample voice recording and wake word calibration."
                            else
                                "AIRA requires microphone access to record voice samples and train your wake word model.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    if (!hasMicPermission) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = Dimens.GapMedium, vertical = Dimens.GapTiny),
                            modifier = Modifier.testTag("grant_mic_permission_btn")
                        ) {
                            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. TARGET WAKE WORD SELECTION CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wake_word_selection_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.GapLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconMedium)
                            )
                            Text(
                                text = "Target Wake Word Phrase",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                        ) {
                            Text(
                                text = "Active: $currentWakeWord",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customPhraseInput,
                        onValueChange = {
                            customPhraseInput = it
                            if (isTrainingWakeWord) {
                                viewModel.startWakeWordTraining(it)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wake_word_target_input"),
                        label = { Text("Custom Trigger Phrase") },
                        placeholder = { Text("e.g. Hey AIRA, Jarvis, Computer") },
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Quick Select Preset Chips
                    Text(
                        text = "Quick Presets:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                        verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                    ) {
                        presetPhrases.forEach { preset ->
                            val isSelected = customPhraseInput.equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    customPhraseInput = preset
                                    viewModel.updateWakeWord(preset)
                                    if (isTrainingWakeWord) {
                                        viewModel.startWakeWordTraining(preset)
                                    }
                                },
                                label = { Text(preset, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("preset_chip_${preset.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // 3. MULTI-SAMPLE VOICE RECORDING WIZARD STUDIO
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wake_word_recording_studio_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.GapLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconStandard)
                            )
                            Text(
                                text = "Voice Sample Calibration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isTrainingWakeWord) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                            ) {
                                Text(
                                    text = "Sample $trainingCurrentStep / 3",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Record your voice 3 times speaking '$customPhraseInput' clearly in your natural voice to train the local offline detector.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    // Step Badges (1, 2, 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                    ) {
                        for (i in 1..3) {
                            val isDone = trainingAttempts.size >= i
                            val isActive = trainingCurrentStep == i && isTrainingWakeWord && !isDone

                            val badgeBg = when {
                                isDone -> colorResource(id = R.color.aira_success_light).copy(alpha = 0.15f)
                                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val badgeBorder = when {
                                isDone -> colorResource(id = R.color.aira_success_light)
                                isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            val badgeText = when {
                                isDone -> colorResource(id = R.color.aira_success_light)
                                isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                                    .background(badgeBg)
                                    .border(1.dp, badgeBorder, RoundedCornerShape(Dimens.CornerRadiusSmall))
                                    .padding(vertical = Dimens.GapSmall),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isDone) "✓ Sample $i" else "Sample $i",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeText
                                    )
                                    if (isDone && trainingAttempts.size >= i) {
                                        Text(
                                            text = trainingAttempts[i - 1],
                                            fontSize = 10.sp,
                                            color = badgeText.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Recording Soundwave Canvas
                    if (isRecordingAttempt) {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val pulseInfinite = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by pulseInfinite.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseScale"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(Dimens.GapMedium),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.GapSmall)
                                        .scale(pulseScale)
                                        .background(MaterialTheme.colorScheme.error, CircleShape)
                                )
                                Text(
                                    text = "Listening... Speak '$customPhraseInput' now!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // Soundwave Visualizer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val midY = h / 2f
                                    val barCount = 28
                                    val barWidth = 8f
                                    val gap = 12f
                                    val totalW = barCount * barWidth + (barCount - 1) * gap
                                    val startX = (w - totalW) / 2f

                                    for (j in 0 until barCount) {
                                        val distanceFromCenter = Math.abs(j - barCount / 2f) / (barCount / 2f)
                                        val factor = 1f - distanceFromCenter * 0.7f
                                        val animOffset = Math.sin((j * 0.3 + System.currentTimeMillis() * 0.008)).toFloat() * 0.25f
                                        val barH = ((trainingLiveAmplitude * 1.8f + 0.12f) * h * factor * (1f + animOffset)).coerceIn(10f, h)

                                        val x = startX + j * (barWidth + gap)
                                        drawRoundRect(
                                            color = primaryColor.copy(alpha = 0.5f + trainingLiveAmplitude * 0.5f),
                                            topLeft = Offset(x, midY - barH / 2f),
                                            size = Size(barWidth, barH),
                                            cornerRadius = CornerRadius(4f, 4f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Status / Feedback Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                            .background(
                                if (trainingQualityScore.contains("Excellent") || trainingQualityScore.contains("Good"))
                                    colorResource(id = R.color.aira_success_light).copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(Dimens.GapMedium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                        ) {
                            Icon(
                                imageVector = if (trainingQualityScore.contains("successful") || trainingQualityScore.contains("Excellent"))
                                    Icons.Default.CheckCircle
                                else
                                    Icons.Default.Info,
                                contentDescription = null,
                                tint = if (trainingQualityScore.contains("Excellent") || trainingQualityScore.contains("Good"))
                                    colorResource(id = R.color.aira_success_light)
                                else
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconSmall)
                            )
                            Text(
                                text = trainingQualityScore,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (trainingQualityScore.contains("Excellent") || trainingQualityScore.contains("Good"))
                                    colorResource(id = R.color.aira_success_light)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Action Controls for Recording
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                    ) {
                        if (!isTrainingWakeWord) {
                            Button(
                                onClick = {
                                    if (!hasMicPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.startWakeWordTraining(customPhraseInput)
                                        viewModel.startRecordingAttempt()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_wake_word_training_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall))
                                Spacer(modifier = Modifier.width(Dimens.GapSmall))
                                Text("Start Trainer", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            if (isRecordingAttempt) {
                                Button(
                                    onClick = { viewModel.stopRecordingAttempt() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stop_recording_sample_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall))
                                    Spacer(modifier = Modifier.width(Dimens.GapTiny))
                                    Text("Stop Sample $trainingCurrentStep", fontSize = 13.sp)
                                }
                            } else if (trainingAttempts.size < 3) {
                                Button(
                                    onClick = {
                                        if (!hasMicPermission) {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            viewModel.startRecordingAttempt()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("record_sample_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall))
                                    Spacer(modifier = Modifier.width(Dimens.GapTiny))
                                    Text("Record Sample $trainingCurrentStep", fontSize = 13.sp)
                                }
                            }

                            if (trainingAttempts.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { viewModel.resetWakeWordTrainingAttempts() },
                                    modifier = Modifier.testTag("reset_training_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(Dimens.IconSmall))
                                    Spacer(modifier = Modifier.width(Dimens.GapTiny))
                                    Text("Reset", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // SAVE & ACTIVATE BUTTON
                    if (trainingAttempts.size >= 3) {
                        Button(
                            onClick = {
                                viewModel.saveAndActivateTrainedWakeWord()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_and_activate_wake_word_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.aira_success_light)
                            ),
                            shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(Dimens.GapSmall))
                            Text(
                                "Save & Activate Wake Word",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 4. REAL-TIME TESTING PAD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wake_word_testing_pad_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.GapLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (isTestingWakeWord) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimens.IconStandard)
                            )
                            Column {
                                Text(
                                    text = "Live Wake Word Verification",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Speak '$currentWakeWord' to test recognition",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isTestingWakeWord,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasMicPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    viewModel.toggleTestingWakeWord(enabled)
                                }
                            },
                            modifier = Modifier.testTag("toggle_test_wake_word_switch")
                        )
                    }

                    if (isTestingWakeWord) {
                        Surface(
                            color = if (isTestWakeWordTriggered)
                                colorResource(id = R.color.aira_success_light).copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                            border = BorderStroke(
                                1.dp,
                                if (isTestWakeWordTriggered)
                                    colorResource(id = R.color.aira_success_light)
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.GapMedium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                            ) {
                                Icon(
                                    imageVector = if (isTestWakeWordTriggered) Icons.Default.CheckCircle else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isTestWakeWordTriggered) colorResource(id = R.color.aira_success_light) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(Dimens.IconStandard)
                                )
                                Text(
                                    text = testTriggerText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isTestWakeWordTriggered) colorResource(id = R.color.aira_success_light) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 5. SAVED WAKE WORD PROFILES HISTORY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_wake_word_profiles_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.GapLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconMedium)
                            )
                            Text(
                                text = "Saved Trained Profiles",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                        ) {
                            Text(
                                text = "${trainedWakeWords.size} Profiles",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny)
                            )
                        }
                    }

                    if (trainedWakeWords.isEmpty()) {
                        Text(
                            text = "No custom wake word profiles saved yet. Use the trainer above to record samples and create your custom trigger profile.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Dimens.GapSmall)
                        )
                    } else {
                        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

                        trainedWakeWords.forEach { profile ->
                            TrainedProfileItem(
                                profile = profile,
                                dateFormat = dateFormat,
                                onActivate = { viewModel.activateTrainedWakeWord(profile.id, profile.word) },
                                onDelete = { viewModel.deleteTrainedWakeWord(profile.id, profile.word) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainedProfileItem(
    profile: TrainedWakeWord,
    dateFormat: SimpleDateFormat,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trained_profile_item_${profile.id}"),
        color = if (profile.isActive)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
        border = BorderStroke(
            1.dp,
            if (profile.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.GapMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                ) {
                    Text(
                        text = profile.word,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (profile.isActive) {
                        Surface(
                            color = colorResource(id = R.color.aira_success_light).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                        ) {
                            Text(
                                text = "Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.aira_success_light),
                                modifier = Modifier.padding(horizontal = Dimens.GapTiny, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                    ) {
                        Text(
                            text = "Quality: ${profile.quality}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Dimens.GapTiny, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Created: ${dateFormat.format(Date(profile.createdAt))}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
            ) {
                if (!profile.isActive) {
                    TextButton(
                        onClick = onActivate,
                        modifier = Modifier.testTag("activate_profile_btn_${profile.id}")
                    ) {
                        Text("Activate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_profile_btn_${profile.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Profile",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimens.IconMedium)
                    )
                }
            }
        }
    }
}

package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Refresh
import com.example.data.ResponseFeedback
import com.example.ui.theme.staggeredEntry
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
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.*
import com.example.ui.components.AiraCard
import com.example.ui.components.AiraBadge
import com.example.ui.components.SkeletonCard
import com.example.ui.components.SkeletonText
import com.example.ui.components.LoadingCard
import com.example.ui.components.LoadingInlineIndicator
import com.example.ui.components.AiraVoiceOrb
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.data.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRecordAudioRationale by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            showRecordAudioRationale = true
        }
    }
    val isListening by viewModel.isListening.collectAsState()
    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()
    val onlineMode = !isOfflineBrain
    val reduceAnimations by viewModel.reduceAnimations.collectAsState()

    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val statusText by viewModel.currentStatus.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val audioAmp by viewModel.audioAmplitude.collectAsState()
    
    val themeIndex by viewModel.themeIndex.collectAsState()

    val piperActiveVoice by viewModel.piperActiveVoice.collectAsState()
    val piperIsModelDownloaded by viewModel.piperIsModelDownloaded.collectAsState()
    val piperDownloadProgress by viewModel.piperDownloadProgress.collectAsState()
    val piperDownloadStatusMessage by viewModel.piperDownloadStatusMessage.collectAsState()
    val piperAvailableVoices = viewModel.piperAvailableVoices
    val modelReadyState by viewModel.modelReadyState.collectAsState()
    val morningBriefing by viewModel.morningBriefing.collectAsState()
    val isBriefingLoading by viewModel.isBriefingLoading.collectAsState()

    // Dynamic greeting text based on current hour
    val currentHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingText = when (currentHour) {
        in 5..11 -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        else -> stringResource(R.string.good_evening)
    }

    // Dynamic selected voice simple name
    val selectedVoiceName = when (piperActiveVoice) {
        "en_US-amy-medium", "amy" -> "Amy"
        "google-lily", "en_US-lily", "lily" -> "Lily"
        "google-zara", "en_US-zara", "zara" -> "Zara"
        "google-ella", "en_UK-ella", "en_GB-ella", "ella" -> "Ella"
        else -> "Amy"
    }
    val voiceLabel = "Voice"

    var showVoiceDropdown by remember { mutableStateOf(false) }

    var pendingNegativeFeedbackMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var pendingNegativeFeedbackQuery by remember { mutableStateOf("") }
    var showFeedbackLogsDialog by remember { mutableStateOf(false) }
    var showFullHistoryDialog by remember { mutableStateOf(false) }

    val sttState by viewModel.sttState.collectAsState()

    // UI CHANGE: Determine dynamic state-based status message
    val derivedStatus = when (sttState) {
        com.example.ui.SttState.LISTENING -> "Listening..."
        com.example.ui.SttState.PROCESSING -> "Processing..."
        com.example.ui.SttState.SPEAKING -> "Speaking..."
        com.example.ui.SttState.IDLE -> "Tap mic to speak"
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(Dimens.responsiveScreenPadding)
                .widthIn(max = 800.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Dimens.GapLarge))

            // 1. TOP ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.GapSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.AppBrand,
                    fontSize = TypographySubsystem.SizeTitleMedium,
                    fontWeight = TypographySubsystem.WeightBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(Dimens.CornerRadiusLarge))
                        .padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny)
                ) {
                    Text(
                        text = "Voice: $selectedVoiceName",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = TypographySubsystem.SizeCaption,
                        fontWeight = TypographySubsystem.WeightMedium
                    )
                }
            }

            Spacer(Modifier.height(Dimens.GapLarge))

            // 2. NEXT 2 LINES BELOW TOP ROW
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = AppStrings.DefaultGreeting,
                        fontSize = TypographySubsystem.SizeDisplay,
                        fontWeight = TypographySubsystem.WeightBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(Dimens.GapSmall))
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconStandard),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(Modifier.height(Dimens.GapTiny))
                Text(
                    text = AppStrings.DefaultSubtitle,
                    fontSize = TypographySubsystem.SizeBodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(Dimens.GapExtraLarge))

            if (isOfflineBrain) {
                Spacer(modifier = Modifier.height(Dimens.GapMedium))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                    modifier = Modifier.fillMaxWidth().testTag("offline_mode_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.GapLarge, vertical = Dimens.GapMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Dimens.GapSmall)
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
                        Spacer(modifier = Modifier.width(Dimens.GapSmall))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                        ) {
                            Text(
                                text = "Private On-Device AI • Voice Ready",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny)
                            )
                        }
                    }
                }
            }

            // Background Services & Fallback Status Indicators Bar
            val isAccessibilityActive = viewModel.isAccessibilityServiceConnected()
            val isDeviceAdminActive = viewModel.checkDeviceAdminActive()
            val selectedTtsEngine by viewModel.selectedTtsEngine.collectAsState()
            val selectedSttEngine by viewModel.selectedSttEngine.collectAsState()

            Spacer(modifier = Modifier.height(Dimens.GapMedium))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("background_status_chips_row"),
                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.GapSmall)
                                .background(colorResource(id = R.color.aira_success_light), CircleShape)
                        )
                        Text(
                            text = "Mic Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (isAccessibilityActive) colorResource(id = R.color.aira_success_light).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                ) {
                    Text(
                        text = if (isAccessibilityActive) "Accessibility On" else "Accessibility Off",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAccessibilityActive) colorResource(id = R.color.aira_success_light) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(Dimens.CornerRadiusSmall)
                ) {
                    Text(
                        text = if (isOfflineBrain) "AI: Offline Mode" else "AI: Online Mode",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.GapSmall, vertical = Dimens.GapTiny)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.GapExtraLarge)) // Section spacing

            val isProcessing = statusText.contains("Processing", ignoreCase = true) || 
                statusText.contains("Analyzing", ignoreCase = true) || 
                statusText.contains("Transitioning", ignoreCase = true) ||
                statusText.contains("Thinking", ignoreCase = true)

            // AIRA Voice Orb (180dp core, 3 concentric glow rings, gradient fill, dynamic state reactivity)
            AiraVoiceOrb(
                isListening = isListening,
                isProcessing = isProcessing,
                isSpeaking = isSpeaking,
                audioAmp = audioAmp,
                reduceAnimations = reduceAnimations,
                derivedStatus = derivedStatus,
                onOrbClick = {
                    if (isListening) {
                        viewModel.stopListening()
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.startListening()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            )

            if (showRecordAudioRationale) {
                AlertDialog(
                    onDismissRequest = { showRecordAudioRationale = false },
                    title = {
                        Text(
                            text = "Microphone Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Text(
                            text = "Aira requires access to your microphone to process voice commands and enable Speech-to-Text features. Please grant audio permission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRecordAudioRationale = false
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        ) {
                            Text("Grant Permission")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRecordAudioRationale = false }) {
                            Text("Dismiss")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.GapExtraLarge)) // Distance below orb

            // Instruction Text / State Status Text
            Spacer(modifier = Modifier.height(Dimens.GapExtraLarge))
            Text(
                text = derivedStatus,
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.GapLarge))

            // Status Card
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Dimens.CornerRadiusLarge))
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "System Status: $derivedStatus. Connection mode: ${if (onlineMode) "Online" else "Offline"}. Voice detection is active."
                    }
                    .padding(horizontal = Dimens.GapLarge, vertical = Dimens.GapMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.GapLarge, Alignment.CenterHorizontally)
            ) {
                // PART A - CONNECTION
                Row(
                    modifier = Modifier
                        .semantics {
                            role = Role.Switch
                            contentDescription = if (onlineMode) "Switch connection mode, currently Online" else "Switch connection mode, currently Offline"
                            onClick(label = "Toggle online/offline connection mode") {
                                viewModel.toggleOfflineBrain(onlineMode)
                                true
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.toggleOfflineBrain(onlineMode)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.GapSmall)
                            .background(colorResource(id = R.color.aira_success_light), CircleShape)
                    )
                    Text(
                        text = if (onlineMode) "Online" else "Offline",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))

                // PART B - VOICE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.GapSmall)
                            .background(colorResource(id = R.color.aira_success_light), CircleShape)
                    )
                    Text(
                        text = "Voice Detection Active",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.GapLarge))

            // Iron Man Morning Briefing Card
            AiraCard(
                modifier = Modifier
                    .staggeredEntry(index = 0)
                    .padding(top = Dimens.GapLarge)
                    .testTag("morning_briefing_card"),
                title = "Iron Man Morning Briefing",
                headerTrailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.playMorningBriefing() },
                            modifier = Modifier.size(Dimens.IconLarge).testTag("play_briefing_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "Play Briefing Voice",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconSmall)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.refreshWeather() },
                            modifier = Modifier.size(Dimens.IconLarge).testTag("refresh_briefing_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh Briefing",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(Dimens.IconSmall)
                            )
                        }
                    }
                }
            ) {
                if (isBriefingLoading) {
                    LoadingCard(
                        title = "Iron Man Briefing",
                        message = "AIRA compiling weather, local status & schedule agenda..."
                    )
                } else {
                    val briefingContent = morningBriefing ?: "Tap refresh to compile your Iron Man morning briefing."
                    Text(
                        text = briefingContent,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Recent Conversations Card
            AiraCard(
                modifier = Modifier
                    .staggeredEntry(index = 1)
                    .padding(top = Dimens.GapExtraLarge),
                title = "Recent Chats",
                headerTrailing = {
                    Row(
                        modifier = Modifier
                            .testTag("open_feedback_logs_btn")
                            .bounceClick(onClick = { showFeedbackLogsDialog = true }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RateReview,
                            contentDescription = "Feedback Logs",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                        Text(
                            text = "Logs",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Row(
                        modifier = Modifier
                            .testTag("view_all_conversations_btn")
                            .bounceClick(onClick = { showFullHistoryDialog = true }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
                    ) {
                        Text(
                            text = "View All",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "View All",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Dimens.IconSmall)
                        )
                    }
                }
            ) {
                    val sdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary

                    val displayItems = remember(chatHistory, primaryColor, secondaryColor) {
                        chatHistory.takeLast(4).reversed().map { msg ->
                            val isUser = msg.sender == "user"
                            val avatar = if (isUser) "U" else "A"
                            val color = if (isUser) secondaryColor else primaryColor
                            val name = if (isUser) "User" else "Aira"
                            val timeStr = sdf.format(java.util.Date(msg.timestamp))
                            val queryText = getQueryForMessage(msg, chatHistory)
                            RecentConvItem(avatar, color, name, msg.message, timeStr, msg, queryText)
                        }
                    }

                    if (displayItems.isEmpty()) {
                        Text(
                            text = "No recent conversations. Tap the voice orb to begin.",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Dimens.GapMedium)
                        )
                    } else {
                        displayItems.forEachIndexed { index, item ->
                            RecentConversationRow(
                                avatarText = item.avatarText,
                                avatarBg = item.avatarBg,
                                name = item.name,
                                message = item.message,
                                time = item.time,
                                chatMessage = item.chatMessage,
                                queryText = item.queryText,
                                viewModel = viewModel,
                                onPromptNegativeFeedback = { msg, query ->
                                    pendingNegativeFeedbackMsg = msg
                                    pendingNegativeFeedbackQuery = query
                                }
                            )
                            if (index < displayItems.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = Dimens.GapTiny)
                                )
                            }
                        }
                    }
                }
            }

        val isAmyDownloaded = piperIsModelDownloaded["en_US-amy-medium"] == true
        val amyProgress = piperDownloadProgress["en_US-amy-medium"]
        val statusMsg = piperDownloadStatusMessage

        var lastStatusMsg by remember { mutableStateOf<String?>(null) }
        var isDismissed by remember { mutableStateOf(false) }
        
        if (statusMsg != lastStatusMsg) {
            lastStatusMsg = statusMsg
            isDismissed = false
        }

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue != SwipeToDismissBoxValue.Settled) {
                    isDismissed = true
                    true
                } else false
            }
        )

        AnimatedVisibility(
            visible = statusMsg != null && !isDismissed && !isAmyDownloaded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Dimens.GapLarge)
                .padding(bottom = Dimens.GapSmall)
        ) {
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
                content = {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tts_download_overlay_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMedium)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(Dimens.GapLarge)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                        ) {
                            val isError = statusMsg?.contains("fail", ignoreCase = true) == true || statusMsg?.contains("error", ignoreCase = true) == true

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = if (isError) Icons.Outlined.Warning else Icons.Outlined.CloudDownload
                                    val iconColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "TTS Download Status",
                                        tint = iconColor,
                                        modifier = Modifier.size(Dimens.IconMedium)
                                    )
                                    Text(
                                        text = if (isError) "Voice Setup Error" else "Acoustic Model Download",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
                                ) {
                                    if (amyProgress != null && amyProgress in 0f..1f) {
                                        Text(
                                            text = "${(amyProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { isDismissed = true },
                                        modifier = Modifier.size(Dimens.IconStandard)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(Dimens.IconSmall)
                                        )
                                    }
                                }
                            }
                            
                            val cleanExplanation = if (isError) {
                                when {
                                    statusMsg?.contains("404") == true -> "Acoustic voice model files not found on server."
                                    statusMsg?.contains("network") == true || statusMsg?.contains("connection") == true -> "Network timeout while downloading voice pack."
                                    else -> "Voice synthesis model failed to initialize. System Speech API remains active."
                                }
                            } else {
                                statusMsg ?: ""
                            }

                            Text(
                                text = cleanExplanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (isError) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.piperTtsManager.startDownload() },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Retry Setup", fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                                    }
                                    Button(
                                        onClick = { isDismissed = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Text("Use System Voice", fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                                    }
                                }
                            } else if (amyProgress != null && amyProgress in 0f..1f) {
                                LinearProgressIndicator(
                                    progress = { amyProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(Dimens.GapSmall)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    pendingNegativeFeedbackMsg?.let { msg ->
        NegativeFeedbackDialog(
            msg = msg,
            queryText = pendingNegativeFeedbackQuery,
            onDismiss = { pendingNegativeFeedbackMsg = null },
            onSubmit = { comment ->
                viewModel.submitFeedback(
                    messageId = msg.id,
                    query = pendingNegativeFeedbackQuery.ifBlank { "Your message" },
                    response = msg.message,
                    isPositive = false,
                    comment = comment
                )
                pendingNegativeFeedbackMsg = null
            }
        )
    }

    if (showFeedbackLogsDialog) {
        FeedbackLogsDialog(
            viewModel = viewModel,
            onDismiss = { showFeedbackLogsDialog = false }
        )
    }

    if (showFullHistoryDialog) {
        FullConversationHistoryDialog(
            viewModel = viewModel,
            onDismiss = { showFullHistoryDialog = false },
            onPromptNegativeFeedback = { msg, q ->
                pendingNegativeFeedbackMsg = msg
                pendingNegativeFeedbackQuery = q
            },
            onOpenFeedbackLogs = {
                showFullHistoryDialog = false
                showFeedbackLogsDialog = true
            }
        )
    }
}

fun getQueryForMessage(msg: ChatMessage, allMessages: List<ChatMessage>): String {
    val idx = allMessages.indexOfFirst { it.id == msg.id }
    if (idx > 0) {
        for (i in idx - 1 downTo 0) {
            if (allMessages[i].sender == "user") {
                return allMessages[i].message
            }
        }
    }
    return "Your message"
}

@Composable
fun ResponseFeedbackButtons(
    msg: ChatMessage,
    queryText: String,
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier,
    onPromptNegativeFeedback: (ChatMessage, String) -> Unit = { _, _ -> }
) {
    val feedbackList by viewModel.feedbackList.collectAsState()
    
    val existingFeedback = remember(feedbackList, msg.id, msg.message) {
        feedbackList.find { fb -> 
            (fb.messageId != null && fb.messageId == msg.id) || 
            (fb.response == msg.message && Math.abs(fb.timestamp - msg.timestamp) < 10000)
        }
    }

    val isPositive = existingFeedback?.feedbackType == "POSITIVE"
    val isNegative = existingFeedback?.feedbackType == "NEGATIVE"

    Row(
        modifier = modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {
                viewModel.submitFeedback(
                    messageId = msg.id,
                    query = queryText.ifBlank { "Your message" },
                    response = msg.message,
                    isPositive = true
                )
            },
            modifier = Modifier
                .size(32.dp)
                .testTag("thumbs_up_button_${msg.id}")
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = "Thumbs up positive feedback",
                tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = {
                onPromptNegativeFeedback(msg, queryText)
            },
            modifier = Modifier
                .size(32.dp)
                .testTag("thumbs_down_button_${msg.id}")
        ) {
            Icon(
                imageVector = if (isNegative) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                contentDescription = "Thumbs down negative feedback",
                tint = if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }

        if (existingFeedback != null) {
            Surface(
                color = if (isPositive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isPositive) "Got it, thanks!" 
                           else if (!existingFeedback.comment.isNullOrBlank()) "Comment: \"${existingFeedback.comment}\"" 
                           else "Got it, thanks!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer 
                            else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NegativeFeedbackDialog(
    msg: ChatMessage,
    queryText: String,
    onDismiss: () -> Unit,
    onSubmit: (comment: String?) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ThumbDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "How was my response?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
            ) {
                Text(
                    text = "Help us improve Aira! What went wrong with this response? (Optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(Dimens.CornerRadiusSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Dimens.GapMedium)) {
                        Text(
                            text = "Q: ${queryText.ifBlank { "Voice request" }}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Dimens.GapMicro))
                        Text(
                            text = "A: ${msg.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("e.g. Inaccurate details, cutoff sentence...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("negative_feedback_comment_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(commentText) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("submit_negative_feedback_btn")
            ) {
                Text("Send Feedback")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onSubmit(null) },
                modifier = Modifier.testTag("skip_negative_feedback_btn")
            ) {
                Text("Skip, thanks")
            }
        }
    )
}

@Composable
fun FeedbackLogsDialog(
    viewModel: AiraViewModel,
    onDismiss: () -> Unit
) {
    val feedbackList by viewModel.feedbackList.collectAsState()
    val sdf = remember { java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Feedback Logs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${feedbackList.size} stored feedback entries",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (feedbackList.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllFeedback() },
                        modifier = Modifier.testTag("clear_all_feedback_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Clear all feedback",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            if (feedbackList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No stored feedback logs yet.\nUse 👍 or 👎 on AI responses to provide feedback.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                ) {
                    items(feedbackList, key = { it.id }) { item ->
                        val isPos = item.feedbackType == "POSITIVE"
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                            border = BorderStroke(
                                1.dp,
                                if (isPos) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(Dimens.GapMedium)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPos) Icons.Filled.ThumbUp else Icons.Filled.ThumbDown,
                                            contentDescription = null,
                                            tint = if (isPos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isPos) "Positive" else "Negative",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = sdf.format(java.util.Date(item.timestamp)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Q: ${item.query}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "A: ${item.response}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!item.comment.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Comment: \"${item.comment}\"",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FullConversationHistoryDialog(
    viewModel: AiraViewModel,
    onDismiss: () -> Unit,
    onPromptNegativeFeedback: (ChatMessage, String) -> Unit,
    onOpenFeedbackLogs: () -> Unit
) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val listState = rememberLazyListState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chat History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onOpenFeedbackLogs,
                    modifier = Modifier.testTag("dialog_feedback_logs_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Feedback,
                        contentDescription = "Feedback Logs",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Logs", fontSize = 12.sp)
                }
            }
        },
        text = {
            if (chatHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No conversation history yet.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatHistory, key = { it.id }) { msg ->
                        val queryText = getQueryForMessage(msg, chatHistory)
                        ChatLogBubble(
                            msg = msg,
                            queryText = queryText,
                            viewModel = viewModel,
                            onPromptNegativeFeedback = onPromptNegativeFeedback
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ChatLogBubble(
    msg: ChatMessage,
    queryText: String = "",
    viewModel: AiraViewModel? = null,
    onPromptNegativeFeedback: (ChatMessage, String) -> Unit = { _, _ -> }
) {
    val isUser = msg.sender == "user"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Holographic sender header label
            Text(
                text = if (isUser) "You" else "Aira" + if (msg.isOffline) " [Offline]" else "",
                fontSize = 11.sp, // Caption
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = if (isUser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Chat body Bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(
                        if (isUser) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = msg.message,
                    color = if (isUser) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp, // Caption
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // AI Feedback Buttons
            if (!isUser && viewModel != null) {
                ResponseFeedbackButtons(
                    msg = msg,
                    queryText = queryText,
                    viewModel = viewModel,
                    onPromptNegativeFeedback = onPromptNegativeFeedback
                )
            }
        }
    }
}

data class RecentConvItem(
    val avatarText: String,
    val avatarBg: Color,
    val name: String,
    val message: String,
    val time: String,
    val chatMessage: ChatMessage? = null,
    val queryText: String = ""
)

@Composable
fun RecentConversationRow(
    avatarText: String,
    avatarBg: Color,
    name: String,
    message: String,
    time: String,
    chatMessage: ChatMessage? = null,
    queryText: String = "",
    viewModel: AiraViewModel? = null,
    onPromptNegativeFeedback: (ChatMessage, String) -> Unit = { _, _ -> }
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(avatarBg.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarText,
                    color = avatarBg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Name & Message
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal
                )
            }

            // Time
            Text(
                text = time,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }

        // Action Feedback Buttons for AI message
        if (name == "Aira" && chatMessage != null && viewModel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ResponseFeedbackButtons(
                    msg = chatMessage,
                    queryText = queryText,
                    viewModel = viewModel,
                    onPromptNegativeFeedback = onPromptNegativeFeedback
                )
            }
        }
    }
}

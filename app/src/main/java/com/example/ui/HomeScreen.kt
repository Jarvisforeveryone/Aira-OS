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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val vmIsListening by viewModel.isListening.collectAsState()
    var isListening by remember { mutableStateOf(false) }
    LaunchedEffect(vmIsListening) {
        isListening = vmIsListening
    }

    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()
    val reduceAnimations by viewModel.reduceAnimations.collectAsState()
    var onlineMode by remember { mutableStateOf(!isOfflineBrain) }
    LaunchedEffect(isOfflineBrain) {
        onlineMode = !isOfflineBrain
    }

    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val statusText by viewModel.currentStatus.collectAsState()
    val audioAmp by viewModel.audioAmplitude.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    
    val themeIndex by viewModel.themeIndex.collectAsState()

    val piperActiveVoice by viewModel.piperActiveVoice.collectAsState()
    val piperIsModelDownloaded by viewModel.piperIsModelDownloaded.collectAsState()
    val piperDownloadProgress by viewModel.piperDownloadProgress.collectAsState()
    val piperDownloadStatusMessage by viewModel.piperDownloadStatusMessage.collectAsState()
    val piperAvailableVoices = viewModel.piperAvailableVoices
    val modelReadyState by viewModel.modelReadyState.collectAsState()

    val listState = rememberLazyListState()

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

    // Auto scroll chat thread on new messages
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    // UI CHANGE: Determine dynamic state-based status message
    val derivedStatus = when {
        isListening -> stringResource(R.string.listening_status)
        isSpeaking -> stringResource(R.string.speaking_status)
        statusText.contains("Processing", ignoreCase = true) || 
        statusText.contains("Analyzing", ignoreCase = true) || 
        statusText.contains("Transitioning", ignoreCase = true) ||
        statusText.contains("Thinking", ignoreCase = true) -> stringResource(R.string.thinking_status)
        else -> stringResource(R.string.ready_status)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "OrbRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    val sweepGradient = Brush.sweepGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), // Outer padding: 24dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = stringResource(R.string.aira_logo_text),
                        fontSize = 30.sp, // AIRA Logo 30sp
                        fontWeight = FontWeight.SemiBold, // SemiBold
                        fontFamily = FontFamily.SansSerif, // Never use monospace
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    when (modelReadyState) {
                        AiraViewModel.VoiceAssistantState.READY -> {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.CheckCircle,
                                contentDescription = "Voice Assistant Offline/Ready",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("voice_assistant_status_ready")
                            )
                        }
                        AiraViewModel.VoiceAssistantState.DOWNLOADING -> {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(16.dp)
                                    .testTag("voice_assistant_status_downloading")
                            )
                        }
                        AiraViewModel.VoiceAssistantState.NOT_DOWNLOADED -> {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.CloudDownload,
                                contentDescription = "Voice Assistant Needs Download",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("voice_assistant_status_not_downloaded")
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Voice: $selectedVoiceName",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (isOfflineBrain) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("offline_mode_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Text(
                                text = "Offline Mode Active",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Text(
                            text = "Llama 3.2 • Amy ONNX • Vosk STT",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp)) // Section spacing: 28dp

            // Greeting Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = greetingText,
                    fontSize = 36.sp, // Greeting 36sp
                    fontWeight = FontWeight.Bold, // Bold
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp)) // Subtitle sits exactly 6dp below
                Text(
                    text = "Ready to help",
                    fontSize = 17.sp, // Subtitle 17sp
                    fontWeight = FontWeight.Normal, // Regular
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp)) // Distance from subtitle: 32dp

            // Hero Orb
            Box(
                modifier = Modifier
                    .size(180.dp) // Orb Size: 180dp
                    .semantics {
                        contentDescription = if (isListening) "Aira Voice Assistant Orb, listening. Tap to stop." else "Aira Voice Assistant Orb, idle. Tap to start talking."
                        role = Role.Button
                        stateDescription = derivedStatus
                        onClick(label = if (isListening) "Stop voice detection" else "Start voice listening") {
                            if (isListening) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                            isListening = !isListening
                            true
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isListening) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                        isListening = !isListening
                    },
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary

                // Layer 2 Central Accent: Premium solid center (respects reduceAnimations)
                val animatedScale by animateFloatAsState(
                    targetValue = if (reduceAnimations) 1.0f else if (isListening) 1.1f + audioAmp * 0.4f else if (isSpeaking) 1.05f + audioAmp * 0.2f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                    label = "OrbScale"
                )

                // Layer 1 Outer Ring: Premium 2dp thin circle
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.2f),
                        radius = (size.minDimension - 2.dp.toPx()) / 2f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .border(1.dp, primaryColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(primaryColor, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp)) // Distance below orb: 28dp

            // Instruction Text
            Spacer(modifier = Modifier.height(20.dp)) // Top spacing: 20dp
            Text(
                text = "Tap the orb to start talking",
                fontSize = 14.sp, // Caption: 14sp
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Muted color, not too bright
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f)) // Adaptive spacing

            // Status Card
            Row(
                modifier = Modifier
                    .height(52.dp) // Height: 52dp
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)) // Soft Material surface, Corner radius: 18dp, No border
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "System Status: $derivedStatus. Connection mode: ${if (onlineMode) "Online" else "Offline"}. Voice detection is active."
                    }
                    .padding(horizontal = 18.dp), // Horizontal padding: 18dp
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally) // Internal spacing: 12dp, Items perfectly centered
            ) {
                // PART A - CONNECTION
                Row(
                    modifier = Modifier
                        .semantics {
                            role = Role.Switch
                            contentDescription = if (onlineMode) "Switch connection mode, currently Online" else "Switch connection mode, currently Offline"
                            onClick(label = "Toggle online/offline connection mode") {
                                onlineMode = !onlineMode
                                viewModel.toggleOfflineBrain(!onlineMode)
                                true
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onlineMode = !onlineMode
                            viewModel.toggleOfflineBrain(!onlineMode)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (onlineMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Text(
                        text = "Online",
                        fontSize = 14.sp, // Caption: 14sp
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = if (onlineMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Divider: very subtle, 1dp, low opacity
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // PART B - VOICE
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Text(
                        text = "Voice Detection Active",
                        fontSize = 14.sp, // Caption: 14sp
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Adaptive spacing

            // Recent Conversations Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp), // Top margin: 24dp
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface // Soft Material surface
                ),
                shape = RoundedCornerShape(22.dp), // Corner Radius: 22dp
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Soft elevation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp) // Padding: 20dp
                ) {
                    // Card Header with perfectly aligned "View All" on baseline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom // Perfect baseline alignment
                    ) {
                        Text(
                            text = "Recent Conversations",
                            fontSize = 18.sp, // Title: 18sp Medium
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                               ) { /* Handle View All */ },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val sdf = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary

                    val displayItems = remember(chatHistory, primaryColor, secondaryColor) {
                        chatHistory.takeLast(3).reversed().map { msg ->
                            val isUser = msg.sender == "user"
                            val avatar = if (isUser) "U" else "A"
                            val color = if (isUser) secondaryColor else primaryColor
                            val name = if (isUser) "User" else "Aira"
                            val timeStr = sdf.format(java.util.Date(msg.timestamp))
                            RecentConvItem(avatar, color, name, msg.message, timeStr)
                        }
                    }

                    if (displayItems.isEmpty()) {
                        Text(
                            text = "No recent conversations. Tap the voice orb to begin.",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        displayItems.forEachIndexed { index, item ->
                            RecentConversationRow(
                                avatarText = item.avatarText,
                                avatarBg = item.avatarBg,
                                name = item.name,
                                message = item.message,
                                time = item.time
                            )
                            if (index < displayItems.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
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

        AnimatedVisibility(
            visible = statusMsg != null && !isDismissed && !isAmyDownloaded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .padding(bottom = 8.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tts_download_overlay_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            val isError = statusMsg?.contains("fail", ignoreCase = true) == true || statusMsg?.contains("error", ignoreCase = true) == true
                            val icon = if (isError) Icons.Outlined.Warning else Icons.Outlined.CloudDownload
                            val iconColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            Icon(
                                imageVector = icon,
                                contentDescription = "TTS Download Status",
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isError) "Voice Setup Error" else "Acoustic Model Download",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (amyProgress != null && amyProgress in 0f..1f) {
                                Text(
                                    text = "${(amyProgress * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { isDismissed = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = statusMsg ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    
                    if (amyProgress != null && amyProgress in 0f..1f) {
                        LinearProgressIndicator(
                            progress = amyProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatLogBubble(msg: ChatMessage) {
    val isUser = msg.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
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
        }
    }
}

data class RecentConvItem(
    val avatarText: String,
    val avatarBg: Color,
    val name: String,
    val message: String,
    val time: String
)

@Composable
fun RecentConversationRow(
    avatarText: String,
    avatarBg: Color,
    name: String,
    message: String,
    time: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp), // Height: 68dp
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp) // Gap: 16dp
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp) // Avatar: 40dp
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
                fontWeight = FontWeight.Medium, // Conversation Name: 16sp Medium
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp, // Conversation Preview: 14sp Regular
                maxLines = 1,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal
            )
        }

        // Time
        Text(
            text = time,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 13.sp, // Time: 13sp
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal
        )
    }
}

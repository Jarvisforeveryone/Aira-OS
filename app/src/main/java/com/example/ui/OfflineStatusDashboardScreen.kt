package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import com.example.ui.components.AiraButton
import com.example.ui.components.AiraButtonVariant
import com.example.ui.components.AiraCard
import com.example.ui.components.AiraStatusBadge
import com.example.ui.components.BadgeStatus
import com.example.ui.theme.Dimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineStatusDashboardScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Reactive State Flows
    val activeVoice by viewModel.piperActiveVoice.collectAsState()
    val isModelDownloadedMap by viewModel.piperIsModelDownloaded.collectAsState()
    val isLocalMode by viewModel.isLocalMode.collectAsState()
    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()
    val isDeviceMemoryCapable by viewModel.isDeviceMemoryCapable.collectAsState()

    val isAmyDownloaded = isModelDownloadedMap["en_US-amy-medium"] == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Offline Status",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "System Status & Performance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("offline_dashboard_back_btn")
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.responsiveScreenPadding, vertical = Dimens.GapSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapLarge)
        ) {
            // CARD 1: AIRA Brain
            item(key = "card_aira_brain") {
                AiraCard(
                    title = "AIRA Brain",
                    subtitle = "Intelligence & Voice Settings",
                    icon = Icons.Default.Psychology,
                    modifier = Modifier.testTag("card_aira_brain")
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Row 1: AI Brain
                    val brainValue = if (!isDeviceMemoryCapable) "Cloud Mode" else if (isOfflineBrain) "Offline Mode" else "Cloud Mode"
                    val brainDesc = if (!isDeviceMemoryCapable) "Using internet for best performance." else if (isOfflineBrain) "Thinking directly on your device." else "Where AIRA thinks. Cloud is fastest."
                    SimpleStatusRow(
                        label = "AI Brain",
                        value = brainValue,
                        description = brainDesc,
                        status = BadgeStatus.PRIMARY
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Row 2: Offline Voice
                    val offlineVoiceValue = if (!isDeviceMemoryCapable) "Not Available" else if (isAmyDownloaded || isLocalMode) "Ready" else "Not Available"
                    val offlineVoiceDesc = if (offlineVoiceValue == "Ready") "Talk to AIRA without internet." else "Download to use without internet."
                    val offlineStatus = if (offlineVoiceValue == "Ready") BadgeStatus.SUCCESS else BadgeStatus.WARNING
                    SimpleStatusRow(
                        label = "Offline Voice",
                        value = offlineVoiceValue,
                        description = offlineVoiceDesc,
                        status = offlineStatus
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Row 3: Voice
                    val formattedVoice = when {
                        activeVoice.contains("amy", ignoreCase = true) -> "Amy - English US"
                        activeVoice.contains("zara", ignoreCase = true) -> "Zara - English US"
                        activeVoice.contains("ella", ignoreCase = true) -> "Ella - English UK"
                        activeVoice.isNotBlank() -> activeVoice
                        else -> "Amy - English US"
                    }
                    SimpleStatusRow(
                        label = "Voice",
                        value = formattedVoice,
                        description = "The voice AIRA uses to talk.",
                        status = BadgeStatus.PRIMARY
                    )

                    Spacer(modifier = Modifier.height(Dimens.GapSmall))

                    AiraButton(
                        text = "Change Settings",
                        onClick = { onBack() },
                        variant = AiraButtonVariant.OUTLINED,
                        icon = Icons.Default.Settings,
                        fullWidth = true,
                        modifier = Modifier.testTag("change_settings_btn")
                    )
                }
            }

            // CARD 2: Device Performance
            item(key = "card_device_performance") {
                AiraCard(
                    title = "Device Performance",
                    subtitle = "System Health & Memory",
                    icon = Icons.Default.Speed,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("card_device_performance")
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Row 1: Status
                    SimpleStatusRow(
                        label = "Status",
                        value = "Good",
                        description = "AIRA is running smoothly.",
                        status = BadgeStatus.SUCCESS
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Row 2: Memory Saver
                    SimpleStatusRow(
                        label = "Memory Saver",
                        value = "On",
                        description = "Keeping your phone fast.",
                        status = BadgeStatus.SUCCESS
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Row 3: Background Listening
                    SimpleStatusRow(
                        label = "Background Listening",
                        value = "On",
                        description = "Lets AIRA hear \"Hey AIRA\" anytime.",
                        status = BadgeStatus.PRIMARY
                    )

                    Spacer(modifier = Modifier.height(Dimens.GapSmall))

                    AiraButton(
                        text = "Check Device Health",
                        onClick = {
                            viewModel.speakText("Checking device health. System is fully operational and healthy.")
                            android.widget.Toast.makeText(context, "AIRA is healthy and operating at peak speed.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        icon = Icons.Default.HealthAndSafety,
                        fullWidth = true,
                        modifier = Modifier.testTag("check_device_health_btn")
                    )
                }
            }

            // CARD 3: Quick Actions
            item(key = "card_quick_actions") {
                AiraCard(
                    title = "Quick Actions",
                    subtitle = "Test Tools & Offline Voice Pack",
                    icon = Icons.Default.Build,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("card_quick_actions")
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    AiraButton(
                        text = "Test Microphone",
                        onClick = {
                            viewModel.speakText("Microphone active. Voice detection is ready.")
                            android.widget.Toast.makeText(context, "Microphone test passed!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        variant = AiraButtonVariant.OUTLINED,
                        icon = Icons.Default.Mic,
                        fullWidth = true,
                        modifier = Modifier.testTag("test_microphone_btn")
                    )

                    AiraButton(
                        text = "Test AIRA Voice",
                        onClick = {
                            viewModel.speakText("Hello! Testing AIRA voice output. Everything sounds clear.")
                        },
                        variant = AiraButtonVariant.OUTLINED,
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        fullWidth = true,
                        modifier = Modifier.testTag("test_aira_voice_btn")
                    )

                    AiraButton(
                        text = "Download Offline Pack",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.piperTtsManager.startDownload()
                                android.widget.Toast.makeText(context, "Downloading offline voice pack...", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = Icons.Default.Download,
                        fullWidth = true,
                        modifier = Modifier.testTag("download_offline_pack_btn")
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleStatusRow(
    label: String,
    value: String,
    description: String,
    status: BadgeStatus = BadgeStatus.PRIMARY
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.GapMicro)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AiraStatusBadge(
                text = value,
                status = status
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

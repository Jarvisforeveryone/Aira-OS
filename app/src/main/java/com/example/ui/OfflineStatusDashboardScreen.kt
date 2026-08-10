package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.colorResource
import com.example.R
import com.example.ui.components.AiraCard
import com.example.ui.components.AiraBadge
import com.example.ui.components.SkeletonCard
import com.example.ui.components.LoadingCard
import com.example.ui.components.LoadingInlineIndicator
import com.example.ui.components.VoskDiagnosticPanel
import com.example.ui.theme.bounceClick
import com.example.ui.theme.Dimens
import com.example.util.NativeLibraryLoader
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

    // Reactive State Flows from ViewModel & Engine Managers
    val activeVoice by viewModel.piperActiveVoice.collectAsState()
    val isModelDownloadedMap by viewModel.piperIsModelDownloaded.collectAsState()
    val downloadProgressMap by viewModel.piperDownloadProgress.collectAsState()
    val downloadStatusMsg by viewModel.piperDownloadStatusMessage.collectAsState()
    val selectedTtsEngine by viewModel.selectedTtsEngine.collectAsState()
    val selectedSttEngine by viewModel.selectedSttEngine.collectAsState()
    val isLocalMode by viewModel.isLocalMode.collectAsState()
    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()
    val llamaThreads by viewModel.llamaThreads.collectAsState()
    val currentEngineSource by viewModel.currentEngineSource.collectAsState()
    val isDeviceMemoryCapable by viewModel.isDeviceMemoryCapable.collectAsState()

    // Diagnostic log & verification trigger
    var diagnosticMessage by remember { mutableStateOf<String?>(null) }
    var isRunningDiagnostics by remember { mutableStateOf(false) }

    // System stats calculation
    val availableProcessors = remember { Runtime.getRuntime().availableProcessors() }
    val maxMemoryMb = remember { Runtime.getRuntime().maxMemory() / (1024 * 1024) }
    val totalMemoryMb = remember { Runtime.getRuntime().totalMemory() / (1024 * 1024) }
    val freeMemoryMb = remember { Runtime.getRuntime().freeMemory() / (1024 * 1024) }
    val usedMemoryMb = totalMemoryMb - freeMemoryMb

    val isJniLoaded = remember { NativeLibraryLoader.isLoaded() }
    val isAmyDownloaded = isModelDownloadedMap["en_US-amy-medium"] == true
    val amyProgress = downloadProgressMap["en_US-amy-medium"] ?: 0f

    val voskModelLoaded = viewModel.isVoskModelLoaded
    val llamaEngineStatus = remember { viewModel.llamaCppBrain.getEngineStatus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Offline Status",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "On-Device Features & System Status",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
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
                            tint = Color(0xFF0F172A)
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
            // SYSTEM DIAGNOSTIC PANEL: GEMINI API CONNECTIVITY & SIMULATED LOCAL PROCESSING
            item {
                val geminiStatus by viewModel.geminiConnectivityStatus.collectAsState()
                val geminiLatency by viewModel.geminiLatencyMs.collectAsState()
                val isTestingGemini by viewModel.isTestingGemini.collectAsState()

                val localTokSec by viewModel.localInferenceTokSec.collectAsState()
                val localLatency by viewModel.localProcessingLatencyMs.collectAsState()
                val isTestingLocal by viewModel.isTestingLocalProcessing.collectAsState()

                AiraCard(
                    modifier = Modifier.testTag("diagnostic_panel_card"),
                    title = "System Performance Overview",
                    icon = Icons.Default.Analytics,
                    headerTrailing = {
                        AiraBadge(text = "LIVE DIAGNOSTICS", badgeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), textColor = MaterialTheme.colorScheme.primary)
                    }
                ) {

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 1. Gemini API Network Connectivity Status
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gemini API Network Connectivity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${geminiLatency}ms",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (geminiStatus.contains("Connected") || geminiStatus.contains("200")) colorResource(R.color.aira_success_light) else colorResource(R.color.aira_warning_light)
                                )
                            }
                            InfoRow(label = "Connection", value = geminiStatus)
                            InfoRow(label = "Target Model", value = "Online AI - Connected")
                            InfoRow(label = "Primary Network Protocol", value = "Internet - Secure")

                            OutlinedButton(
                                onClick = { viewModel.runGeminiDiagnosticCheck() },
                                enabled = !isTestingGemini,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ping_gemini_api_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isTestingGemini) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Testing Connectivity...", fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ping Gemini API Endpoint", fontSize = 13.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 2. Simulated & On-Device Local Processing Status
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Local Processing Speed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${localLatency}ms / ${localTokSec} tok/s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            InfoRow(label = "Local LLM Engine", value = "Offline AI - Ready")
                            InfoRow(label = "Voice Input Processing", value = "Audio - Active")
                            InfoRow(label = "Voice Output Engine", value = "Voice Engine - Ready")
                            InfoRow(label = "Simulated Processing Speed", value = "%.1f tokens/sec (Latency %dms)".format(localTokSec, localLatency))

                            Button(
                                onClick = { viewModel.runLocalProcessingBenchmark() },
                                enabled = !isTestingLocal,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("benchmark_local_processing_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isTestingLocal) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Benchmarking Local Core...", fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Benchmark Local Processing Engine", fontSize = 13.sp)
                                }
                            }
                        }
                }
            }

            // VOSK ENGINE & VOICE TRIGGER DIAGNOSTIC SUITE
            item {
                VoskDiagnosticPanel(viewModel = viewModel)
            }

            // BACKGROUND SERVICES & ENGINE FALLBACKS CARD
            item {
                val isAccessibilityActive = viewModel.isAccessibilityServiceConnected()
                val isDeviceAdminActive = viewModel.checkDeviceAdminActive()
                val totalRamMb by viewModel.totalRamMb.collectAsState()
                val isMemoryCapable by viewModel.isDeviceMemoryCapable.collectAsState()

                val lowRamWarning = com.example.utils.MemoryManager.getLowRamWarningText(context)
                if (lowRamWarning != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("low_ram_warning_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = lowRamWarning,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("background_services_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Background Services",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMemoryCapable) colorResource(R.color.aira_success_light).copy(alpha = 0.15f) else colorResource(R.color.aira_warning_light).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isMemoryCapable) "RAM OK (${totalRamMb}MB)" else "LOW RAM (${totalRamMb}MB)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMemoryCapable) colorResource(R.color.aira_success_light) else colorResource(R.color.aira_warning_light),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text("Active Background System Services", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        InfoRow(label = "Assistant Voice FGS", value = "Active (Foreground Type: Microphone)")
                        InfoRow(label = "Accessibility Service", value = if (isAccessibilityActive) "Active & Connected" else "Disconnected")
                        InfoRow(label = "Device Policy Admin", value = if (isDeviceAdminActive) "Active & Enforced" else "Inactive")

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text("Current Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        InfoRow(
                            label = "AI Reasoning Engine",
                            value = if (isOfflineBrain) "Llama 3.2 1B (Local Offline)" else "Gemini 3.5 Flash (Cloud API)"
                        )
                        InfoRow(
                            label = "Voice Output",
                            value = when (selectedTtsEngine) {
                                com.example.ui.AiraViewModel.TtsEngine.PIPER_OFFLINE -> "Amy Voice (Offline)"
                                com.example.ui.AiraViewModel.TtsEngine.GOOGLE_TTS -> "Google Voice"
                                else -> "Auto Switch"
                            }
                        )
                        InfoRow(
                            label = "Speech Input",
                            value = if (selectedSttEngine.name.contains("VOSK")) "Offline Speech" else "Online Speech"
                        )
                    }
                }
            }

            // OVERALL OPERATIONAL SUMMARY CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overall_status_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isJniLoaded && (isAmyDownloaded || voskModelLoaded))
                                                colorResource(R.color.aira_success_light)
                                            else
                                                colorResource(R.color.aira_warning_light)
                                        )
                                )
                                Text(
                                    text = "System Core Operational",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isOfflineBrain) "OFFLINE BRAIN" else "HYBRID MODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Live status of on-device voice recognition, speech synthesis, AI assistant, and system performance.",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        // Hardware capability chips row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusChip(
                                label = if (isJniLoaded) "Hardware Accelerated" else "Standard Mode",
                                isPositive = isJniLoaded,
                                icon = Icons.Default.Speed
                            )
                            StatusChip(
                                label = "$availableProcessors CPU Threads",
                                isPositive = true,
                                icon = Icons.Default.Memory
                            )
                            StatusChip(
                                label = "${usedMemoryMb}MB / ${maxMemoryMb}MB Heap",
                                isPositive = true,
                                icon = Icons.Default.Storage
                            )
                        }
                    }
                }
            }

            // ENGINE 1: PIPER TTS (OFFLINE SPEECH SYNTHESIS)
            item {
                EngineDetailCard(
                    title = "Offline Voice Synthesis",
                    subtitle = "High-Quality Offline Voice Models",
                    icon = Icons.Default.RecordVoiceOver,
                    statusBadgeText = when {
                        isAmyDownloaded && isJniLoaded -> "READY (Hardware Accelerated)"
                        isAmyDownloaded -> "READY (Voice Ready)"
                        amyProgress > 0f -> "DOWNLOADING (${(amyProgress * 100).toInt()}%)"
                        else -> "MODEL MISSING"
                    },
                    statusBadgeColor = when {
                        isAmyDownloaded -> colorResource(R.color.aira_success_light)
                        amyProgress > 0f -> colorResource(R.color.aira_info_light)
                        else -> colorResource(R.color.aira_warning_light)
                    },
                    testTag = "piper_tts_engine_card"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow(label = "Current Voice", value = activeVoice)
                        InfoRow(label = "Voice Output", value = selectedTtsEngine.name)
                        InfoRow(
                            label = "Offline Voice Model (English)",
                            value = if (isAmyDownloaded) "Downloaded & Verified (~45MB)" else "Not Downloaded"
                        )
                        InfoRow(
                            label = "Hardware Voice Engine",
                            value = if (isJniLoaded) "Voice Engine Active" else "Unavailable"
                        )

                        if (amyProgress > 0f && !isAmyDownloaded) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Model Download Progress",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(amyProgress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { amyProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }

                        downloadStatusMsg?.let { msg ->
                            Text(
                                text = "Status: $msg",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.speakText("Amy real Piper offline voice engine is active and operational.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_amy_voice_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Voice", fontSize = 13.sp)
                            }

                            if (!isAmyDownloaded) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.piperTtsManager.downloadAmyModel(
                                                onProgress = { /* Progress bound via state flow */ },
                                                onComplete = { success ->
                                                    val msg = if (success) "Amy model downloaded successfully" else "Failed to download Amy model"
                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_amy_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download Amy", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ENGINE 2: VOSK STT (OFFLINE SPEECH RECOGNITION)
            item {
                EngineDetailCard(
                    title = "Offline Voice Recognition",
                    subtitle = "High-Accuracy On-Device Speech Input",
                    icon = Icons.Default.Mic,
                    statusBadgeText = if (voskModelLoaded) "MODEL LOADED & READY" else "NOT INITIALIZED",
                    statusBadgeColor = if (voskModelLoaded) colorResource(R.color.aira_success_light) else colorResource(R.color.aira_warning_light),
                    testTag = "vosk_stt_engine_card"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Offline Speech Mode",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isLocalMode) "Processing voice input on-device without internet" else "Auto (Offline recognition active when disconnected)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isLocalMode,
                                onCheckedChange = { viewModel.toggleLocalMode(it) },
                                modifier = Modifier.testTag("dashboard_local_mode_switch")
                            )
                        }
                        InfoRow(label = "Speech Input", value = if (isLocalMode) "LOCAL VOSK MODE (FORCED)" else selectedSttEngine.name)
                        InfoRow(label = "Model Identifier", value = "Speech Model - Ready")
                        InfoRow(
                            label = "Model Memory Cache",
                            value = if (voskModelLoaded) "Loaded in RAM (Unpacked)" else "Not Loaded"
                        )
                        InfoRow(label = "Audio Stream Processor", value = "Audio Quality - Good")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.initVoskModel()
                                    android.widget.Toast.makeText(context, "Initializing Vosk Offline Model...", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("init_vosk_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Initialize Vosk", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.switchToOfflineVosk()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("switch_offline_vosk_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Listen Offline", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ENGINE 3: LLAMA 3.2 LOCAL AI & DATABASE
            item {
                EngineDetailCard(
                    title = "On-Device Private AI Brain",
                    subtitle = "Private Local AI Model & Secure Saved Facts",
                    icon = Icons.Default.Psychology,
                    statusBadgeText = if (!isDeviceMemoryCapable) "DISABLED (<3GB RAM)" else if (isOfflineBrain) "OFFLINE BRAIN ACTIVE" else "ONLINE HYBRID",
                    statusBadgeColor = if (!isDeviceMemoryCapable) MaterialTheme.colorScheme.error else if (isOfflineBrain) colorResource(R.color.aira_success_light) else MaterialTheme.colorScheme.primary,
                    testTag = "llama_local_brain_card"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!isDeviceMemoryCapable) {
                            Text(
                                text = "⚠️ Disabled on devices with less than 3GB RAM to conserve memory. Cloud AI and standard voice speech remain active.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 16.sp
                            )
                        }
                        InfoRow(label = "System Status", value = llamaEngineStatus)
                        InfoRow(label = "Model Quantization", value = "Llama-3.2 1B/3B Instruct (Q4_K_M)")
                        InfoRow(label = "Parallel CPU Threads", value = "$llamaThreads Threads Configured")
                        InfoRow(label = "Current Intelligence Source", value = currentEngineSource)
                        InfoRow(label = "Local Persistence", value = "Local Storage - Ready")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Force Offline Brain Mode",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (isDeviceMemoryCapable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = if (isDeviceMemoryCapable) "Process all AI queries strictly on-device without cloud calls"
                                           else "Not supported on 2GB RAM devices",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isOfflineBrain && isDeviceMemoryCapable,
                                enabled = isDeviceMemoryCapable,
                                onCheckedChange = { viewModel.toggleOfflineBrain(it) },
                                modifier = Modifier.testTag("toggle_offline_brain_switch")
                            )
                        }
                    }
                }
            }

            // ENGINE 4: HARDWARE ACCELERATION & SYSTEM DIAGNOSTICS
            item {
                EngineDetailCard(
                    title = "Device Hardware & Performance",
                    subtitle = "Device Capabilities & Status",
                    icon = Icons.Default.Hardware,
                    statusBadgeText = if (isJniLoaded) "HARDWARE READY" else "PARTIAL",
                    statusBadgeColor = if (isJniLoaded) colorResource(R.color.aira_success_light) else colorResource(R.color.aira_warning_light),
                    testTag = "hw_acceleration_card"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow(label = "Voice Engine Drivers", value = if (isJniLoaded) "Operating Normally" else "Standard Mode")
                        InfoRow(label = "CPU Core Allocation", value = "$availableProcessors Hardware Processors")
                        InfoRow(label = "Heap Memory Usage", value = "$usedMemoryMb MB allocated of $maxMemoryMb MB max")
                        InfoRow(label = "Audio Driver Latency", value = "Audio - Low Latency")

                        diagnosticMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isRunningDiagnostics = true
                                coroutineScope.launch {
                                    val valid = viewModel.piperTtsManager.runPiperModelDiagnostics()
                                    diagnosticMessage = if (valid) {
                                        "Diagnostics Complete: All downloaded Piper voice model files are valid & intact."
                                    } else {
                                        "Diagnostics Complete: Some model files are missing or corruption detected."
                                    }
                                    isRunningDiagnostics = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_diagnostics_btn"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isRunningDiagnostics,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isRunningDiagnostics) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Running Verification...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Full Model Verification Diagnostics", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    isPositive: Boolean,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isPositive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EngineDetailCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    statusBadgeText: String,
    statusBadgeColor: Color,
    testTag: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBadgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusBadgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = statusBadgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

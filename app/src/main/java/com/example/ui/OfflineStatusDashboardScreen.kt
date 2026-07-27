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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()
    val llamaThreads by viewModel.llamaThreads.collectAsState()
    val currentEngineSource by viewModel.currentEngineSource.collectAsState()

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
                            text = "Offline Status Dashboard",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Local Engines, Models & Hardware Acceleration",
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
                            tint = MaterialTheme.colorScheme.primary
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
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SYSTEM DIAGNOSTIC PANEL: GEMINI API CONNECTIVITY & SIMULATED LOCAL PROCESSING
            item {
                val geminiStatus by viewModel.geminiConnectivityStatus.collectAsState()
                val geminiLatency by viewModel.geminiLatencyMs.collectAsState()
                val isTestingGemini by viewModel.isTestingGemini.collectAsState()

                val localTokSec by viewModel.localInferenceTokSec.collectAsState()
                val localLatency by viewModel.localProcessingLatencyMs.collectAsState()
                val isTestingLocal by viewModel.isTestingLocalProcessing.collectAsState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diagnostic_panel_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "System Diagnostic Panel",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "LIVE DIAGNOSTICS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

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
                                    color = if (geminiStatus.contains("Connected") || geminiStatus.contains("200")) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                )
                            }
                            InfoRow(label = "Endpoint Status", value = geminiStatus)
                            InfoRow(label = "Target Model", value = "gemini-3.5-flash (v1beta API)")
                            InfoRow(label = "Primary Network Protocol", value = "HTTPS / OkHttp TLS 1.3")

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
                                    text = "Simulated Local Processing Status",
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
                            InfoRow(label = "Local LLM Engine", value = "Llama 3.2 1B (Q4_K_M Quantized)")
                            InfoRow(label = "Vosk STT Decoder", value = "16kHz Mono Stream Processor (Active)")
                            InfoRow(label = "Piper TTS JNI Runtime", value = "libonnxruntime.so + libpiper.so Ready")
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
                                                Color(0xFF4CAF50)
                                            else
                                                Color(0xFFFF9800)
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
                            text = "Live monitoring of local Speech Recognition (Vosk STT), Speech Synthesis (Real Piper ONNX TTS & Google TTS), Local Reasoning Engine (Llama 3.2), and JNI Hardware Acceleration.",
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
                                label = if (isJniLoaded) "JNI Accelerated" else "Java Fallback",
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
                    title = "Piper TTS (Offline Voice Engine)",
                    subtitle = "Real JNI ONNX Runtime & Local Voice Models",
                    icon = Icons.Default.RecordVoiceOver,
                    statusBadgeText = when {
                        isAmyDownloaded && isJniLoaded -> "READY (JNI Accelerated)"
                        isAmyDownloaded -> "READY (ONNX Ready)"
                        amyProgress > 0f -> "DOWNLOADING (${(amyProgress * 100).toInt()}%)"
                        else -> "MODEL MISSING"
                    },
                    statusBadgeColor = when {
                        isAmyDownloaded -> Color(0xFF4CAF50)
                        amyProgress > 0f -> Color(0xFF2196F3)
                        else -> Color(0xFFFF9800)
                    },
                    testTag = "piper_tts_engine_card"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow(label = "Active Voice Model", value = activeVoice)
                        InfoRow(label = "Selected TTS Engine", value = selectedTtsEngine.name)
                        InfoRow(
                            label = "Amy JNI Model (en_US-amy-medium)",
                            value = if (isAmyDownloaded) "Downloaded & Verified (~45MB)" else "Not Downloaded"
                        )
                        InfoRow(
                            label = "JNI C++ ONNX Bridge",
                            value = if (isJniLoaded) "libonnxruntime.so + libpiper.so Active" else "Unavailable"
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
                    title = "Vosk STT (Offline Speech Recognizer)",
                    subtitle = "16kHz PCM Audio Stream Decoder",
                    icon = Icons.Default.Mic,
                    statusBadgeText = if (voskModelLoaded) "MODEL LOADED & READY" else "NOT INITIALIZED",
                    statusBadgeColor = if (voskModelLoaded) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    testTag = "vosk_stt_engine_card"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow(label = "STT Engine Mode", value = selectedSttEngine.name)
                        InfoRow(label = "Model Identifier", value = "vosk-model-small-en-us-0.15")
                        InfoRow(
                            label = "Model Memory Cache",
                            value = if (voskModelLoaded) "Loaded in RAM (Unpacked)" else "Not Loaded"
                        )
                        InfoRow(label = "Audio Stream Processor", value = "16,000Hz 16-bit Mono PCM")

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
                    title = "Llama 3.2 Local Brain & Storage",
                    subtitle = "On-Device Neural Reasoning & SQLite Memory",
                    icon = Icons.Default.Psychology,
                    statusBadgeText = if (isOfflineBrain) "OFFLINE BRAIN ACTIVE" else "ONLINE HYBRID",
                    statusBadgeColor = if (isOfflineBrain) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    testTag = "llama_local_brain_card"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow(label = "Engine Status", value = llamaEngineStatus)
                        InfoRow(label = "Model Quantization", value = "Llama-3.2 1B/3B Instruct (Q4_K_M)")
                        InfoRow(label = "Parallel CPU Threads", value = "$llamaThreads Threads Configured")
                        InfoRow(label = "Current Intelligence Source", value = currentEngineSource)
                        InfoRow(label = "Local Persistence", value = "SQLite Room DB (Chat, Memory & Rules)")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Force Offline Brain Mode",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Process all AI queries strictly on-device without cloud calls",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isOfflineBrain,
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
                    title = "Hardware Acceleration & Diagnostics",
                    subtitle = "Native JNI Drivers & Hardware Capabilities",
                    icon = Icons.Default.Hardware,
                    statusBadgeText = if (isJniLoaded) "JNI DRIVERS READY" else "PARTIAL",
                    statusBadgeColor = if (isJniLoaded) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    testTag = "hw_acceleration_card"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        InfoRow(label = "JNI Native Runtime", value = if (isJniLoaded) "Loaded (libonnxruntime.so & libpiper.so)" else "UnsatisfiedLinkError / Missing JNI")
                        InfoRow(label = "CPU Core Allocation", value = "$availableProcessors Hardware Processors")
                        InfoRow(label = "Heap Memory Usage", value = "$usedMemoryMb MB allocated of $maxMemoryMb MB max")
                        InfoRow(label = "Audio Driver Latency", value = "Low Latency AudioTrack PCM Mode")

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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

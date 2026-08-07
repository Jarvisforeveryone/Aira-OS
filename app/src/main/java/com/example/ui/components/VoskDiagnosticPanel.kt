package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AiraViewModel
import com.example.utils.VoskLogEntry
import com.example.utils.VoskLogLevel
import com.example.utils.VoskLogManager
import kotlinx.coroutines.launch

@Composable
fun VoskDiagnosticPanel(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel Diagnostic States
    val rawAudioLevel by viewModel.voskRawAudioLevel.collectAsState()
    val confidenceScore by viewModel.voskConfidenceScore.collectAsState()
    val wordConfidences by viewModel.voskWordConfidences.collectAsState()
    val triggerStatus by viewModel.voskTriggerStatus.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isVoskLoaded = viewModel.isVoskModelLoaded

    // Log Manager States
    val allLogs by VoskLogManager.logs.collectAsState()
    val initErrorCount by VoskLogManager.initErrorCount.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, INIT_ERRORS, WARN_ERR
    var searchText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(true) }

    val filteredLogs = remember(allLogs, selectedFilter, searchText) {
        allLogs.filter { entry ->
            val matchesFilter = when (selectedFilter) {
                "INIT_ERRORS" -> entry.level == VoskLogLevel.INIT_ERROR
                "WARN_ERR" -> entry.level == VoskLogLevel.WARN || entry.level == VoskLogLevel.ERROR || entry.level == VoskLogLevel.INIT_ERROR
                else -> true
            }
            val matchesSearch = if (searchText.isBlank()) true else {
                entry.message.contains(searchText, ignoreCase = true) ||
                        entry.tag.contains(searchText, ignoreCase = true) ||
                        (entry.throwableMessage?.contains(searchText, ignoreCase = true) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll log terminal to bottom on new log entries
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vosk_diagnostic_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isVoskLoaded) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Vosk Engine Status",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Vosk Trigger & STT Diagnostic Suite",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isVoskLoaded) "Engine Loaded • Real-Time Monitor Active" else "Engine Offline • STT Init Failed",
                            fontSize = 12.sp,
                            color = if (isVoskLoaded) Color(0xFF4CAF50) else Color(0xFFEF5350)
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.testTag("toggle_vosk_diagnostic_expand")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse Diagnostics",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 1. Live Input Audio Level Meter
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Raw Audio Input Level",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val levelPercent = (rawAudioLevel * 100).toInt().coerceIn(0, 100)
                            val levelLabel = when {
                                !isListening -> "Mic Off"
                                rawAudioLevel < 0.05f -> "Silent ($levelPercent%)"
                                rawAudioLevel > 0.90f -> "Clipping/Noise ($levelPercent%)"
                                else -> "Normal Input ($levelPercent%)"
                            }
                            Text(
                                text = levelLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    !isListening -> MaterialTheme.colorScheme.onSurfaceVariant
                                    rawAudioLevel < 0.05f -> Color(0xFFFFA726)
                                    rawAudioLevel > 0.90f -> Color(0xFFEF5350)
                                    else -> Color(0xFF66BB6A)
                                }
                            )
                        }

                        // Meter Progress Bar
                        val animatedLevel by animateFloatAsState(targetValue = rawAudioLevel, label = "audio_level")
                        val meterColor by animateColorAsState(
                            targetValue = when {
                                animatedLevel < 0.05f -> Color(0xFFFFA726)
                                animatedLevel > 0.90f -> Color(0xFFEF5350)
                                else -> Color(0xFF66BB6A)
                            },
                            label = "meter_color"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedLevel.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(meterColor)
                            )
                        }
                    }

                    // 2. Recognition Confidence Visualizer
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recognition Confidence Score",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val confPercent = (confidenceScore * 100).toInt().coerceIn(0, 100)
                            Text(
                                text = "$confPercent% (Target ≥ 60%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (confidenceScore >= 0.60f) Color(0xFF4CAF50) else Color(0xFFFFA726)
                            )
                        }

                        // Confidence Meter with 60% threshold marker
                        val animatedConf by animateFloatAsState(targetValue = confidenceScore, label = "confidence")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedConf.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (animatedConf >= 0.60f) Color(0xFF4CAF50) else Color(0xFFFFA726))
                            )
                            // 60% Threshold Line
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(2.dp)
                                    .align(Alignment.CenterStart)
                                    .offset(x = 180.dp) // Approximate visual threshold
                                    .background(Color.White)
                            )
                        }

                        // Word-by-Word Breakdown Chips
                        if (wordConfidences.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                wordConfidences.take(5).forEach { (word, conf) ->
                                    val cPct = (conf * 100).toInt()
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (conf >= 0.60f) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = "$word ($cPct%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Trigger Status Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Trigger Status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Current Trigger Status",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = triggerStatus,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 4. Real-Time Filtered Vosk Logs Terminal
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Real-Time Vosk Engine Logs",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (initErrorCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFD32F2F),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = "$initErrorCount STT Init Error(s)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val formatted = VoskLogManager.getFormattedLogsText()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Vosk Engine Logs", formatted))
                                        Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("copy_vosk_logs_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Logs",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { VoskLogManager.clearLogs() },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("clear_vosk_logs_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear Logs",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Filter Chips Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                label = { Text("All (${allLogs.size})", fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_all_logs")
                            )

                            FilterChip(
                                selected = selectedFilter == "INIT_ERRORS",
                                onClick = { selectedFilter = "INIT_ERRORS" },
                                label = { Text("STT Init Errors ($initErrorCount)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD32F2F),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_init_error_logs")
                            )

                            FilterChip(
                                selected = selectedFilter == "WARN_ERR",
                                onClick = { selectedFilter = "WARN_ERR" },
                                label = { Text("Warnings/Errors", fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_warn_err_logs")
                            )
                        }

                        // Terminal Log Window
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF121212),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            if (filteredLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No Vosk engine logs captured yet.",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filteredLogs, key = { it.id }) { entry ->
                                        VoskLogLine(entry = entry)
                                    }
                                }
                            }
                        }

                        // Re-Init Diagnostic Action Button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.reinitVoskForDiagnostic()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reinit_vosk_diagnostic_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Test STT Re-Init",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Vosk STT Init Diagnostic Pipeline", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoskLogLine(entry: VoskLogEntry) {
    val levelColor = when (entry.level) {
        VoskLogLevel.INFO -> Color(0xFF81C784)
        VoskLogLevel.WARN -> Color(0xFFFFB74D)
        VoskLogLevel.ERROR -> Color(0xFFE57373)
        VoskLogLevel.INIT_ERROR -> Color(0xFFFF5252)
    }

    val tagLabel = when (entry.level) {
        VoskLogLevel.INIT_ERROR -> "[STT_INIT_ERR]"
        VoskLogLevel.ERROR -> "[ERROR]"
        VoskLogLevel.WARN -> "[WARN]"
        VoskLogLevel.INFO -> "[INFO]"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = entry.formattedTime,
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = tagLabel,
                color = levelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "[${entry.tag}]",
                color = Color.LightGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = entry.message,
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        if (entry.throwableMessage != null) {
            Text(
                text = "↳ ${entry.throwableMessage}",
                color = Color(0xFFFF8A80),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

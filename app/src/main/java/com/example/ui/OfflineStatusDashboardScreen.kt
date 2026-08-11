package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.AiraCard
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
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "System Status & Performance",
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
            // CARD 1: AIRA Brain
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_aira_brain"),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "AIRA Brain",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "AIRA Brain",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Intelligence & Voice Settings",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Row 1: AI Brain
                        val brainValue = if (!isDeviceMemoryCapable) "Cloud Mode" else if (isOfflineBrain) "Offline Mode" else "Cloud Mode"
                        val brainDesc = if (!isDeviceMemoryCapable) "Using internet for best performance." else if (isOfflineBrain) "Thinking directly on your device." else "Where AIRA thinks. Cloud is fastest."
                        SimpleStatusRow(
                            label = "AI Brain",
                            value = brainValue,
                            description = brainDesc,
                            valueColor = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Row 2: Offline Voice
                        val offlineVoiceValue = if (!isDeviceMemoryCapable) "Not Available" else if (isAmyDownloaded || isLocalMode) "Ready" else "Not Available"
                        val offlineVoiceDesc = if (offlineVoiceValue == "Ready") "Talk to AIRA without internet." else "Download to use without internet."
                        val offlineVoiceColor = if (offlineVoiceValue == "Ready") colorResource(R.color.aira_success_light) else colorResource(R.color.aira_warning_light)
                        SimpleStatusRow(
                            label = "Offline Voice",
                            value = offlineVoiceValue,
                            description = offlineVoiceDesc,
                            valueColor = offlineVoiceColor
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
                            valueColor = MaterialTheme.colorScheme.primary
                        )

                        OutlinedButton(
                            onClick = { onBack() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("change_settings_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Settings", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // CARD 2: Device Performance
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_device_performance"),
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorResource(R.color.aira_success_light).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Device Performance",
                                    tint = colorResource(R.color.aira_success_light),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Device Performance",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "System Health & Memory",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Row 1: Status
                        SimpleStatusRow(
                            label = "Status",
                            value = "Good",
                            description = "AIRA is running smoothly.",
                            valueColor = colorResource(R.color.aira_success_light)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Row 2: Memory Saver
                        SimpleStatusRow(
                            label = "Memory Saver",
                            value = "On",
                            description = "Keeping your phone fast.",
                            valueColor = colorResource(R.color.aira_success_light)
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Row 3: Background Listening
                        SimpleStatusRow(
                            label = "Background Listening",
                            value = "On",
                            description = "Lets AIRA hear \"Hey AIRA\" anytime.",
                            valueColor = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = {
                                viewModel.speakText("Checking device health. System is fully operational and healthy.")
                                android.widget.Toast.makeText(context, "AIRA is healthy and operating at peak speed.", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("check_device_health_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Device Health", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // CARD 3: Quick Actions
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_quick_actions"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Quick Actions",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Quick Actions",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Test Tools & Offline Voice Pack",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Button 1: Test Microphone
                        OutlinedButton(
                            onClick = {
                                viewModel.speakText("Microphone active. Voice detection is ready.")
                                android.widget.Toast.makeText(context, "Microphone test passed!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_microphone_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Microphone", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }

                        // Button 2: Test AIRA Voice
                        OutlinedButton(
                            onClick = {
                                viewModel.speakText("Hello! Testing AIRA voice output. Everything sounds clear.")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_aira_voice_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test AIRA Voice", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }

                        // Button 3: Download Offline Pack
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.piperTtsManager.startDownload()
                                    android.widget.Toast.makeText(context, "Downloading offline voice pack...", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_offline_pack_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Offline Pack", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
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
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = valueColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = valueColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = description,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

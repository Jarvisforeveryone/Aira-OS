package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
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
import com.example.ui.AiraViewModel
import com.example.ui.theme.Dimens
import com.example.utils.ShizukuManager

@Composable
fun DeviceControlSetupCard(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier,
    sdkVersionOverride: Int? = null
) {
    val context = LocalContext.current
    val isShizukuRunning by viewModel.isShizukuRunning.collectAsState()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()
    val effectiveSdkVersion = sdkVersionOverride ?: Build.VERSION.SDK_INT

    var isShizukuInstalled by remember { mutableStateOf(ShizukuManager.isShizukuInstalled(context)) }
    var isLadbInstalled by remember { mutableStateOf(ShizukuManager.isLadbInstalled(context)) }

    fun refreshAllStatus() {
        viewModel.refreshShizukuStatus()
        isShizukuInstalled = ShizukuManager.isShizukuInstalled(context)
        isLadbInstalled = ShizukuManager.isLadbInstalled(context)
    }

    LaunchedEffect(Unit) {
        refreshAllStatus()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_control_setup_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isShizukuRunning && isShizukuGranted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.dp,
            if (isShizukuRunning && isShizukuGranted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isShizukuRunning && isShizukuGranted) {
                                    colorResource(id = R.color.aira_success_light)
                                } else if (isShizukuRunning) {
                                    colorResource(id = R.color.aira_warning_light)
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                shape = CircleShape
                            )
                            .testTag("device_control_status_dot")
                    )
                    Text(
                        text = "Device Control Setup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { refreshAllStatus() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("device_control_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Status",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Status State Block
            if (isShizukuRunning && isShizukuGranted) {
                // ==========================================
                // STATE 1: ACTIVE (Green Status) - Setup Guide Hidden
                // ==========================================
                Surface(
                    color = colorResource(id = R.color.aira_success_light).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colorResource(id = R.color.aira_success_light).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_control_active_container")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = colorResource(id = R.color.aira_success_light),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Device Control Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("device_control_active_text")
                            )
                            Text(
                                text = "AIRA is connected via privileged Shizuku shell. Hardware controls (Wi-Fi, Bluetooth, GPS, Volume, Brightness) execute instantly via native svc/cmd hooks.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else if (isShizukuRunning && !isShizukuGranted) {
                // ==========================================
                // STATE 2: RUNNING BUT UNGRANTED
                // ==========================================
                Surface(
                    color = colorResource(id = R.color.aira_warning_light).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colorResource(id = R.color.aira_warning_light).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "Permission Required",
                                tint = colorResource(id = R.color.aira_warning_light)
                            )
                            Text(
                                text = "Shizuku Running (Permission Required)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Shizuku service is running in background. Grant permission to AIRA to enable direct hardware and system management.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.requestShizukuPermission() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_shizuku_perm_btn")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Grant Shizuku Permission")
                        }
                    }
                }
            } else {
                // ==========================================
                // STATE 3: SHIZUKU NOT RUNNING -> VERSION-SPECIFIC SETUP
                // ==========================================
                if (effectiveSdkVersion >= Build.VERSION_CODES.R) {
                    // -------------------------------------------------------------
                    // CASE 1: ANDROID 11+ (API >= 30) - WIRELESS DEBUGGING FLOW
                    // -------------------------------------------------------------
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag("android_11_setup_container")
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Android 11+ Guide",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Android 11+ Setup (Wireless Debugging)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Enable Wireless ADB in Developer Options. Open Shizuku, pair, and start server.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.testTag("android_11_instruction_text")
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { ShizukuManager.openShizukuApp(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("open_shizuku_app_btn")
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Shizuku", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { refreshAllStatus() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("refresh_status_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Check Status", fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    // -------------------------------------------------------------
                    // CASE 2: ANDROID 10 & BELOW (API <= 29) - LADB FLOW
                    // -------------------------------------------------------------
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag("android_10_ladb_setup_container")
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Android 10 LADB Guide",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Android 10 & Below (LADB / Local ADB)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Enable USB Debugging in Developer Options.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("android_10_instruction_text")
                                    )
                                }
                            }
                        }

                        // App Installation Checks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Step A: Shizuku
                            if (!isShizukuInstalled) {
                                Button(
                                    onClick = { ShizukuManager.openPlayStore(context, ShizukuManager.SHIZUKU_PACKAGE) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("install_shizuku_btn")
                                ) {
                                    Text("Install Shizuku", fontSize = 11.sp)
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colorResource(id = R.color.aira_success_light), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Shizuku Installed", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // Step B: LADB
                            if (!isLadbInstalled) {
                                Button(
                                    onClick = { ShizukuManager.openPlayStore(context, ShizukuManager.LADB_PACKAGE) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("install_ladb_btn")
                                ) {
                                    Text("Install LADB", fontSize = 11.sp)
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colorResource(id = R.color.aira_success_light), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("LADB Installed", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Step C: Command Box & Copy Button
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ladb_command_section")
                        ) {
                            Text(
                                text = "Open LADB, paste the command, and press enter. Shizuku will start.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("ladb_instruct_text")
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = ShizukuManager.LADB_SHIZUKU_START_COMMAND,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.testTag("ladb_command_text")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Shizuku Start Command", ShizukuManager.LADB_SHIZUKU_START_COMMAND)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Command copied! Paste it in LADB", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("copy_ladb_command_btn")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Copy Command", fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { ShizukuManager.openLadbApp(context) },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("open_ladb_btn")
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Open LADB", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Refresh / Check Connection Button
                        OutlinedButton(
                            onClick = { refreshAllStatus() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("refresh_ladb_status_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Refresh Status", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

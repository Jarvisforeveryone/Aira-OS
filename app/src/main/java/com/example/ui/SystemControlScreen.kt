package com.example.ui

import android.content.Intent
import android.media.AudioManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.colorResource
import com.example.R
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.LazyRow
import com.example.data.Action
import com.example.ui.theme.success
import com.example.ui.theme.warning
import com.example.data.Command
import com.example.data.Reminder
import com.example.data.VoiceCommandManager
import com.example.data.AppDatabase
import kotlinx.coroutines.launch

@Composable
fun SystemControlScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "automation_home",
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("automation_home") {
            AutomationHomeScreen(
                viewModel = viewModel,
                onNavigateToSmartAuto = { navController.navigate("smart_auto_screen") },
                onNavigateToMyActions = { navController.navigate("my_actions_screen") },
                onNavigateToVoiceCommands = { navController.navigate("voice_command_screen") }
            )
        }
        composable("smart_auto_screen") {
            SmartAutoScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("my_actions_screen") {
            MyActionsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("voice_command_screen") {
            VoiceCommandScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationHomeScreen(
    viewModel: AiraViewModel,
    onNavigateToSmartAuto: () -> Unit,
    onNavigateToMyActions: () -> Unit,
    onNavigateToVoiceCommands: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    val scrollState = rememberScrollState()

    // Screen-level state for quick controls
    var alarmHour by remember { mutableStateOf("07") }
    var alarmMinute by remember { mutableStateOf("00") }
    var reminderTitle by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("09:00 AM") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Device Control",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp, // Section Title: 22sp
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp), // Outer Screen Padding: 24dp
            verticalArrangement = Arrangement.spacedBy(28.dp) // Between Sections: 28dp
        ) {
            // ================== SYSTEM UTILS ==================
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "System Utilities",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp, // Labels: 13sp Medium
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp) // Between Cards: 18dp
                ) {
                    // Flashlight card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Flashlight",
                                fontSize = 16.sp, // Body: 16sp
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleFlashlight(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("flash_on_btn"),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "ON",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("On", fontSize = 13.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                                }
                                Button(
                                    onClick = { viewModel.toggleFlashlight(false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("flash_off_btn"),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOff,
                                        contentDescription = "OFF",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Off", fontSize = 13.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Audio Mode selector card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Profile",
                                fontSize = 16.sp, // Body
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.setSoundMode(AudioManager.RINGER_MODE_NORMAL) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Normal",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setSoundMode(AudioManager.RINGER_MODE_VIBRATE) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Vibrate",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.setSoundMode(AudioManager.RINGER_MODE_SILENT) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeMute,
                                        contentDescription = "Silent",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ALARM SYSTEM SCHEDULER
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Alarm Clock",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp, // Labels
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Set Chronos Trigger",
                            fontSize = 18.sp, // Card Title
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = alarmHour,
                                onValueChange = { alarmHour = it.take(2) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("alarm_hour_input"),
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Hour (24h)", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) }
                            )
                            TextField(
                                value = alarmMinute,
                                onValueChange = { alarmMinute = it.take(2) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("alarm_min_input"),
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Minute", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) }
                            )

                            Button(
                                onClick = {
                                    val h = alarmHour.toIntOrNull() ?: 7
                                    val m = alarmMinute.toIntOrNull() ?: 0
                                    viewModel.setSystemAlarm(h, m, "Aira Scheduled Alert")
                                    viewModel.speakText("Scheduled Chronos Alarm for $h:$m successfully")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(54.dp)
                                    .testTag("set_alarm_btn")
                            ) {
                                Text("Schedule", fontSize = 14.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }

            // SAVE REMINDERS BLOCK
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Reminders & Logbook",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp, // Labels
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Add Memory Task",
                            fontSize = 18.sp, // Card Title
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        TextField(
                            value = reminderTitle,
                            onValueChange = { reminderTitle = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reminder_title_input"),
                            textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            label = { Text("Task Message", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = reminderTime,
                                onValueChange = { reminderTime = it },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("reminder_time_input"),
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Scheduled Time", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) }
                            )

                            Button(
                                onClick = {
                                    if (reminderTitle.isNotEmpty()) {
                                        viewModel.addReminder(reminderTitle, reminderTime)
                                        viewModel.speakText("Commit memory log: $reminderTitle.")
                                        reminderTitle = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("add_reminder_btn")
                            ) {
                                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save", fontSize = 14.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // List of Reminders
                        if (reminders.isEmpty()) {
                            Text(
                                text = "No active reminders recorded",
                                fontSize = 14.sp, // Caption
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                reminders.forEach { reminder ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 64.dp)
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                             Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = reminder.title,
                                                    fontSize = 16.sp, // Body
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Alarm: " + reminder.timeLabel,
                                                    fontSize = 14.sp, // Caption
                                                    fontFamily = FontFamily.SansSerif,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteReminder(reminder) },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .testTag("delete_reminder_${reminder.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================== REDESIGNED SECTION: 3 NAVIGATION OPTIONS ==================
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "AI Routines & Automation",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp, // Labels
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(start = 8.dp)
                )

                // Option 1: Smart Auto
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSmartAuto() }
                        .testTag("smart_auto_nav_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 64.dp)
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Smart Suggestions",
                                fontSize = 18.sp, // Card Title
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Explore routine ideas generated by AI analysis.",
                                fontSize = 14.sp, // Caption
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Navigate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Option 2: My Actions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMyActions() }
                        .testTag("my_actions_nav_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 64.dp)
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "My Actions",
                                fontSize = 18.sp, // Card Title
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Manage and trigger configured automation actions.",
                                fontSize = 14.sp, // Caption
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Navigate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Option 3: Voice Commands
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToVoiceCommands() }
                        .testTag("voice_commands_nav_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 64.dp)
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Voice Commands",
                                fontSize = 18.sp, // Card Title
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Bind vocal trigger phrases to target action queues.",
                                fontSize = 14.sp, // Caption
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Navigate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Voice Command Execution Log Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Execution Activity",
                        fontSize = 13.sp, // Labels
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    val logs by viewModel.voiceCommandLogs.collectAsState()
                    if (logs.isNotEmpty()) {
                        Text(
                            text = "CLEAR ALL",
                            fontSize = 13.sp, // Labels
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clickable { viewModel.clearVoiceCommandLogs() }
                                .testTag("clear_voice_logs_btn")
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                val logs by viewModel.voiceCommandLogs.collectAsState()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_command_logs_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Text(
                                text = "No recent vocal executions logged.",
                                fontSize = 14.sp, // Caption
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                logs.forEach { log ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Mic,
                                                        contentDescription = "Mic",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = "Matched Route",
                                                        fontSize = 16.sp, // Body
                                                        fontWeight = FontWeight.Medium,
                                                        fontFamily = FontFamily.SansSerif,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Text(
                                                    text = if (log.status == "SUCCESS") "SUCCESS" else "FAILED",
                                                    fontSize = 13.sp, // Labels
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (log.status == "SUCCESS") MaterialTheme.colorScheme.success else MaterialTheme.colorScheme.error
                                                )
                                            }

                                            Text(
                                                text = "Vocal Input: \"${log.command}\"",
                                                fontSize = 14.sp, // Caption
                                                fontFamily = FontFamily.SansSerif,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Matched trigger: '${log.matchedTrigger ?: "None"}'",
                                                fontSize = 14.sp, // Caption
                                                fontFamily = FontFamily.SansSerif,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Timestamp: ${log.timestamp}",
                                                fontSize = 13.sp, // Labels
                                                fontFamily = FontFamily.SansSerif,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAutoScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // States for Smart Auto suggestions
    val showsNightInstalled = remember { mutableStateOf(false) }
    val showsLensInstalled = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Suggested Routines",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp, // Section Title
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("smart_auto_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Aira Suggestions Engine",
                        fontSize = 18.sp, // Card Title
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Pre-configured routines prepared by the system analyzer for instant installation.",
                        fontSize = 14.sp, // Caption
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    // Suggestion 1: Night routine
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 64.dp)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Sleep Chain Routine",
                                    fontSize = 16.sp, // Body
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Trigger phrase: \"Good Night\"",
                                    fontSize = 14.sp, // Caption
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Tasks: Silent Sound Profile + Flashlight OFF",
                                    fontSize = 13.sp, // Labels
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val dao = AppDatabase.getDatabase(viewModel.getApplication()).voiceCommandDao()
                                        val acts = dao.getAllActions()
                                        val silId = acts.find { it.name.contains("Silent") }?.id ?: 7L
                                        val flOffId = acts.find { it.name.contains("Flashlight Off") }?.id ?: 2L
                                        viewModel.insertCommand(Command(
                                            triggerPhrase = "good night",
                                            actionIdsJson = "[$silId, $flOffId]",
                                            priority = 6
                                        ))
                                        showsNightInstalled.value = true
                                        viewModel.speakText("Installed Sleep Routine")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !showsNightInstalled.value,
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(
                                    text = if (showsNightInstalled.value) "Installed" else "Install",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Suggestion 2: Max sight
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 64.dp)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Vision Booster Routine",
                                    fontSize = 16.sp, // Body
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Trigger phrase: \"Boost\"",
                                    fontSize = 14.sp, // Caption
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Tasks: Flashlight ON + Maximized Screen Brightness",
                                    fontSize = 13.sp, // Labels
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val dao = AppDatabase.getDatabase(viewModel.getApplication()).voiceCommandDao()
                                        val acts = dao.getAllActions()
                                        val flOnId = acts.find { it.name.contains("Flashlight On") }?.id ?: 1L
                                        val bId = acts.find { it.name.contains("Set Brightness") }?.id ?: 10L
                                        viewModel.insertCommand(Command(
                                            triggerPhrase = "boost",
                                            actionIdsJson = "[$flOnId, $bId]",
                                            priority = 9
                                        ))
                                        showsLensInstalled.value = true
                                        viewModel.speakText("Installed Max Vision booster")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !showsLensInstalled.value,
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(
                                    text = if (showsLensInstalled.value) "Installed" else "Install",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActionsScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val allActions by viewModel.allActions.collectAsState()
    val allCommands by viewModel.allCommands.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Add Action Screen overlay/card expanded panel states
    var showAddActionPanel by remember { mutableStateOf(false) }
    var newActionName by remember { mutableStateOf("") }
    var newActionType by remember { mutableStateOf("SYSTEM_API") } // SYSTEM_API, INTENT, SHELL, DELAY
    var newActionParams by remember { mutableStateOf("{\"action\":\"flashlight_on\"}") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Actions Vault",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp, // Section Title
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("my_actions_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
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
                        Text(
                            text = "Actions Control",
                            fontSize = 18.sp, // Card Title
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = { showAddActionPanel = !showAddActionPanel },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (showAddActionPanel) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Toggle Add Action",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (showAddActionPanel) "Close" else "New Action",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Expansion panel for creating Actions
                    AnimatedVisibility(
                        visible = showAddActionPanel,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Create Action Sequence",
                                fontSize = 16.sp, // Body
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextField(
                                value = newActionName,
                                onValueChange = { newActionName = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Action Name", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) }
                            )

                            // Type dropdown toggle (simulated via horizontal row selections)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Action Type",
                                    fontSize = 13.sp, // Labels
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("SYSTEM_API", "INTENT", "SHELL", "DELAY").forEach { type ->
                                        val isSelected = newActionType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    newActionType = type
                                                    newActionParams = when (type) {
                                                        "SYSTEM_API" -> "{\"action\":\"flashlight_on\"}"
                                                        "INTENT" -> "{\"action\":\"open_camera\"}"
                                                        "SHELL" -> "{\"command\":\"ls -la\"}"
                                                        "DELAY" -> "{\"duration\":500}"
                                                        else -> "{}"
                                                    }
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val displayName = when(type) {
                                                "SYSTEM_API" -> "System"
                                                "INTENT" -> "Intent"
                                                "SHELL" -> "Shell"
                                                "DELAY" -> "Delay"
                                                else -> type
                                            }
                                            Text(
                                                text = displayName,
                                                fontSize = 13.sp, // Labels
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            TextField(
                                value = newActionParams,
                                onValueChange = { newActionParams = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Json Configuration Parameters", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) }
                            )

                            Button(
                                onClick = {
                                    if (newActionName.isNotEmpty()) {
                                        viewModel.insertAction(Action(
                                            name = newActionName,
                                            type = newActionType,
                                            paramsJson = newActionParams
                                        ))
                                        newActionName = ""
                                        showAddActionPanel = false
                                        viewModel.speakText("Action registered successfully")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Action", fontSize = 16.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    // List of registered Actions
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        allActions.forEach { action ->
                            val associatedCommand = allCommands.find { cmd ->
                                val actionIds = try {
                                    org.json.JSONArray(cmd.actionIdsJson).let { arr ->
                                        (0 until arr.length()).map { arr.getLong(it) }
                                    }
                                } catch (e: Exception) {
                                    cmd.actionIdsJson.replace("[", "").replace("]", "").split(",").mapNotNull { it.trim().toLongOrNull() }
                                }
                                actionIds.contains(action.id)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = associatedCommand != null) {
                                        associatedCommand?.let { cmd ->
                                            coroutineScope.launch {
                                                val m = VoiceCommandManager.getInstance(viewModel.getApplication())
                                                val phraseWithDefaultVal = cmd.triggerPhrase.replace("{number}", "50").replace("{text}", "boss")
                                                m.matchAndExecuteCommand(phraseWithDefaultVal, viewModel)
                                            }
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 64.dp)
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = action.name,
                                            fontSize = 16.sp, // Body
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Type: ${action.type} • Params: ${action.paramsJson}",
                                            fontSize = 14.sp, // Caption
                                            fontFamily = FontFamily.SansSerif,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (associatedCommand != null) {
                                            Text(
                                                text = "Voice Phrase: '${associatedCommand.triggerPhrase}'",
                                                fontSize = 14.sp, // Caption
                                                fontFamily = FontFamily.SansSerif,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (associatedCommand != null) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val m = VoiceCommandManager.getInstance(viewModel.getApplication())
                                                        val phraseWithDefaultVal = associatedCommand.triggerPhrase.replace("{number}", "50").replace("{text}", "boss")
                                                        m.matchAndExecuteCommand(phraseWithDefaultVal, viewModel)
                                                    }
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PlayArrow,
                                                    contentDescription = "Run",
                                                    tint = MaterialTheme.colorScheme.success,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteAction(action) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val allActions by viewModel.allActions.collectAsState()
    val allCommands by viewModel.allCommands.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var commandSearchQuery by remember { mutableStateOf("") }
    var conflictWarningMessage by remember { mutableStateOf<String?>(null) }
    
    // Add Command Screen overlay/card expanded panel states
    var showAddCommandPanel by remember { mutableStateOf(false) }
    var newTriggerPhrase by remember { mutableStateOf("") }
    var newCommandPriority by remember { mutableStateOf("5") }
    val newSelectedActionIds = remember { mutableStateListOf<Long>() }
    var newBatteryCondition by remember { mutableStateOf("NONE") } // NONE, LT_20, GT_80
    var newTimeCondition by remember { mutableStateOf("NONE") } // NONE, DAY, NIGHT

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vocal Trigger Registry",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp, // Section Title
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("voice_commands_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Live Voice Command Parser Card
            var voiceInputTestText by remember { mutableStateOf("") }
            var parseResultOutput by remember { mutableStateOf<String?>(null) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Voice Command Parser Tester",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Test voice commands for Wi-Fi toggling, screen brightness, setting alarms, sound modes, or flashlight.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif
                    )

                    OutlinedTextField(
                        value = voiceInputTestText,
                        onValueChange = { voiceInputTestText = it },
                        modifier = Modifier.fillMaxWidth().testTag("voice_command_input"),
                        placeholder = { Text("e.g. 'set brightness to 80%' or 'set alarm for 7:30 AM'", fontSize = 14.sp) },
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (voiceInputTestText.isNotBlank()) {
                                    val res = viewModel.parseAndExecuteVoiceCommand(voiceInputTestText)
                                    parseResultOutput = res
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("parse_and_execute_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Parse & Execute Action", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    // Sample Quick Preset Chips
                    val presets = listOf(
                        "turn on wifi",
                        "set brightness to 80%",
                        "set alarm for 7:30 AM",
                        "silent mode",
                        "lock screen",
                        "take screenshot",
                        "open notifications",
                        "open quick settings",
                        "open power menu",
                        "click on save",
                        "check device admin"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets.size) { index ->
                            val chip = presets[index]
                            SuggestionChip(
                                onClick = {
                                    voiceInputTestText = chip
                                    parseResultOutput = viewModel.parseAndExecuteVoiceCommand(chip)
                                },
                                label = { Text(chip, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("preset_chip_$index")
                            )
                        }
                    }

                    parseResultOutput?.let { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "PARSER EXECUTION OUTPUT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = result,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Accessibility & Device Policy Framework Integration Card
            val isAccessibilityActive = viewModel.isAccessibilityServiceConnected()
            val isDeviceAdminActive = viewModel.checkDeviceAdminActive()
            val context = LocalContext.current

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Accessibility & Device Policy Framework",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Automates deep Android system actions and security policies via natural language commands.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Accessibility Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Accessibility Service", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isAccessibilityActive) "Active & Connected" else "Disconnected",
                                fontSize = 12.sp,
                                color = if (isAccessibilityActive) colorResource(id = R.color.aira_success_light) else MaterialTheme.colorScheme.error
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("open_accessibility_settings_btn")
                        ) {
                            Text("Configure", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Device Policy Admin Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Device Policy Admin", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isDeviceAdminActive) "Active & Enforced" else "Inactive",
                                fontSize = 12.sp,
                                color = if (isDeviceAdminActive) colorResource(id = R.color.aira_success_light) else MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = {
                                val intent = viewModel.getDeviceAdminActivationIntent()
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("activate_device_admin_btn")
                        ) {
                            Text(if (isDeviceAdminActive) "Manage" else "Enable Admin", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text("Quick Accessibility & Policy Actions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    // Quick Action Buttons Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { parseResultOutput = viewModel.lockDeviceScreen() },
                                modifier = Modifier.weight(1f).testTag("quick_lock_screen_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Lock Screen", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { parseResultOutput = viewModel.parseAndExecuteVoiceCommand("take screenshot") },
                                modifier = Modifier.weight(1f).testTag("quick_screenshot_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Screenshot", fontSize = 12.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { parseResultOutput = viewModel.parseAndExecuteVoiceCommand("open notifications") },
                                modifier = Modifier.weight(1f).testTag("quick_notifications_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Notifications", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { parseResultOutput = viewModel.parseAndExecuteVoiceCommand("open power menu") },
                                modifier = Modifier.weight(1f).testTag("quick_power_menu_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Power Menu", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
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
                        Text(
                            text = "Triggers Engine",
                            fontSize = 18.sp, // Card Title
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = { showAddCommandPanel = !showAddCommandPanel },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (showAddCommandPanel) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Toggle Add Command",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                              )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (showAddCommandPanel) "Close" else "New Command",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Add custom command sub-panel
                    AnimatedVisibility(
                        visible = showAddCommandPanel,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Create Custom Voice Trigger",
                                fontSize = 16.sp, // Body
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextField(
                                value = newTriggerPhrase,
                                onValueChange = { newTriggerPhrase = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Trigger Vocal Phrase", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                placeholder = { Text("Use {number} or {text} slots...", fontSize = 14.sp, fontFamily = FontFamily.SansSerif) }
                            )

                            TextField(
                                value = newCommandPriority,
                                onValueChange = { newCommandPriority = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                label = { Text("Execution Priority (1 to 10)", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) }
                            )

                            // Actions checklist selection layout
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Select Target Actions to Bind",
                                    fontSize = 13.sp, // Labels
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    allActions.forEach { act ->
                                        val isChecked = newSelectedActionIds.contains(act.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable {
                                                    if (isChecked) newSelectedActionIds.remove(act.id)
                                                    else newSelectedActionIds.add(act.id)
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = {
                                                    if (isChecked) newSelectedActionIds.remove(act.id)
                                                    else newSelectedActionIds.add(act.id)
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = act.name + " [${act.type}]",
                                                fontSize = 14.sp, // Caption
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // Exceptional Feature 2: Conditional Chain Constraint options
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Execution Condition Locks",
                                    fontSize = 13.sp, // Labels
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // Battery condition row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Battery",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier.width(64.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    listOf("NONE", "LT_20", "GT_80").forEach { item ->
                                        val isSel = newBatteryCondition == item
                                        val disp = when(item) {
                                            "NONE" -> "Disabled"
                                            "LT_20" -> "Low (<20%)"
                                            "GT_80" -> "High (>80%)"
                                            else -> item
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSel) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { newBatteryCondition = item }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = disp,
                                                fontSize = 13.sp, // Labels
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Time condition row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TimeRange",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier.width(64.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    listOf("NONE", "DAY", "NIGHT").forEach { item ->
                                        val isSel = newTimeCondition == item
                                        val disp = when(item) {
                                            "NONE" -> "Disabled"
                                            "DAY" -> "Day"
                                            "NIGHT" -> "Night"
                                            else -> item
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSel) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { newTimeCondition = item }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = disp,
                                                fontSize = 13.sp, // Labels
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            conflictWarningMessage?.let { msg ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = "Trigger Conflict Alert", fontSize = 16.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                        Text(text = msg, fontSize = 14.sp, fontFamily = FontFamily.SansSerif, color = MaterialTheme.colorScheme.onSurface)
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Button(
                                                onClick = {
                                                    if (newTriggerPhrase.isNotEmpty() && newSelectedActionIds.isNotEmpty()) {
                                                        val conditionsObj = org.json.JSONObject()
                                                        if (newBatteryCondition == "LT_20") conditionsObj.put("batteryLt", 20)
                                                        if (newBatteryCondition == "GT_80") conditionsObj.put("batteryGt", 80)
                                                        if (newTimeCondition != "NONE") conditionsObj.put("timeRange", newTimeCondition)

                                                        val actionIdsJson = org.json.JSONArray(newSelectedActionIds.toList()).toString()

                                                        viewModel.insertCommand(Command(
                                                            triggerPhrase = newTriggerPhrase.lowercase().trim(),
                                                            priority = newCommandPriority.toIntOrNull() ?: 5,
                                                            actionIdsJson = actionIdsJson,
                                                            conditionsJson = if (conditionsObj.length() > 0) conditionsObj.toString() else ""
                                                        ))

                                                        newTriggerPhrase = ""
                                                        newSelectedActionIds.clear()
                                                        newBatteryCondition = "NONE"
                                                        newTimeCondition = "NONE"
                                                        showAddCommandPanel = false
                                                        conflictWarningMessage = null
                                                        viewModel.speakText("Speech path triggers updated")
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Override & Force Save", fontSize = 13.sp, fontFamily = FontFamily.SansSerif, color = MaterialTheme.colorScheme.onError)
                                            }
                                            TextButton(
                                                onClick = { conflictWarningMessage = null }
                                            ) {
                                                Text("Dismiss", fontSize = 13.sp, fontFamily = FontFamily.SansSerif, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (newTriggerPhrase.isNotEmpty() && newSelectedActionIds.isNotEmpty()) {
                                        val lowerNewPhrase = newTriggerPhrase.lowercase().trim()
                                        
                                        // Scan and find matching high-sim conflict >= 95%
                                        val conflictCmd = allCommands.find {
                                            calculateLevenshteinSimilarity(lowerNewPhrase, it.triggerPhrase.lowercase().trim()) > 0.95f
                                        }

                                        if (conflictCmd != null) {
                                            conflictWarningMessage = "This phrase is mathematically close to existing '${conflictCmd.triggerPhrase}'. Force save anyway?"
                                        } else {
                                            val conditionsObj = org.json.JSONObject()
                                            if (newBatteryCondition == "LT_20") conditionsObj.put("batteryLt", 20)
                                            if (newBatteryCondition == "GT_80") conditionsObj.put("batteryGt", 80)
                                            if (newTimeCondition != "NONE") conditionsObj.put("timeRange", newTimeCondition)

                                            val actionIdsJson = org.json.JSONArray(newSelectedActionIds.toList()).toString()

                                            viewModel.insertCommand(Command(
                                                triggerPhrase = newTriggerPhrase.lowercase().trim(),
                                                priority = newCommandPriority.toIntOrNull() ?: 5,
                                                actionIdsJson = actionIdsJson,
                                                conditionsJson = if (conditionsObj.length() > 0) conditionsObj.toString() else ""
                                            ))

                                            // Reset fields
                                            newTriggerPhrase = ""
                                            newSelectedActionIds.clear()
                                            newBatteryCondition = "NONE"
                                            newTimeCondition = "NONE"
                                            showAddCommandPanel = false
                                            viewModel.speakText("Speech path triggers updated")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Trigger Command", fontSize = 16.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    // Floating Search input to filter trigger phrases quickly
                    TextField(
                        value = commandSearchQuery,
                        onValueChange = { commandSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Search Vocal Triggers...", fontSize = 16.sp, fontFamily = FontFamily.SansSerif, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Outlined.Search, "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
                    )

                    // Render matching voice commands
                    val filteredCommands = allCommands.filter {
                        it.triggerPhrase.lowercase().contains(commandSearchQuery.lowercase())
                    }

                    if (filteredCommands.isEmpty()) {
                        Text(
                            text = "No triggers matching parameters found",
                            fontSize = 14.sp, // Caption
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            filteredCommands.forEach { cmd ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 64.dp)
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = cmd.triggerPhrase,
                                                    fontSize = 16.sp, // Body
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = FontFamily.SansSerif,
                                                    color = MaterialTheme.colorScheme.primary
                                                )

                                                // Badge priority
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                        .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "P${cmd.priority}",
                                                        fontSize = 11.sp, // Labels / Small indicators
                                                        fontFamily = FontFamily.SansSerif,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                // Conditions constraint badge
                                                if (cmd.conditionsJson.isNotEmpty()) {
                                                    Icon(Icons.Default.Lock, "Condition Locked", tint = MaterialTheme.colorScheme.warning, modifier = Modifier.size(14.dp))
                                                }
                                            }

                                            Text(
                                                text = "Action sequence: " + cmd.actionIdsJson,
                                                fontSize = 14.sp, // Caption
                                                fontFamily = FontFamily.SansSerif,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (cmd.conditionsJson.isNotEmpty()) {
                                                Text(
                                                    text = "Conditions constraints: " + cmd.conditionsJson,
                                                    fontSize = 13.sp, // Labels
                                                    fontFamily = FontFamily.SansSerif,
                                                    color = MaterialTheme.colorScheme.warning.copy(alpha = 0.7f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Text(
                                                text = "Used ${cmd.useCount} times",
                                                fontSize = 13.sp, // Labels
                                                fontFamily = FontFamily.SansSerif,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // TEST COMMAND BUTTON
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val m = VoiceCommandManager.getInstance(viewModel.getApplication())
                                                        val phraseWithDefaultVal = cmd.triggerPhrase.replace("{number}", "50").replace("{text}", "boss")
                                                        m.matchAndExecuteCommand(phraseWithDefaultVal, viewModel)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .testTag("test_cmd_${cmd.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.PlayArrow,
                                                    contentDescription = "Test Execute",
                                                    tint = MaterialTheme.colorScheme.success,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            // DELETE COMMAND
                                            IconButton(
                                                onClick = { viewModel.deleteCommand(cmd) },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .testTag("delete_cmd_${cmd.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Trigger",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateLevenshteinSimilarity(x: String, y: String): Float {
    val m = x.length
    val n = y.length
    if (m == 0 && n == 0) return 1.0f
    if (m == 0 || n == 0) return 0.0f
    
    val dp = IntArray(n + 1) { it }
    for (i in 1..m) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..n) {
            val temp = dp[j]
            if (x[i - 1] == y[j - 1]) {
                dp[j] = prev
            } else {
                dp[j] = minOf(dp[j - 1], dp[j], prev) + 1
            }
            prev = temp
        }
    }
    val distance = dp[n]
    val maxLen = maxOf(m, n)
    return 1.0f - (distance.toFloat() / maxLen)
}

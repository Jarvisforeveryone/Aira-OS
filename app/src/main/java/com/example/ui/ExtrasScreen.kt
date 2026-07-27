package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.colorResource
import com.example.R

@Composable
fun ExtrasScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val weatherData by viewModel.weatherText.collectAsState()
    val newsArticles by viewModel.newsFeed.collectAsState()
    val isOfflineBrain by viewModel.isOfflineBrain.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp) // Outer Screen Padding: 24dp
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(28.dp) // Between Sections: 28dp
    ) {
        // Large Screen Title
        Text(
            text = "Timeline",
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp, // Large Screen Title: 34sp SemiBold
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.SansSerif
        )

        if (isOfflineBrain) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("extras_offline_mode_banner")
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
                                .background(colorResource(id = R.color.aira_success_light), CircleShape)
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

        // CARD 1: ENVIRONMENTAL METRIC CORES (WEATHER)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Premium dark surface
            shape = RoundedCornerShape(22.dp), // Corner Radius: 22dp
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Very soft Material elevation
        ) {
            Column(
                modifier = Modifier.padding(20.dp), // Inside Cards: 20dp
                verticalArrangement = Arrangement.spacedBy(16.dp) // Between Elements: 16dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weather",
                        fontSize = 18.sp, // Card Title: 18sp Medium
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                    )
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Weather Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp) // Icons: 24dp
                    )
                }

                Text(
                    text = "Updating environmental metrics...",
                    fontSize = 14.sp, // Caption: 14sp
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontFamily = FontFamily.SansSerif
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = weatherData,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                        fontSize = 16.sp, // Body: 16sp Regular
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag("weather_display_txt")
                    )
                }
            }
        }

        var selectedArticleForDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

        if (selectedArticleForDialog != null) {
            val (title, detail) = selectedArticleForDialog!!
            AlertDialog(
                onDismissRequest = { selectedArticleForDialog = null },
                title = {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Broadcast Transmission • Live Aira Feed",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = detail,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.speakText(detail)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Read Aloud", fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedArticleForDialog = null }) {
                        Text("Close", fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp)
            )
        }

        // CARD 2: DIGITAL NEWS TRANSMISSION CORE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Premium dark surface
            shape = RoundedCornerShape(22.dp), // Corner Radius: 22dp
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Very soft Material elevation
        ) {
            Column(
                modifier = Modifier.padding(20.dp), // Inside Cards: 20dp
                verticalArrangement = Arrangement.spacedBy(16.dp) // Between Elements: 16dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "News Feed",
                        fontSize = 18.sp, // Card Title: 18sp Medium
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                    )
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = "News Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp) // Icons: 24dp
                    )
                }

                Text(
                    text = "Latest Intel & Updates (Tap card to view full article details)",
                    fontSize = 14.sp, // Caption: 14sp
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontFamily = FontFamily.SansSerif
                )

                if (newsArticles.isEmpty()) {
                    Text(
                        text = "No updates available at this moment.",
                        fontSize = 16.sp, // Body: 16sp Regular
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp) // Comfortable spacing
                    ) {
                        newsArticles.forEachIndexed { i, article ->
                            val extendedDetail = when (i) {
                                0 -> "AI Advances in local edge reasoning platforms enable on-device LLM inference via Llama 3.2 1B/3B without cloud dependencies, providing zero-latency privacy-focused intelligence directly on Android."
                                1 -> "Android 16 dynamic color customization rolls out system-wide with expanded Material 3 palettes, adaptive contrast ratios, and seamless dark/light mode transitions."
                                else -> "Global climate systems display warming trends this season. Multi-spectral satellite telemetry records ocean surface temperature variations across tropical regions."
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 64.dp) // Height: 64dp minimum
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                    .clickable {
                                        selectedArticleForDialog = Pair("Broadcast Update #${i + 1}", extendedDetail)
                                    }
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Broadcast Update #${i + 1}",
                                    fontSize = 13.sp, // Labels: 13sp Medium
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = article,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                    fontSize = 16.sp, // Body: 16sp Regular
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.testTag("news_article_$i")
                                )
                            }
                        }
                    }
                }
            }
        }

        // CARD 3: AIRA INTELLIGENCE ALERTS & BROADCASTER
        var selectedCategory by remember { mutableStateOf("Breaking") }
        val categories = listOf("Breaking", "AI Upgrades", "Weather Emergency", "Daily Agenda")
        val context = androidx.compose.ui.platform.LocalContext.current

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
                        text = "Aira Alert Simulator",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                    )
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alert Config",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Broadcast live intelligence alerts directly to your device notifications tray to test the high-tech voice alert systems.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 18.sp
                )

                // Horizontal Category Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        val isCatSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCatSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isCatSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (isCatSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val (title, content, tts) = when (selectedCategory) {
                            "Breaking" -> Triple(
                                "Quantum Break-through",
                                "Quantum computing grid registers room-temperature coherence milestone.",
                                "Aira alert: Room-temperature quantum coherence has been achieved in the primary grid."
                            )
                            "AI Upgrades" -> Triple(
                                "Neural Models Upgraded",
                                "Aira synthetic intelligence models successfully upgraded to standard v3.5 architecture.",
                                "Intelligence upgrade complete. Neural models are now operating at optimal speed."
                            )
                            "Weather Emergency" -> Triple(
                                "Severe Auroral Storm",
                                "Kp-index reaches 8.7. High-latitude communication degradation predicted. Auroral displays visible tonight.",
                                "Caution: Severe geomagnetic auroral storm is active. Communication interference possible."
                            )
                            else -> Triple(
                                "Daily Intel Briefing",
                                "Your personalized daily intelligence digest is ready. Review schedule, tasks, and memory logs.",
                                "Stand by: Your personalized daily agenda briefing is ready for review."
                            )
                        }

                        // 1. Post Device Push Notification
                        com.example.service.AiraNotificationManager.showNewsAlertNotification(
                            context,
                            title,
                            content,
                            selectedCategory
                        )

                        // 2. Play Dynamic Voice Prompt
                        viewModel.speakText(tts)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("broadcast_news_alert_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notify",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trigger $selectedCategory Alert",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun ExtrasScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val weatherData by viewModel.weatherText.collectAsState()
    val newsArticles by viewModel.newsFeed.collectAsState()

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
                    text = "Latest Intel & Updates",
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 64.dp) // Height: 64dp minimum
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Broadcast Update",
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
                    text = "Broadcast mock intelligence updates directly to your device notifications tray to test the high-tech alert systems.",
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

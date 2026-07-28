package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val newsItems by viewModel.newsItems.collectAsState()
    val newsArticles by viewModel.newsFeed.collectAsState()
    val isNewsLoading by viewModel.isNewsLoading.collectAsState()
    val newsError by viewModel.newsError.collectAsState()
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Llama 3.2 • Amy ONNX • Vosk STT",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
                        tint = Color(0xFF2563EB),
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
                        text = "Google News Feed",
                        fontSize = 18.sp, // Card Title: 18sp Medium
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.fetchNews() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh News",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Newspaper,
                            contentDescription = "News Icon",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp) // Icons: 24dp
                        )
                    }
                }

                Text(
                    text = "Live headlines & updates from Google News RSS (Tap card to read full story)",
                    fontSize = 14.sp, // Caption: 14sp
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontFamily = FontFamily.SansSerif
                )

                if (isNewsLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Fetching Google News RSS feed...",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else if (newsError != null && newsItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = newsError ?: "Failed to load Google News RSS feed",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = { viewModel.fetchNews() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry Loading RSS Feed", fontSize = 13.sp, fontFamily = FontFamily.SansSerif)
                        }
                    }
                } else if (newsItems.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        newsItems.forEachIndexed { i, item ->
                            val metaLabel = if (item.source.isNotEmpty()) {
                                if (item.pubDate.isNotEmpty()) "${item.source} • ${item.pubDate}" else item.source
                            } else {
                                item.pubDate.ifEmpty { "Google News" }
                            }
                            val dialogDetail = if (item.description.isNotBlank()) {
                                "${item.title}\n\n${item.description}"
                            } else {
                                item.title
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                    .clickable {
                                        selectedArticleForDialog = Pair(item.title, dialogDetail)
                                    }
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = metaLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    fontFamily = FontFamily.SansSerif,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = item.title,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.testTag("news_article_$i")
                                )
                                if (item.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.description,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else if (newsArticles.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        newsArticles.forEachIndexed { i, article ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .defaultMinSize(minHeight = 64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                    .clickable {
                                        selectedArticleForDialog = Pair("Google News Item #${i + 1}", article)
                                    }
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Google News Update #${i + 1}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = article,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    modifier = Modifier.testTag("news_article_$i")
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No news updates available at this moment.",
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        tint = Color(0xFF2563EB),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                                color = if (isCatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

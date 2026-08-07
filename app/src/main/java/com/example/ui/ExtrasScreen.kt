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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Cloud
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.components.AiraCard
import com.example.ui.components.AiraBadge
import com.example.ui.theme.bounceClick

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
                            text = "Private On-Device AI • Voice Ready",
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

        var showWeatherDetailsDialog by remember { mutableStateOf(false) }
        var searchCityQuery by remember { mutableStateOf("") }
        val openMeteoData by viewModel.openMeteoWeather.collectAsState()

        // CARD 1: ENVIRONMENTAL METRIC CORES (WEATHER)
        AiraCard(
            onClick = { showWeatherDetailsDialog = true },
            title = "Weather",
            subtitle = "Checking local weather...",
            icon = Icons.Outlined.Cloud,
            headerTrailing = {
                AiraBadge(text = "LIVE WEATHER", badgeColor = MaterialTheme.colorScheme.primaryContainer, textColor = MaterialTheme.colorScheme.primary)
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Text(
                    text = weatherData,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("weather_display_txt")
                )
            }
        }

        if (showWeatherDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showWeatherDetailsDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cloud,
                            contentDescription = "Cloud Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Open-Meteo Weather Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchCityQuery,
                            onValueChange = { searchCityQuery = it },
                            placeholder = { Text("Search city (e.g. London, Tokyo)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = {
                                        if (searchCityQuery.isNotBlank()) {
                                            viewModel.searchCityWeather(searchCityQuery)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search"
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.refreshWeather()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Use GPS"
                                        )
                                    }
                                }
                            }
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                openMeteoData?.let { data ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (data.country.isNotEmpty()) "${data.locationName}, ${data.country}" else data.locationName,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (data.isGpsLocation) {
                                                Text(
                                                    text = "Exact GPS Location Active",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${data.temperatureC.toInt()}°C",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Text("Condition: ${data.conditionDescription}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Wind Speed: ${data.windSpeedKmH} km/h (Deg: ${data.windDirectionDeg}°)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Coordinates: ${data.latitude}, ${data.longitude}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Day/Night: ${if (data.isDaytime) "Daytime ☀️" else "Nighttime 🌙"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } ?: Text(
                                    text = weatherData,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "Powered by Open-Meteo API (Free & Open)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showWeatherDetailsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        var selectedArticleForDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

        val currentArticle = selectedArticleForDialog
        if (currentArticle != null) {
            val (title, detail) = currentArticle
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

        // CARD 2: DIGITAL NEWS TRANSMISSION CORE (WITH CATEGORIES & IMAGES)
        val newsCategories = listOf("Education", "Finance", "Technology", "Sports", "Health", "Entertainment")
        val selectedCategoryName by viewModel.selectedNewsCategory.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Google News Feed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.fetchNews(selectedCategoryName) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh News",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Newspaper,
                            contentDescription = "News Icon",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 1. CATEGORIES: Horizontal scroll chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allCategories = listOf("All") + newsCategories
                    allCategories.forEach { cat ->
                        val isSelected = cat.equals(selectedCategoryName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.fetchNews(cat) },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

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
                            text = "Fetching $selectedCategoryName news...",
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
                            text = newsError ?: "Failed to load Google News feed",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = { viewModel.fetchNews(selectedCategoryName) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry Loading Feed", fontSize = 13.sp, fontFamily = FontFamily.SansSerif)
                        }
                    }
                } else if (newsItems.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                    .clickable {
                                        selectedArticleForDialog = Pair(item.title, dialogDetail)
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                            .data(item.imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Article image",
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Newspaper,
                                            contentDescription = "News placeholder",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = metaLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        fontFamily = FontFamily.SansSerif,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.title,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.SansSerif,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.testTag("news_article_$i")
                                    )
                                }
                            }
                        }
                    }
                } else if (newsArticles.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        newsArticles.forEachIndexed { i, article ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                                    .clickable {
                                        selectedArticleForDialog = Pair("Google News Update #${i + 1}", article)
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Newspaper,
                                        contentDescription = "News icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Google News Update #${i + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.SansSerif
                                    )
                                    Text(
                                        text = article,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.testTag("news_article_$i")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No news updates available at this moment.",
                        fontSize = 14.sp,
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
                    text = "Send sample notification alerts to your phone to test AIRA's alert sounds and voice reminders.",
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

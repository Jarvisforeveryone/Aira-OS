package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.LoadingInlineIndicator
import com.example.ui.components.SkeletonList
import com.example.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasScreen(
    viewModel: AiraViewModel,
    modifier: Modifier = Modifier
) {
    val weatherData by viewModel.weatherText.collectAsState()
    val openMeteoData by viewModel.openMeteoWeather.collectAsState()
    val newsItems by viewModel.newsItems.collectAsState()
    val newsArticles by viewModel.newsFeed.collectAsState()
    val isNewsLoading by viewModel.isNewsLoading.collectAsState()
    val newsError by viewModel.newsError.collectAsState()
    val selectedCategoryName by viewModel.selectedNewsCategory.collectAsState()

    var showWeatherDetailsDialog by remember { mutableStateOf(false) }
    var searchCityQuery by remember { mutableStateOf("") }
    var selectedArticleForDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAllNews by remember { mutableStateOf(false) }

    val dismissedNewsTitles = remember { mutableStateListOf<String>() }
    var isPullRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    val onRefreshAll: () -> Unit = {
        coroutineScope.launch {
            isPullRefreshing = true
            viewModel.refreshWeather()
            viewModel.fetchNews(selectedCategoryName)
            delay(1000)
            isPullRefreshing = false
        }
    }

    // ROOT CONTAINER WITH PULL-TO-REFRESH
    PullToRefreshBox(
        isRefreshing = isPullRefreshing || isNewsLoading,
        onRefresh = onRefreshAll,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = Dimens.GapLarge, vertical = Dimens.GapLarge)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(Dimens.GapLarge)
            ) {
                // CARD 1: WEATHER
                val cityText = openMeteoData?.let {
                    if (it.country.isNotEmpty()) "${it.locationName}, ${it.country}" else it.locationName
                } ?: weatherData.split(":").getOrNull(0)?.trim()?.ifEmpty { "Current Location" } ?: "Current Location"

                val tempText = openMeteoData?.let {
                    "${it.temperatureC.toInt()}°C"
                } ?: Regex("(-?\\d+°[CF])").find(weatherData)?.value ?: "20°C"

                val conditionText = openMeteoData?.conditionDescription
                    ?: weatherData.split(":").getOrNull(1)?.split(",")?.getOrNull(1)?.trim()
                    ?: "Partly Cloudy"

                val detailsText = openMeteoData?.let {
                    "Wind: ${it.windSpeedKmH} km/h • Coordinates: ${it.latitude}, ${it.longitude}"
                } ?: weatherData

                val statusText = if (openMeteoData?.isGpsLocation == true) "Exact GPS Active" else "Updating weather..."

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWeatherDetailsDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(Dimens.CornerRadiusExtraLarge),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationLow)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.GapLarge)
                    ) {
                        // 1. Row[Icon + Title]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = "Weather",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(Dimens.IconStandard)
                            )
                            Spacer(modifier = Modifier.width(Dimens.GapSmall))
                            Text(
                                text = "Weather",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. Text: [actual city]
                        Spacer(modifier = Modifier.height(Dimens.GapMedium))
                        Text(
                            text = cityText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 3. Text: [actual temperature]
                        Spacer(modifier = Modifier.height(Dimens.GapTiny))
                        Text(
                            text = tempText,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 4. Text: [actual condition]
                        Spacer(modifier = Modifier.height(Dimens.GapSmall))
                        Text(
                            text = conditionText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 5. Text: [actual details]
                        Spacer(modifier = Modifier.height(Dimens.GapTiny))
                        Text(
                            text = detailsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 6. Row[Dot + Status Text]
                        Spacer(modifier = Modifier.height(Dimens.GapMedium))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Dimens.GapSmall)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(Dimens.GapSmall))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // CARD 2: NEWS
                val newsCategories = listOf("Education", "Finance", "Technology", "Sports", "Health", "Entertainment")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(Dimens.CornerRadiusExtraLarge),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationLow)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.GapLarge),
                        verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                    ) {
                        // 1. Row[Icon + Title + Refresh Button]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Newspaper,
                                    contentDescription = "News",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(Dimens.IconStandard)
                                )
                                Spacer(modifier = Modifier.width(Dimens.GapSmall))
                                Text(
                                    text = "News",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (dismissedNewsTitles.isNotEmpty()) {
                                    TextButton(
                                        onClick = { dismissedNewsTitles.clear() }
                                    ) {
                                        Text(
                                            text = "Restore All",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        dismissedNewsTitles.clear()
                                        viewModel.fetchNews(selectedCategoryName)
                                    },
                                    modifier = Modifier.size(Dimens.MinTouchTarget)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh News",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(Dimens.IconMedium)
                                    )
                                }
                            }
                        }

                        // 2. Category Chips: Horizontal scroll
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny),
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
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                                    modifier = Modifier.padding(horizontal = Dimens.GapTiny)
                                )
                            }
                        }

                        // 3. News List
                        if (isNewsLoading) {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)) {
                                LoadingInlineIndicator(message = "Fetching $selectedCategoryName news...")
                                SkeletonList(count = 2)
                            }
                        } else if (newsError != null && newsItems.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimens.GapMedium),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                            ) {
                                Text(
                                    text = newsError ?: "Failed to load Google News feed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MaterialTheme.colorScheme.error
                                )
                                OutlinedButton(
                                    onClick = { viewModel.fetchNews(selectedCategoryName) },
                                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                                ) {
                                    Text("Retry Loading Feed", style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.SansSerif)
                                }
                            }
                        } else if (newsItems.isNotEmpty()) {
                            val availableNews = newsItems.filterNot { item -> dismissedNewsTitles.contains(item.title) }
                            val displayedNews = if (showAllNews) availableNews else availableNews.take(4)

                            Column(
                                verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                            ) {
                                if (availableNews.isEmpty()) {
                                    Text(
                                        text = "All news headlines dismissed. Tap refresh or Restore All to reload.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = Dimens.GapMedium)
                                    )
                                } else {
                                    displayedNews.forEachIndexed { i, item ->
                                        val metaLabel = if (item.source.isNotEmpty()) {
                                            if (item.pubDate.isNotEmpty()) "${item.source} • ${item.pubDate}" else item.source
                                        } else {
                                            item.pubDate.ifEmpty { "Google News" }
                                        }
                                        val speechText = if (item.description.isNotBlank()) {
                                            "${item.title}. ${item.description}"
                                        } else {
                                            item.title
                                        }
                                        val dialogDetail = speechText

                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = { dismissValue ->
                                                if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                    dismissedNewsTitles.add(item.title)
                                                    true
                                                } else false
                                            }
                                        )

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            backgroundContent = {
                                                val color = when (dismissState.dismissDirection) {
                                                    SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                                    else -> Color.Transparent
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                                                        .background(color)
                                                        .padding(horizontal = Dimens.GapLarge),
                                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Dismiss Headline",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                    .clickable {
                                                        selectedArticleForDialog = Pair(item.title, dialogDetail)
                                                    }
                                                    .padding(Dimens.GapMedium),
                                                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Left Column content
                                                Column(
                                                    modifier = Modifier.weight(0.68f),
                                                    verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
                                                ) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.testTag("news_article_$i")
                                                    )
                                                    if (item.description.isNotBlank()) {
                                                        Text(
                                                            text = item.description,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = metaLabel,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }

                                                // Play Icon for Text-To-Speech
                                                IconButton(
                                                    onClick = {
                                                        viewModel.speakText(speechText)
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Read headline aloud",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }

                                                // Right Image
                                                Box(
                                                    modifier = Modifier
                                                        .size(72.dp)
                                                        .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (item.imageUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(LocalContext.current)
                                                                .data(item.imageUrl)
                                                                .crossfade(true)
                                                                .build(),
                                                            contentDescription = "Article image",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Newspaper,
                                                            contentDescription = "News placeholder",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(Dimens.IconStandard)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. VIEW MORE BUTTON
                                if (availableNews.size > 4) {
                                    TextButton(
                                        onClick = { showAllNews = !showAllNews },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (showAllNews) "Show Less" else "View More (${availableNews.size - 4} more)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else if (newsArticles.isNotEmpty()) {
                            val availableArticles = newsArticles.filterNot { article -> dismissedNewsTitles.contains(article) }
                            val displayedArticles = if (showAllNews) availableArticles else availableArticles.take(4)

                            Column(
                                verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                            ) {
                                if (availableArticles.isEmpty()) {
                                    Text(
                                        text = "All news headlines dismissed. Tap refresh to reload.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = Dimens.GapMedium)
                                    )
                                } else {
                                    displayedArticles.forEachIndexed { i, article ->
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            confirmValueChange = { dismissValue ->
                                                if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                    dismissedNewsTitles.add(article)
                                                    true
                                                } else false
                                            }
                                        )

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            backgroundContent = {
                                                val color = when (dismissState.dismissDirection) {
                                                    SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                                    else -> Color.Transparent
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                                                        .background(color)
                                                        .padding(horizontal = Dimens.GapLarge),
                                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Dismiss Headline",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                    .clickable {
                                                        selectedArticleForDialog = Pair("Google News Update #${i + 1}", article)
                                                    }
                                                    .padding(Dimens.GapMedium),
                                                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(0.68f),
                                                    verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
                                                ) {
                                                    Text(
                                                        text = "Google News Update #${i + 1}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = article,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.testTag("news_article_$i")
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.speakText(article)
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Read headline aloud",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(72.dp)
                                                        .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Newspaper,
                                                        contentDescription = "News icon",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(Dimens.IconStandard)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (availableArticles.size > 4) {
                                    TextButton(
                                        onClick = { showAllNews = !showAllNews },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (showAllNews) "Show Less" else "View More",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No news updates available at this moment.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // CARD 3: FINANCE (FINANCE NEWS)
                val financeNewsList = remember(newsItems) {
                    val keywords = listOf("finance", "market", "stock", "bank", "economy", "dollar", "rate", "business", "trade", "company", "inflation")
                    val filtered = newsItems.filter { item ->
                        val combined = "${item.title} ${item.description} ${item.source}".lowercase()
                        keywords.any { combined.contains(it) }
                    }
                    if (filtered.isNotEmpty()) filtered.take(4) else newsItems.take(4)
                }

                val defaultFinanceHeadlines = listOf(
                    Pair("Global Markets Stabilize as Inflation Slows", "Central banks report favorable economic trends with core inflation metrics trending downwards globally."),
                    Pair("Enterprise Tech Sector Drives Q3 Revenue Growth", "Leading software and cloud infrastructure providers record strong operational gains this quarter."),
                    Pair("Global Energy Sector Expands Renewable Infrastructure", "Capital allocation toward solar and wind storage technologies reaches record highs."),
                    Pair("Consumer Confidence Index Shows Steady Recovery", "Latest economic indicators point to resilient retail demand and stabilized job market figures.")
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(Dimens.CornerRadiusExtraLarge),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationLow)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.GapLarge),
                        verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                    ) {
                        // 1. Row[Icon + Title]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Finance",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Dimens.IconStandard)
                            )
                            Spacer(modifier = Modifier.width(Dimens.GapSmall))
                            Text(
                                text = "Finance News",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. Finance News Items
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)
                        ) {
                            if (financeNewsList.isNotEmpty()) {
                                financeNewsList.forEachIndexed { i, item ->
                                    val metaLabel = if (item.source.isNotEmpty()) {
                                        if (item.pubDate.isNotEmpty()) "${item.source} • ${item.pubDate}" else item.source
                                    } else {
                                        "Markets & Business"
                                    }
                                    val speechText = if (item.description.isNotBlank()) {
                                        "${item.title}. ${item.description}"
                                    } else {
                                        item.title
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .clickable {
                                                selectedArticleForDialog = Pair(item.title, speechText)
                                            }
                                            .padding(Dimens.GapMedium),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(0.68f),
                                            verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.testTag("finance_news_$i")
                                            )
                                            if (item.description.isNotBlank()) {
                                                Text(
                                                    text = item.description,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = metaLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.speakText(speechText)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Read finance news aloud",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item.imageUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(item.imageUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "Finance news image",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = "Finance placeholder",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(Dimens.IconStandard)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                defaultFinanceHeadlines.forEachIndexed { i, (headline, summary) ->
                                    val speechText = "$headline. $summary"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .clickable {
                                                selectedArticleForDialog = Pair(headline, speechText)
                                            }
                                            .padding(Dimens.GapMedium),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(0.68f),
                                            verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)
                                        ) {
                                            Text(
                                                text = headline,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.testTag("finance_news_$i")
                                            )
                                            Text(
                                                text = summary,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Financial Markets • Today",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.speakText(speechText)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Read finance news aloud",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = "Finance icon",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(Dimens.IconStandard)
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

    // DIALOGS PRESERVED
    if (showWeatherDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showWeatherDetailsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = "Cloud Icon",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(Dimens.IconStandard)
                    )
                    Text(
                        text = "Open-Meteo Weather Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchCityQuery,
                        onValueChange = { searchCityQuery = it },
                        placeholder = { Text("Search city (e.g. London, Tokyo)", style = MaterialTheme.typography.bodyMedium) },
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
                        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.GapMedium),
                            verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)
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
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (data.isGpsLocation) {
                                            Text(
                                                text = "Exact GPS Location Active",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${data.temperatureC.toInt()}°C",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Text("Condition: ${data.conditionDescription}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                Text("Wind Speed: ${data.windSpeedKmH} km/h (Deg: ${data.windDirectionDeg}°)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                Text("Coordinates: ${data.latitude}, ${data.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Day/Night: ${if (data.isDaytime) "Daytime ☀️" else "Nighttime 🌙"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } ?: Text(
                                text = weatherData,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "Powered by Open-Meteo API (Free & Open)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWeatherDetailsDialog = false }) {
                    Text("Close", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    val currentArticle = selectedArticleForDialog
    if (currentArticle != null) {
        val (title, detail) = currentArticle
        AlertDialog(
            onDismissRequest = { selectedArticleForDialog = null },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapMedium)) {
                    Text(
                        text = "Broadcast Transmission • Live Aira Feed",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.speakText(detail)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                ) {
                    Text("Read Aloud", fontFamily = FontFamily.SansSerif, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedArticleForDialog = null }) {
                    Text("Close", fontFamily = FontFamily.SansSerif, style = MaterialTheme.typography.labelLarge)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(Dimens.CornerRadiusExtraLarge)
        )
    }
}

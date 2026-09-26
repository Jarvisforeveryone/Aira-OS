package com.example.ui

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Memory
import com.example.ui.theme.Dimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemoryBankScreen(
    viewModel: AiraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val memories by viewModel.memories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var addFactText by remember { mutableStateOf("") }
    var addCategory by remember { mutableStateOf("Personal") }
    var addIsImportant by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<Memory?>(null) }
    var editFactText by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Personal") }
    var editIsImportant by remember { mutableStateOf(false) }

    var showClearAllConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(editingMemory) {
        editingMemory?.let {
            editFactText = it.factText
            editCategory = it.category
            editIsImportant = it.isImportant
        }
    }

    val categories = listOf("All", "Personal", "Work", "Tasks", "Reminders", "Preferences")

    val filteredMemories = remember(memories, selectedCategory, searchQuery) {
        memories.filter { mem ->
            val matchesCat = if (selectedCategory == "All") true else mem.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = if (searchQuery.isBlank()) true else mem.factText.contains(searchQuery.trim(), ignoreCase = true)
            matchesCat && matchesQuery
        }.sortedWith(compareByDescending<Memory> { it.isImportant }.thenByDescending { it.createdAt })
    }

    val totalMemoriesCount = memories.size
    val importantMemoriesCount = memories.count { it.isImportant }
    val uniqueCategoriesCount = memories.map { it.category }.distinct().size

    // Add Memory Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.add_memory_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapLarge)) {
                    OutlinedTextField(
                        value = addFactText,
                        onValueChange = { addFactText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_memory_input"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                        placeholder = { Text("e.g. Likes black coffee with double shot", style = MaterialTheme.typography.bodyMedium) },
                        minLines = 2,
                        maxLines = 4
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                        Text(
                            text = stringResource(R.string.category_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                            items(listOf("Personal", "Work", "Tasks", "Reminders", "Preferences")) { cat ->
                                FilterChip(
                                    selected = addCategory == cat,
                                    onClick = { addCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.mark_important),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = addIsImportant,
                            onCheckedChange = { addIsImportant = it },
                            modifier = Modifier.testTag("add_memory_important_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addFactText.isNotBlank()) {
                            viewModel.addMemoryManual(addFactText, addCategory, addIsImportant)
                            addFactText = ""
                            addIsImportant = false
                            showAddDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Fact saved to Memory Bank ✅")
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_memory_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    modifier = Modifier.testTag("cancel_add_memory_btn")
                ) {
                    Text(stringResource(R.string.cancel), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Edit Memory Dialog
    val currentMemory = editingMemory
    if (showEditDialog && currentMemory != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.edit_memory_title),
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editFactText,
                        onValueChange = { editFactText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_memory_input"),
                        textStyle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text(stringResource(R.string.memory_fact_label), fontSize = 14.sp) },
                        minLines = 2,
                        maxLines = 4
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.category_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Personal", "Work", "Tasks", "Reminders", "Preferences")) { cat ->
                                FilterChip(
                                    selected = editCategory == cat,
                                    onClick = { editCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp, fontFamily = FontFamily.SansSerif) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.mark_important),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = editIsImportant,
                            onCheckedChange = { editIsImportant = it },
                            modifier = Modifier.testTag("edit_memory_important_switch")
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Origin Source:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentMemory.source.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editFactText.isNotBlank()) {
                            viewModel.updateMemory(
                                id = currentMemory.id,
                                factText = editFactText.trim(),
                                source = currentMemory.source,
                                createdAt = currentMemory.createdAt,
                                category = editCategory,
                                isImportant = editIsImportant
                            )
                            showEditDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Fact updated in Memory Bank ✅")
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_edit_memory_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.deleteMemory(currentMemory.id)
                            showEditDialog = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Deleted from Memory Bank")
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("dialog_delete_memory_btn")
                    ) {
                        Text(stringResource(R.string.delete), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                    }
                    TextButton(
                        onClick = { showEditDialog = false },
                        modifier = Modifier.testTag("dialog_cancel_memory_btn")
                    ) {
                        Text(stringResource(R.string.cancel), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(
                    text = stringResource(R.string.clear_all_memories_confirm_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.clear_all_memories_confirm_desc),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearMemories()
                        showClearAllConfirm = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("All memories cleared")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_clear_all_btn")
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.memory_settings_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("memory_bank_title")
                        )
                        Text(
                            text = stringResource(R.string.memory_bank_subtitle),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("memory_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_memory_fab_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Memory",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats & Engine Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("memory_bank_stats_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "LOCAL ROOM STORAGE ENGINE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "On-Device Privacy",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Stat Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MemoryStatItem(
                            label = "Total Facts",
                            value = "$totalMemoriesCount",
                            icon = Icons.Outlined.BookmarkBorder,
                            modifier = Modifier.weight(1f)
                        )
                        MemoryStatItem(
                            label = "Starred Facts",
                            value = "$importantMemoriesCount",
                            icon = Icons.Outlined.Star,
                            modifier = Modifier.weight(1f)
                        )
                        MemoryStatItem(
                            label = "Categories",
                            value = "$uniqueCategoriesCount",
                            icon = Icons.Default.Category,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val msg = viewModel.exportMemoriesToDownloads(context)
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("export_memories_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.export_backup), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val msg = viewModel.importMemoriesFromDownloads(context)
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("import_memories_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.import_backup), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_memories_input"),
                placeholder = { Text(stringResource(R.string.search_memories), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Category Chips Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("filter_chip_$cat")
                    )
                }
            }

            // Header for Recalled Facts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECALLED FACTS (${filteredMemories.size})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                )
                if (memories.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearAllConfirm = true },
                        modifier = Modifier.testTag("clear_all_memories_btn")
                    ) {
                        Text(
                            text = stringResource(R.string.clear_all),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Memory Items List
            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != "All") "No memories matching filter criteria" else stringResource(R.string.no_memories_placeholder),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMemories, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("memory_card_item_${item.id}")
                                .combinedClickable(
                                    onClick = {
                                        editingMemory = item
                                        showEditDialog = true
                                    },
                                    onLongClick = {
                                        editingMemory = item
                                        showEditDialog = true
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isImportant) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                1.dp,
                                if (item.isImportant) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Category Badge
                                        Surface(
                                            color = when (item.category) {
                                                "Work" -> MaterialTheme.colorScheme.secondaryContainer
                                                "Tasks" -> MaterialTheme.colorScheme.tertiaryContainer
                                                "Reminders" -> MaterialTheme.colorScheme.errorContainer
                                                "Preferences" -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.category.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        // Source Badge
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = item.source.uppercase(),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = item.factText,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = if (item.isImportant) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.testTag("memory_text_${item.id}")
                                    )

                                    Text(
                                        text = DateFormat.format("yyyy-MM-dd HH:mm", item.createdAt).toString(),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Important Star Toggle Button
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleMemoryImportant(item)
                                            coroutineScope.launch {
                                                val stateText = if (!item.isImportant) "Marked as important ⭐" else "Unmarked important"
                                                snackbarHostState.showSnackbar(stateText)
                                            }
                                        },
                                        modifier = Modifier.testTag("toggle_important_btn_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (item.isImportant) Icons.Default.Star else Icons.Outlined.Star,
                                            contentDescription = "Toggle Important",
                                            tint = if (item.isImportant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Edit Button
                                    IconButton(
                                        onClick = {
                                            editingMemory = item
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_memory_btn_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Edit Memory",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteMemory(item.id)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Fact removed from Memory Bank")
                                            }
                                        },
                                        modifier = Modifier.testTag("delete_memory_btn_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete Memory",
                                            tint = MaterialTheme.colorScheme.error,
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

@Composable
private fun MemoryStatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.SansSerif
        )
    }
}

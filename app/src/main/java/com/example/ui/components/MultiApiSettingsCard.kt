package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ApiManager
import com.example.data.ChatKeyManager
import com.example.network.api.ApiDefaults
import com.example.network.api.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiApiSettingsCard() {
    val context = LocalContext.current
    val apiManager = remember { ApiManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var isExpanded by remember { mutableStateOf(true) }
    var activeProvider by remember { mutableStateOf(apiManager.getActiveProvider()) }
    var activeModel by remember { mutableStateOf(apiManager.getSelectedModel(activeProvider)) }
    var providerDropdownExpanded by remember { mutableStateOf(false) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Multi-API",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Multi-API Configuration",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active: ${activeProvider.displayName} ($activeModel)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    Text(
                        text = "AIRA OS supports 8 API providers with automatic failover chain. Configure your primary provider below.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Provider Dropdown
                    ExposedDropdownMenuBox(
                        expanded = providerDropdownExpanded,
                        onExpandedChange = { providerDropdownExpanded = !providerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${activeProvider.displayName} ${if (activeProvider.isPaid) "(Paid)" else "(Free)"}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Primary API Provider") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = providerDropdownExpanded,
                            onDismissRequest = { providerDropdownExpanded = false }
                        ) {
                            ApiProvider.values().forEach { provider ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(provider.displayName, fontWeight = FontWeight.Medium)
                                            Text(
                                                if (provider.isPaid) "Paid" else "Free",
                                                color = if (provider.isPaid) Color(0xFFE53935) else Color(0xFF43A047),
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        activeProvider = provider
                                        apiManager.setActiveProvider(provider)
                                        val defaultModel = ApiDefaults.modelsMap[provider]?.firstOrNull() ?: ""
                                        activeModel = apiManager.getSelectedModel(provider)
                                        if (activeModel.isBlank()) {
                                            activeModel = defaultModel
                                            apiManager.setSelectedModel(provider, defaultModel)
                                        }
                                        providerDropdownExpanded = false
                                        testResultMsg = null
                                    }
                                )
                            }
                        }
                    }

                    // Model Dropdown
                    val availableModels = ApiDefaults.modelsMap[activeProvider] ?: emptyList()
                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = activeModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Selected Model") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        activeModel = model
                                        apiManager.setSelectedModel(activeProvider, model)
                                        modelDropdownExpanded = false
                                        testResultMsg = null
                                    }
                                )
                            }
                        }
                    }

                    // Connection Test Button & Output
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    isTesting = true
                                    testResultMsg = "Testing ${activeProvider.displayName} connection..."
                                    val apiKey = apiManager.getKeyForProvider(activeProvider)
                                    val result = apiManager.testConnection(activeProvider, apiKey, activeModel)
                                    withContext(Dispatchers.Main) {
                                        isTesting = false
                                        testResultMsg = if (result.isSuccess) {
                                            result.getOrNull() ?: "Connected successfully!"
                                        } else {
                                            val rawErr = result.exceptionOrNull()?.message ?: "Unknown error"
                                            val cleanErr = when {
                                                rawErr.contains("401") || rawErr.contains("403") || rawErr.contains("Key") -> "Invalid API Key. Please verify your API key in the settings below."
                                                rawErr.contains("404") -> "Model unavailable. Please select another model or provider."
                                                rawErr.contains("429") -> "Rate limit reached. Please wait a moment or switch provider."
                                                else -> "Unable to reach ${activeProvider.displayName}. Please check your network connection."
                                            }
                                            "Connection Failed: $cleanErr"
                                        }
                                    }
                                }
                            },
                            enabled = !isTesting,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Test Connection")
                        }
                    }

                    testResultMsg?.let { msg ->
                        Surface(
                            color = if (msg.contains("verified") || msg.contains("Connected") || msg.contains("✅"))
                                Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                color = if (msg.contains("verified") || msg.contains("Connected") || msg.contains("✅"))
                                    Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                    Text(
                        text = "API Key Vault (Unlimited Keys per Provider)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Unlimited Multi-Key Provider Vault
                    ApiProvider.values().forEach { provider ->
                        ProviderMultiKeyVaultField(
                            provider = provider,
                            apiManager = apiManager
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderMultiKeyVaultField(
    provider: ApiProvider,
    apiManager: ApiManager
) {
    val context = LocalContext.current
    val multiKeyManager = remember { com.example.data.MultiKeyManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var keyList by remember(provider) { mutableStateOf(multiKeyManager.getKeys(provider.name)) }
    var newKeyInput by remember { mutableStateOf("") }
    var isAddingKey by remember { mutableStateOf(false) }
    var testingKeyMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${provider.displayName} (${keyList.size} keys)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { isAddingKey = !isAddingKey },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isAddingKey) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Add key",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isAddingKey) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newKeyInput,
                    onValueChange = { newKeyInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_key_input_${provider.name.lowercase()}"),
                    placeholder = { Text("Paste new ${provider.displayName} key") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        val trimmed = newKeyInput.trim()
                        if (trimmed.isNotBlank()) {
                            multiKeyManager.addKey(provider.name, trimmed)
                            apiManager.saveKeyForProvider(provider, trimmed)
                            keyList = multiKeyManager.getKeys(provider.name)
                            newKeyInput = ""
                            isAddingKey = false

                            // Auto-verify newly added key
                            scope.launch(Dispatchers.IO) {
                                com.example.network.api.ApiTest.testKey(context, provider, trimmed, apiManager.getSelectedModel(provider))
                                withContext(Dispatchers.Main) {
                                    keyList = multiKeyManager.getKeys(provider.name)
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add")
                }
            }
        }

        // Display List of Configured Keys
        if (keyList.isEmpty()) {
            Text(
                text = "No keys configured",
                fontSize = 12.sp,
                color = Color.Gray
            )
        } else {
            keyList.forEachIndexed { index, key ->
                val status = multiKeyManager.getKeyStatus(provider.name, key)
                val isTestingKey = testingKeyMap[key] == true

                val maskedKey = if (key.length > 8) {
                    "****" + key.takeLast(4)
                } else {
                    "****"
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (status.isSuccess) "🟢" else "🔴",
                                fontSize = 12.sp
                            )
                            Column {
                                Text(
                                    text = maskedKey,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = status.message,
                                    fontSize = 11.sp,
                                    color = if (status.isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Test Key Button
                            OutlinedButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        testingKeyMap = testingKeyMap + (key to true)
                                        com.example.network.api.ApiTest.testKey(
                                            context = context,
                                            provider = provider,
                                            apiKey = key,
                                            model = apiManager.getSelectedModel(provider)
                                        )
                                        withContext(Dispatchers.Main) {
                                            testingKeyMap = testingKeyMap - key
                                            keyList = multiKeyManager.getKeys(provider.name)
                                        }
                                    }
                                },
                                enabled = !isTestingKey,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTestingKey) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Test", fontSize = 11.sp)
                                }
                            }

                            // Delete Key Button
                            IconButton(
                                onClick = {
                                    multiKeyManager.removeKey(provider.name, index)
                                    keyList = multiKeyManager.getKeys(provider.name)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete key",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

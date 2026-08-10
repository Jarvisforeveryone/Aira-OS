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
                                .menuAnchor()
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
                                .menuAnchor()
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
                                            result.getOrNull() ?: "Connected!"
                                        } else {
                                            "Failed: ${result.exceptionOrNull()?.message}"
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
                        text = "API Key Vault (All Providers)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Individual Provider Key Fields
                    ApiProvider.values().forEach { provider ->
                        ProviderKeyInputField(
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
private fun ProviderKeyInputField(
    provider: ApiProvider,
    apiManager: ApiManager
) {
    var keyText by remember(provider) { mutableStateOf(apiManager.getKeyForProvider(provider)) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${provider.displayName} Key",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (keyText.isNotBlank()) "Configured" else "Not set",
                fontSize = 11.sp,
                color = if (keyText.isNotBlank()) Color(0xFF2E7D32) else Color.Gray
            )
        }

        OutlinedTextField(
            value = keyText,
            onValueChange = { newValue ->
                keyText = newValue
                apiManager.saveKeyForProvider(provider, newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_key_input_${provider.name.lowercase()}"),
            placeholder = { Text("Enter ${provider.displayName} API key") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle key visibility"
                    )
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PiperTtsManager
import com.example.util.NativeLibraryLoader
import kotlinx.coroutines.launch

class PiperTtsTestActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            var libStatus by remember { mutableStateOf("Not Checked") }
            var isLoaded by remember { mutableStateOf(NativeLibraryLoader.isLoaded()) }
            var testText by remember { mutableStateOf("Hello! This is a real-time JNI synthesis test using Piper.") }
            var statusMessage by remember { mutableStateOf("Idle") }
            
            LaunchedEffect(Unit) {
                isLoaded = NativeLibraryLoader.isLoaded()
                libStatus = if (isLoaded) "Loaded successfully" else "Not loaded"
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Piper TTS JNI Diagnostic") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp)
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Card for Native Library Status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "JNI Native Library Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Divider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("libonnxruntime.so:")
                                Text(
                                    text = if (isLoaded) "EXISTS & LOADED" else "PENDING",
                                    color = if (isLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("libpiper.so:")
                                Text(
                                    text = if (isLoaded) "EXISTS & LOADED" else "PENDING",
                                    color = if (isLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Load Status: $libStatus",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Button(
                                onClick = {
                                    val success = NativeLibraryLoader.loadLibraries(this@PiperTtsTestActivity)
                                    isLoaded = success
                                    libStatus = if (success) "Loaded successfully" else "Failed to load libraries"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LibraryMusic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trigger JNI Library Load")
                            }
                        }
                    }

                    // Card for Synthesis Test
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "TTS Synthesis Test",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Divider()
                            OutlinedTextField(
                                value = testText,
                                onValueChange = { testText = it },
                                label = { Text("Text to Synthesize") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            statusMessage = "Synthesizing and playing..."
                                            val ttsManager = PiperTtsManager.activeInstance ?: PiperTtsManager(applicationContext)
                                            ttsManager.selectedTtsEngine = "PIPER_OFFLINE"
                                            ttsManager.speak(testText)
                                        } catch (e: Exception) {
                                            statusMessage = "Error: ${e.message}"
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = isLoaded
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Synthesize")
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val ttsManager = PiperTtsManager.activeInstance ?: PiperTtsManager(applicationContext)
                                            ttsManager.stop()
                                            statusMessage = "Stopped"
                                        } catch (e: Exception) {
                                            statusMessage = "Error: ${e.message}"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.weight(1f),
                                    enabled = isLoaded
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop")
                                }
                            }

                            Text(
                                text = "Status: $statusMessage",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "This screen validates JNI integration directly on device using standard System.loadLibrary APIs.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

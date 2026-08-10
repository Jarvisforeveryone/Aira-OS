package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.DownloadManager
import kotlinx.coroutines.launch

@Composable
fun SetupPopup() {
    val state by DownloadManager.popupState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (!state.showPopup) return

    AlertDialog(
        onDismissRequest = {
            if (state.isPromptState || state.isFailed || state.isComplete) {
                DownloadManager.dismissPopup()
            }
        },
        title = {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                if (!state.isPromptState && !state.isFailed) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { state.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        },
        confirmButton = {
            if (state.isPromptState) {
                Button(
                    onClick = {
                        scope.launch {
                            DownloadManager.downloadAllModels(context)
                        }
                    }
                ) {
                    Text("Download All")
                }
            } else if (state.isFailed) {
                Button(
                    onClick = {
                        scope.launch {
                            DownloadManager.retryDownload(context)
                        }
                    }
                ) {
                    Text("Retry")
                }
            }
        },
        dismissButton = {
            if (state.isPromptState) {
                TextButton(
                    onClick = {
                        DownloadManager.dismissPopup()
                    }
                ) {
                    Text("Select Later")
                }
            } else if (state.isFailed) {
                TextButton(
                    onClick = {
                        DownloadManager.dismissPopup()
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

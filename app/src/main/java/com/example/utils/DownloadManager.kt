package com.example.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

enum class DownloadComponent {
    PIPER_TTS,
    VOSK_STT
}

data class DownloadPopupState(
    val showPopup: Boolean = false,
    val title: String = "Downloading essential components...",
    val progressPercent: Int = 0,
    val statusText: String = "",
    val isFailed: Boolean = false,
    val isComplete: Boolean = false,
    val failedComponent: DownloadComponent? = null
)

object DownloadManager {
    private const val TAG = "DownloadManager"

    const val PIPER_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx"
    const val PIPER_CONFIG_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx.json"
    const val VOSK_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"

    // Target expected size bounds
    private const val PIPER_MIN_SIZE_BYTES = 55_000_000L // ~63.2 MB (63,201,294 bytes)
    private const val VOSK_MIN_EXTRACTED_BYTES = 10_000_000L // ~12 MB extracted

    private val _popupState = MutableStateFlow(DownloadPopupState())
    val popupState: StateFlow<DownloadPopupState> = _popupState.asStateFlow()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun dismissPopup() {
        _popupState.value = DownloadPopupState(showPopup = false)
    }

    // --- PIPER TTS LOCAL FILE CHECKS & DOWNLOAD ---
    fun getPiperModelFile(context: Context): File {
        val dir = File(context.filesDir, "piper_models")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "en_US-amy-medium.onnx")
    }

    fun getPiperConfigFile(context: Context): File {
        val dir = File(context.filesDir, "piper_models")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "en_US-amy-medium.onnx.json")
    }

    fun isPiperModelDownloaded(context: Context): Boolean {
        val file = getPiperModelFile(context)
        return file.exists() && file.length() >= PIPER_MIN_SIZE_BYTES
    }

    suspend fun downloadPiperModel(context: Context, onResult: (Boolean) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        if (isPiperModelDownloaded(context)) {
            withContext(Dispatchers.Main) { onResult(true) }
            return@withContext true
        }

        withContext(Dispatchers.Main) {
            _popupState.value = DownloadPopupState(
                showPopup = true,
                title = "Downloading essential components...",
                progressPercent = 0,
                statusText = "Downloading Amy Voice Model...",
                isFailed = false,
                isComplete = false,
                failedComponent = DownloadComponent.PIPER_TTS
            )
        }

        var success = false
        var attempts = 0
        while (attempts < 2 && !success) {
            attempts++
            try {
                val modelFile = getPiperModelFile(context)
                val configFile = getPiperConfigFile(context)
                val tempModelFile = File(modelFile.absolutePath + ".tmp")

                // 1. Download Model ONNX
                val request = Request.Builder().url(PIPER_URL).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("HTTP Error ${response.code}")
                    val body = response.body ?: throw Exception("Empty response body")
                    val totalBytes = body.contentLength()

                    FileOutputStream(tempModelFile).use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(32768)
                            var read: Int
                            var downloaded = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                                downloaded += read
                                val pct = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                                withContext(Dispatchers.Main) {
                                    _popupState.value = _popupState.value.copy(
                                        progressPercent = pct,
                                        statusText = "Downloading Amy Voice ($pct%)"
                                    )
                                }
                            }
                        }
                    }
                }

                // Verify size
                if (tempModelFile.length() < PIPER_MIN_SIZE_BYTES) {
                    tempModelFile.delete()
                    throw Exception("Downloaded file is corrupt or truncated (${tempModelFile.length()} bytes)")
                }

                if (modelFile.exists()) modelFile.delete()
                if (!tempModelFile.renameTo(modelFile)) {
                    tempModelFile.copyTo(modelFile, overwrite = true)
                    tempModelFile.delete()
                }

                // 2. Download Model Config JSON
                try {
                    val configRequest = Request.Builder().url(PIPER_CONFIG_URL).build()
                    client.newCall(configRequest).execute().use { resp ->
                        if (resp.isSuccessful && resp.body != null) {
                            FileOutputStream(configFile).use { out ->
                                resp.body!!.byteStream().copyTo(out)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Config download warning: ${e.message}")
                }

                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Attempt $attempts failed to download Piper TTS: ${e.message}", e)
            }
        }

        withContext(Dispatchers.Main) {
            if (success) {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    title = "Download complete",
                    progressPercent = 100,
                    statusText = "Amy Voice ready",
                    isFailed = false,
                    isComplete = true
                )
                kotlinx.coroutines.delay(1200L)
                dismissPopup()
                onResult(true)
            } else {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    title = "Download failed",
                    progressPercent = 0,
                    statusText = "Failed to download Amy Voice. Tap Retry.",
                    isFailed = true,
                    failedComponent = DownloadComponent.PIPER_TTS
                )
                onResult(false)
            }
        }
        return@withContext success
    }

    // --- VOSK STT LOCAL FILE CHECKS & DOWNLOAD ---
    fun getVoskModelDir(context: Context): File {
        val primaryDir = File(context.filesDir, "vosk_models/model-en")
        if (File(primaryDir, "conf/model.conf").exists()) return primaryDir
        val legacyDir = File(context.filesDir, "models/model-en")
        if (File(legacyDir, "conf/model.conf").exists()) return legacyDir
        return primaryDir
    }

    fun isVoskModelDownloaded(context: Context): Boolean {
        val dir = getVoskModelDir(context)
        val conf = File(dir, "conf/model.conf")
        if (!dir.exists() || !conf.exists() || conf.length() == 0L) return false

        val totalSize = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return totalSize >= VOSK_MIN_EXTRACTED_BYTES
    }

    suspend fun downloadVoskModel(context: Context, onResult: (Boolean) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        if (isVoskModelDownloaded(context)) {
            withContext(Dispatchers.Main) { onResult(true) }
            return@withContext true
        }

        withContext(Dispatchers.Main) {
            _popupState.value = DownloadPopupState(
                showPopup = true,
                title = "Downloading essential components...",
                progressPercent = 0,
                statusText = "Downloading Vosk Offline STT Model...",
                isFailed = false,
                isComplete = false,
                failedComponent = DownloadComponent.VOSK_STT
            )
        }

        var success = false
        var attempts = 0
        while (attempts < 2 && !success) {
            attempts++
            try {
                val zipFile = File(context.cacheDir, "vosk-model-small-en-us-0.15.zip")
                if (zipFile.exists()) zipFile.delete()

                val request = Request.Builder().url(VOSK_URL).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("HTTP Error ${response.code}")
                    val body = response.body ?: throw Exception("Empty response body")
                    val totalBytes = body.contentLength()

                    FileOutputStream(zipFile).use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(32768)
                            var read: Int
                            var downloaded = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                                downloaded += read
                                val pct = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                                withContext(Dispatchers.Main) {
                                    _popupState.value = _popupState.value.copy(
                                        progressPercent = pct,
                                        statusText = "Downloading Vosk STT ($pct%)"
                                    )
                                }
                            }
                        }
                    }
                }

                // Extract
                withContext(Dispatchers.Main) {
                    _popupState.value = _popupState.value.copy(
                        statusText = "Extracting Vosk Model..."
                    )
                }

                val targetDir = File(context.filesDir, "vosk_models/model-en")
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()

                val tempExtractDir = File(context.cacheDir, "vosk_temp_extract")
                if (tempExtractDir.exists()) tempExtractDir.deleteRecursively()
                tempExtractDir.mkdirs()

                ZipInputStream(zipFile.inputStream()).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        val entryFile = File(tempExtractDir, entry.name)
                        if (entry.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile?.mkdirs()
                            entryFile.outputStream().use { output ->
                                zipInput.copyTo(output)
                            }
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }

                val subDirs = tempExtractDir.listFiles { f -> f.isDirectory }
                if (subDirs != null && subDirs.isNotEmpty()) {
                    subDirs[0].renameTo(targetDir)
                } else {
                    tempExtractDir.renameTo(targetDir)
                }

                if (tempExtractDir.exists()) tempExtractDir.deleteRecursively()
                if (zipFile.exists()) zipFile.delete()

                val totalExtracted = targetDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                val confFile = File(targetDir, "conf/model.conf")
                if (!confFile.exists() || totalExtracted < VOSK_MIN_EXTRACTED_BYTES) {
                    targetDir.deleteRecursively()
                    throw Exception("Extracted Vosk model is incomplete ($totalExtracted bytes)")
                }

                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Attempt $attempts failed to download Vosk STT: ${e.message}", e)
            }
        }

        withContext(Dispatchers.Main) {
            if (success) {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    title = "Download complete",
                    progressPercent = 100,
                    statusText = "Vosk STT ready",
                    isFailed = false,
                    isComplete = true
                )
                kotlinx.coroutines.delay(1200L)
                dismissPopup()
                onResult(true)
            } else {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    title = "Download failed",
                    progressPercent = 0,
                    statusText = "Failed to download Vosk model. Tap Retry.",
                    isFailed = true,
                    failedComponent = DownloadComponent.VOSK_STT
                )
                onResult(false)
            }
        }
        return@withContext success
    }

    suspend fun retryDownload(context: Context) {
        val component = _popupState.value.failedComponent
        if (component == DownloadComponent.PIPER_TTS) {
            downloadPiperModel(context)
        } else if (component == DownloadComponent.VOSK_STT) {
            downloadVoskModel(context)
        }
    }
}

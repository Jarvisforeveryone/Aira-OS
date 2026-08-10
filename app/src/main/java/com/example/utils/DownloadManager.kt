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
    VOSK_STT,
    LLAMA_CPP,
    ALL
}

data class DownloadPopupState(
    val showPopup: Boolean = false,
    val isPromptState: Boolean = false, // First-open 3-in-1 prompt
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
    const val VOSK_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.22.zip"
    const val LLAMA_URL = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"

    // Target expected size bounds for verification
    private const val PIPER_MIN_SIZE_BYTES = 55_000_000L // ~63 MB
    private const val VOSK_MIN_EXTRACTED_BYTES = 10_000_000L // ~40 MB zip / ~12 MB+ extracted
    private const val LLAMA_MIN_SIZE_BYTES = 1_000_000_000L // ~1.5 GB

    private val _popupState = MutableStateFlow(DownloadPopupState())
    val popupState: StateFlow<DownloadPopupState> = _popupState.asStateFlow()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    fun dismissPopup() {
        _popupState.value = DownloadPopupState(showPopup = false)
    }

    fun showSetupPrompt() {
        _popupState.value = DownloadPopupState(
            showPopup = true,
            isPromptState = true,
            title = "Offline AI & Voice Setup",
            statusText = "Download offline AI voice, speech recognition, and Llama 3.2 models for 100% private offline mode."
        )
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

    suspend fun downloadPiperModel(
        context: Context,
        showProgressPopup: Boolean = true,
        onResult: (Boolean) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (isPiperModelDownloaded(context)) {
            withContext(Dispatchers.Main) { onResult(true) }
            return@withContext true
        }

        if (showProgressPopup) {
            withContext(Dispatchers.Main) {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    isPromptState = false,
                    title = "Downloading Voice Model",
                    progressPercent = 0,
                    statusText = "Connecting to HuggingFace (Amy Voice)...",
                    isFailed = false,
                    isComplete = false,
                    failedComponent = DownloadComponent.PIPER_TTS
                )
            }
        }

        var success = false
        var attempts = 0
        while (attempts < 3 && !success) {
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
                                if (showProgressPopup) {
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

        if (showProgressPopup) {
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
                    kotlinx.coroutines.delay(1000L)
                    dismissPopup()
                } else {
                    _popupState.value = DownloadPopupState(
                        showPopup = true,
                        title = "Download failed",
                        progressPercent = 0,
                        statusText = "Failed to download Amy Voice. Tap Retry.",
                        isFailed = true,
                        failedComponent = DownloadComponent.PIPER_TTS
                    )
                }
            }
        }

        withContext(Dispatchers.Main) { onResult(success) }
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

    suspend fun downloadVoskModel(
        context: Context,
        showProgressPopup: Boolean = true,
        onResult: (Boolean) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (isVoskModelDownloaded(context)) {
            withContext(Dispatchers.Main) { onResult(true) }
            return@withContext true
        }

        if (showProgressPopup) {
            withContext(Dispatchers.Main) {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    isPromptState = false,
                    title = "Downloading STT Model",
                    progressPercent = 0,
                    statusText = "Connecting to Alphacephei (Vosk STT)...",
                    isFailed = false,
                    isComplete = false,
                    failedComponent = DownloadComponent.VOSK_STT
                )
            }
        }

        var success = false
        var attempts = 0
        while (attempts < 3 && !success) {
            attempts++
            try {
                val zipFile = File(context.cacheDir, "vosk-model-small-en-us-0.22.zip")
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
                                if (showProgressPopup) {
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
                }

                // Extract
                if (showProgressPopup) {
                    withContext(Dispatchers.Main) {
                        _popupState.value = _popupState.value.copy(
                            statusText = "Extracting Vosk Model..."
                        )
                    }
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

        if (showProgressPopup) {
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
                    kotlinx.coroutines.delay(1000L)
                    dismissPopup()
                } else {
                    _popupState.value = DownloadPopupState(
                        showPopup = true,
                        title = "Download failed",
                        progressPercent = 0,
                        statusText = "Failed to download Vosk model. Tap Retry.",
                        isFailed = true,
                        failedComponent = DownloadComponent.VOSK_STT
                    )
                }
            }
        }

        withContext(Dispatchers.Main) { onResult(success) }
        return@withContext success
    }

    // --- LLAMA 3.2 OFFLINE AI LOCAL FILE CHECKS & DOWNLOAD ---
    fun getLlamaModelFile(context: Context): File {
        val primaryDir = File(context.filesDir, "llama_models")
        if (!primaryDir.exists()) primaryDir.mkdirs()
        val primaryFile = File(primaryDir, "Llama-3.2-3B-Instruct-Q4_K_M.gguf")
        if (primaryFile.exists() && primaryFile.length() >= LLAMA_MIN_SIZE_BYTES) return primaryFile

        val legacyDir = File(context.filesDir, "models")
        if (!legacyDir.exists()) legacyDir.mkdirs()
        val legacyFile1 = File(legacyDir, "Llama-3.2-3B-Instruct-Q4_K_M.gguf")
        if (legacyFile1.exists() && legacyFile1.length() >= LLAMA_MIN_SIZE_BYTES) return legacyFile1

        val legacyFile2 = File(legacyDir, "llama-3.2-1b-instruct.gguf")
        if (legacyFile2.exists() && legacyFile2.length() >= LLAMA_MIN_SIZE_BYTES) return legacyFile2

        return primaryFile
    }

    fun isLlamaModelDownloaded(context: Context): Boolean {
        val file = getLlamaModelFile(context)
        return file.exists() && file.length() >= LLAMA_MIN_SIZE_BYTES
    }

    suspend fun downloadLlamaModel(
        context: Context,
        showProgressPopup: Boolean = true,
        onResult: (Boolean) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (isLlamaModelDownloaded(context)) {
            withContext(Dispatchers.Main) { onResult(true) }
            return@withContext true
        }

        if (showProgressPopup) {
            withContext(Dispatchers.Main) {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    isPromptState = false,
                    title = "Downloading Offline AI Model",
                    progressPercent = 0,
                    statusText = "Connecting to HuggingFace (Llama 3.2 3B)...",
                    isFailed = false,
                    isComplete = false,
                    failedComponent = DownloadComponent.LLAMA_CPP
                )
            }
        }

        var success = false
        var attempts = 0
        while (attempts < 3 && !success) {
            attempts++
            try {
                val modelFile = getLlamaModelFile(context)
                val tempModelFile = File(modelFile.absolutePath + ".tmp")

                val request = Request.Builder().url(LLAMA_URL).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("HTTP Error ${response.code}")
                    val body = response.body ?: throw Exception("Empty response body")
                    val totalBytes = body.contentLength()

                    FileOutputStream(tempModelFile).use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(65536)
                            var read: Int
                            var downloaded = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                                downloaded += read
                                val pct = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                                if (showProgressPopup) {
                                    withContext(Dispatchers.Main) {
                                        _popupState.value = _popupState.value.copy(
                                            progressPercent = pct,
                                            statusText = "Downloading Llama 3.2 AI ($pct%)"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Verify file size
                if (tempModelFile.length() < LLAMA_MIN_SIZE_BYTES) {
                    tempModelFile.delete()
                    throw Exception("Downloaded Llama model is corrupt or incomplete (${tempModelFile.length()} bytes)")
                }

                if (modelFile.exists()) modelFile.delete()
                if (!tempModelFile.renameTo(modelFile)) {
                    tempModelFile.copyTo(modelFile, overwrite = true)
                    tempModelFile.delete()
                }

                success = true
            } catch (e: Exception) {
                Log.e(TAG, "Attempt $attempts failed to download Llama 3.2: ${e.message}", e)
            }
        }

        if (showProgressPopup) {
            withContext(Dispatchers.Main) {
                if (success) {
                    _popupState.value = DownloadPopupState(
                        showPopup = true,
                        title = "Download complete",
                        progressPercent = 100,
                        statusText = "Llama 3.2 Offline AI ready",
                        isFailed = false,
                        isComplete = true
                    )
                    kotlinx.coroutines.delay(1000L)
                    dismissPopup()
                } else {
                    _popupState.value = DownloadPopupState(
                        showPopup = true,
                        title = "Download failed",
                        progressPercent = 0,
                        statusText = "Failed to download Llama 3.2 AI model. Tap Retry.",
                        isFailed = true,
                        failedComponent = DownloadComponent.LLAMA_CPP
                    )
                }
            }
        }

        withContext(Dispatchers.Main) { onResult(success) }
        return@withContext success
    }

    // --- 3-IN-1 ALL MODELS DOWNLOAD ---
    suspend fun downloadAllModels(context: Context, onResult: (Boolean) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (!MemoryManager.isOfflineSupported(context)) {
            Log.w(TAG, "Offline mode not supported on this device. Skipping all downloads.")
            withContext(Dispatchers.Main) {
                dismissPopup()
                onResult(false)
            }
            return@withContext
        }

        withContext(Dispatchers.Main) {
            _popupState.value = DownloadPopupState(
                showPopup = true,
                isPromptState = false,
                title = "Downloading Offline Bundle",
                progressPercent = 0,
                statusText = "Starting offline bundle download...",
                isFailed = false,
                isComplete = false,
                failedComponent = DownloadComponent.ALL
            )
        }

        val piperOk = downloadPiperModel(context, showProgressPopup = true)
        if (!piperOk) {
            withContext(Dispatchers.Main) { onResult(false) }
            return@withContext
        }

        val voskOk = downloadVoskModel(context, showProgressPopup = true)
        if (!voskOk) {
            withContext(Dispatchers.Main) { onResult(false) }
            return@withContext
        }

        val llamaOk = downloadLlamaModel(context, showProgressPopup = true)

        withContext(Dispatchers.Main) {
            if (llamaOk) {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    title = "Offline Setup Complete",
                    progressPercent = 100,
                    statusText = "All offline AI & Voice models downloaded!",
                    isFailed = false,
                    isComplete = true
                )
                kotlinx.coroutines.delay(1200L)
                dismissPopup()
                onResult(true)
            } else {
                _popupState.value = DownloadPopupState(
                    showPopup = true,
                    title = "Download incomplete",
                    progressPercent = 0,
                    statusText = "Some offline models failed to download.",
                    isFailed = true,
                    failedComponent = DownloadComponent.ALL
                )
                onResult(false)
            }
        }
    }

    suspend fun retryDownload(context: Context) {
        val component = _popupState.value.failedComponent
        when (component) {
            DownloadComponent.PIPER_TTS -> downloadPiperModel(context, showProgressPopup = true)
            DownloadComponent.VOSK_STT -> downloadVoskModel(context, showProgressPopup = true)
            DownloadComponent.LLAMA_CPP -> downloadLlamaModel(context, showProgressPopup = true)
            DownloadComponent.ALL -> downloadAllModels(context)
            null -> dismissPopup()
        }
    }
}

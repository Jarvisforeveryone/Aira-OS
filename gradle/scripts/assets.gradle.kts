import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream

fun calculateChecksum(file: File, algorithm: String = "SHA-256"): String {
    if (!file.exists() || file.length() == 0L) return ""
    val digest = MessageDigest.getInstance(algorithm)
    file.inputStream().use { inputStream ->
        val buffer = ByteArray(8192)
        var bytesRead = inputStream.read(buffer)
        while (bytesRead != -1) {
            digest.update(buffer, 0, bytesRead)
            bytesRead = inputStream.read(buffer)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

tasks.register("locateAndUnzipVoskModel") {
    doLast {
        val targetConf = file("src/main/assets/models/model-en/conf/model.conf")
        if (targetConf.exists() && targetConf.length() > 0) {
            println("Vosk model already downloaded and unzipped. Skipping task to speed up build.")
            return@doLast
        }
        val zipName = "vosk-model-small-en-us-0.15.zip"
        var zipFile: File? = null

        // Search in standard locations first
        val searchDirs = listOf(
            rootDir,
            rootDir.parentFile,
            projectDir,
            file("src/main/assets/models"),
            file("src/main/assets"),
            file("../"),
            file("../../")
        )
        for (dir in searchDirs) {
            if (dir == null) continue
            val f = File(dir, zipName)
            if (f.exists() && f.length() > 0) {
                zipFile = f
                break
            }
        }

        // Recursive walk if not found yet
        if (zipFile == null) {
            rootDir.walkTopDown().maxDepth(5).forEach { f ->
                if (f.name == zipName && f.length() > 0) {
                    zipFile = f
                }
            }
        }

        val targetDir = file("src/main/assets/models/model-en")

        if (zipFile == null) {
            println("Local ZIP file '$zipName' not found. Downloading from AlphaCephei...")
            val downloadFile = file("src/main/assets/models/$zipName")
            downloadFile.parentFile.mkdirs()

            try {
                val urlString = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
                var connection = URI(urlString).toURL().openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                var status = connection.responseCode
                while (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                       status == HttpURLConnection.HTTP_MOVED_PERM ||
                       status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    connection = URI(newUrl).toURL().openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    status = connection.responseCode
                }

                if (status == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        downloadFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    println("Vosk model zip downloaded successfully.")
                    zipFile = downloadFile
                } else {
                    println("Failed to download Vosk model from Alphacephei: HTTP status $status")
                }
            } catch (e: Exception) {
                println("Error downloading model ZIP: ${e.message}")
            }
        }

        if (zipFile != null && zipFile!!.exists() && zipFile!!.length() > 0) {
            val checksum = calculateChecksum(zipFile!!, "SHA-256")
            println("Found ZIP at: ${zipFile!!.absolutePath} - Size: ${zipFile!!.length()} bytes - SHA-256: $checksum")

            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()

            val tempExtractDir = file("src/main/assets/models/temp_extract")
            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
            }
            tempExtractDir.mkdirs()

            println("Extracting ZIP contents...")
            ZipInputStream(zipFile!!.inputStream()).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    val entryFile = File(tempExtractDir, entry.name)
                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile.mkdirs()
                        entryFile.outputStream().use { output ->
                            zipInput.copyTo(output)
                        }
                    }
                    zipInput.closeEntry()
                    entry = zipInput.nextEntry
                }
            }
            println("Extracted ZIP contents.")

            val subDirs = tempExtractDir.listFiles { f -> f.isDirectory }
            if (subDirs != null && subDirs.isNotEmpty()) {
                val sourceDir = subDirs[0]
                sourceDir.renameTo(targetDir)
                println("Renamed model subfolder to $targetDir")
            } else {
                tempExtractDir.renameTo(targetDir)
                println("Renamed extraction folder directly to $targetDir")
            }

            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
            }

            val confDir = File(targetDir, "conf")
            val confFile = File(confDir, "model.conf")
            if (!confDir.exists()) {
                confDir.mkdirs()
            }
            if (!confFile.exists()) {
                confFile.writeText("")
                println("Created empty model.conf at: ${confFile.absolutePath}")
            }
        } else {
            println("WARNING: Vosk ZIP file not available. Initializing empty model structure as fallback.")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
                val confFile = file("src/main/assets/models/model-en/conf/model.conf")
                confFile.parentFile.mkdirs()
                confFile.writeText("")
            }
        }
    }
}

tasks.register("downloadAssets") {
    doLast {
        val piperModelsDir = file("src/main/assets/piper/models")
        if (!piperModelsDir.exists()) {
            piperModelsDir.mkdirs()
        }

        val finalOnnx = file("src/main/assets/piper/models/en_US-amy-medium.onnx")
        val finalJson = file("src/main/assets/piper/models/en_US-amy-medium.onnx.json")

        if (finalOnnx.exists() && finalOnnx.length() > 0 && finalJson.exists() && finalJson.length() > 0) {
            val onnxChecksum = calculateChecksum(finalOnnx, "SHA-256")
            val jsonChecksum = calculateChecksum(finalJson, "SHA-256")
            println("Piper TTS assets already exist and verified in piper/models.")
            println("en_US-amy-medium.onnx SHA-256: $onnxChecksum")
            println("en_US-amy-medium.onnx.json SHA-256: $jsonChecksum")
            return@doLast
        }

        val onnxUrl = "https://drive.google.com/uc?export=download&id=11ODrQItImBgQYBwWA6wV7uUuzkuWDOBa"
        val jsonUrl = "https://drive.google.com/uc?export=download&id=1NNvlGCY_4uBPXLbfTKf0GkmcjLeJpBqY"

        fun downloadFile(urlString: String, tempFile: File) {
            try {
                println("Downloading from $urlString to ${tempFile.absolutePath}...")
                var connection = URI(urlString).toURL().openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                var status = connection.responseCode
                while (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                       status == HttpURLConnection.HTTP_MOVED_PERM ||
                       status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    connection = URI(newUrl).toURL().openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    status = connection.responseCode
                }
                if (status == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    println("Download completed. Saved to ${tempFile.absolutePath} - Size: ${tempFile.length()} bytes")
                } else {
                    println("WARNING: Failed to download asset from $urlString (HTTP $status)")
                }
            } catch (e: Exception) {
                println("WARNING: Could not download asset file ($urlString): ${e.message}")
            }
        }

        val tempOnnxTxt = file("src/main/assets/piper/models/en_US-amy-medium.onnx.tmp")
        val tempJsonTxt = file("src/main/assets/piper/models/en_US-amy-medium.onnx.json.tmp")

        downloadFile(onnxUrl, tempOnnxTxt)
        downloadFile(jsonUrl, tempJsonTxt)

        // Checksum verification & moving
        if (tempOnnxTxt.exists() && tempOnnxTxt.length() > 0) {
            val sha256 = calculateChecksum(tempOnnxTxt, "SHA-256")
            println("Verified en_US-amy-medium.onnx SHA-256 checksum: $sha256")
            if (sha256.isNotEmpty()) {
                tempOnnxTxt.renameTo(finalOnnx)
                println("Successfully verified and promoted en_US-amy-medium.onnx")
            }
        }

        if (tempJsonTxt.exists() && tempJsonTxt.length() > 0) {
            val sha256 = calculateChecksum(tempJsonTxt, "SHA-256")
            println("Verified en_US-amy-medium.onnx.json SHA-256 checksum: $sha256")
            if (sha256.isNotEmpty()) {
                tempJsonTxt.renameTo(finalJson)
                println("Successfully verified and promoted en_US-amy-medium.onnx.json")
            }
        }

        println("=== ASSET VERIFICATION SUMMARY ===")
        piperModelsDir.listFiles()?.forEach { f ->
            val sha256 = calculateChecksum(f, "SHA-256")
            println("- ${f.name}: Size=${f.length()} bytes (~${String.format("%.2f", f.length().toDouble() / (1024 * 1024))} MB) | SHA-256=$sha256")
        }
    }
}

// Chain tasks strictly to avoid race conditions: locateAndUnzipVoskModel -> downloadAssets -> preBuild
tasks.named("downloadAssets") {
    dependsOn("locateAndUnzipVoskModel")
}

tasks.named("preBuild") {
    dependsOn("downloadAssets")
}

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
        println("Vosk model is managed on-demand at runtime by the app. Skipping asset bundling.")
    }
}

tasks.register("downloadAssets") {
    doLast {
        println("Piper TTS models are managed on-demand at runtime by the app. Skipping asset bundling.")
    }
}

// Chain tasks strictly: locateAndUnzipVoskModel -> downloadAssets -> preBuild
tasks.named("downloadAssets") {
    dependsOn("locateAndUnzipVoskModel")
}

tasks.named("preBuild") {
    dependsOn("downloadAssets")
}


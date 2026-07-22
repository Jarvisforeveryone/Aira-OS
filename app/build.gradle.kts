import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.aira.axrtpw"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  sourceSets {
    getByName("main") {
      jniLibs.srcDir("src/main/jniLibs")
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.work.runtime.ktx)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  
  implementation("com.alphacephei:vosk-android:0.3.75@aar")
  implementation("net.java.dev.jna:jna:5.18.1@aar")
  implementation("com.google.code.gson:gson:2.11.0")
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
        
        println("--- DIAGNOSTIC: Searching for any .zip files in root and project ---")
        val workDir = file("../") // this is the workspace root "/"
        workDir.walkTopDown().maxDepth(5).forEach { f ->
            if (f.name.endsWith(".zip")) {
                println("Found ZIP file: ${f.absolutePath}")
            }
        }
        println("--- END DIAGNOSTIC ---")
        
        // Search in some standard locations first
        val searchDirs = listOf(
            rootDir,
            rootDir.parentFile,
            projectDir,
            file("src/main/assets/models"),
            file("src/main/assets"),
            file("../"), // the real workspace root
            file("../../") // potential parent
        )
        for (dir in searchDirs) {
            if (dir == null) continue
            val f = File(dir, zipName)
            if (f.exists()) {
                zipFile = f
                break
            }
        }

        // Recursive walk if not found yet
        if (zipFile == null) {
            println("Walking the root directory recursively to find $zipName...")
            rootDir.walkTopDown().forEach { f ->
                if (f.name == zipName) {
                    zipFile = f
                }
            }
        }

        if (zipFile == null) {
            println("Walking project directory to find $zipName...")
            projectDir.walkTopDown().forEach { f ->
                if (f.name == zipName) {
                    zipFile = f
                }
            }
        }

        // Check if there is already a model dir or another ZIP we can use
        if (zipFile == null) {
            // Let's check if the zip file actually got renamed, or if any zip with 'vosk' is found
            workDir.walkTopDown().maxDepth(4).forEach { f ->
                if (f.name.contains("vosk") && f.name.endsWith(".zip")) {
                    zipFile = f
                }
            }
        }

        val targetDir = file("src/main/assets/models/model-en")

        if (zipFile == null) {
            println("Local ZIP file '$zipName' not found. Falling back to downloading from AlphaCephei...")
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

        if (zipFile != null) {
            println("Found ZIP at: ${zipFile!!.absolutePath}")
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
            
            // Clean up extraction temp directory if it still exists
            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
            }
            
            // Clean up downloaded file if we downloaded it
            if (zipFile!!.absolutePath.contains("src/main/assets/models/")) {
                zipFile!!.delete()
            }
            
            // Make sure the conf directory and model.conf exist in final path
            val confDir = File(targetDir, "conf")
            val confFile = File(confDir, "model.conf")
            if (!confDir.exists()) {
                confDir.mkdirs()
            }
            if (!confFile.exists()) {
                confFile.writeText("") // Create empty model.conf if not present to avoid Vosk failure
                println("Created empty model.conf at: ${confFile.absolutePath}")
            }
        } else {
            println("ERROR: Local ZIP file '$zipName' not found! Make sure it was uploaded.")
            // Failsafe: Let's create an empty model directory so compilation does not fail if they haven't uploaded yet
            if (!targetDir.exists()) {
                targetDir.mkdirs()
                val confFile = file("src/main/assets/models/model-en/conf/model.conf")
                confFile.parentFile.mkdirs()
                confFile.writeText("")
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("locateAndUnzipVoskModel")
}

tasks.register("downloadAssets") {
    doLast {
        val ttsModelsDir = file("src/main/assets/tts_models")
        if (!ttsModelsDir.exists()) {
            ttsModelsDir.mkdirs()
        }

        // Delete dummy files as requested
        val dummyOnnx = file("src/main/assets/tts_models/amifemalevoicemodel.onnx")
        val dummyJson = file("src/main/assets/tts_models/amifemalevoicemodel.json")
        if (dummyOnnx.exists()) {
            println("Deleting dummy ONNX...")
            dummyOnnx.delete()
        }
        if (dummyJson.exists()) {
            println("Deleting dummy JSON...")
            dummyJson.delete()
        }

        val onnxUrl = "https://drive.google.com/uc?export=download&id=11ODrQItImBgQYBwWA6wV7uUuzkuWDOBa"
        val jsonUrl = "https://drive.google.com/uc?export=download&id=1NNvlGCY_4uBPXLbfTKf0GkmcjLeJpBqY"

        fun downloadFile(urlString: String, tempFile: File) {
            println("Downloading from $urlString to ${tempFile.absolutePath}...")
            var connection = URI(urlString).toURL().openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            var status = connection.responseCode
            while (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                   status == HttpURLConnection.HTTP_MOVED_PERM || 
                   status == 307 || status == 308) {
                val newUrl = connection.getHeaderField("Location")
                println("Redirected to $newUrl")
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
                throw GradleException("Failed to download file. Response code: $status")
            }
        }

        // 1. Download as amymodel.onnx.txt and config.json.txt
        val tempOnnxTxt = file("src/main/assets/tts_models/amymodel.onnx.txt")
        val tempJsonTxt = file("src/main/assets/tts_models/config.json.txt")

        downloadFile(onnxUrl, tempOnnxTxt)
        downloadFile(jsonUrl, tempJsonTxt)

        // 2. Rename and move to target location
        val finalOnnx = file("src/main/assets/tts_models/amymodel.onnx")
        val finalJson = file("src/main/assets/tts_models/config.json")

        if (tempOnnxTxt.exists()) {
            println("Renaming amymodel.onnx.txt to amymodel.onnx...")
            tempOnnxTxt.renameTo(finalOnnx)
        }
        if (tempJsonTxt.exists()) {
            println("Renaming config.json.txt to config.json...")
            tempJsonTxt.renameTo(finalJson)
        }

        // 3. Print verified sizes
        println("=== VERIFICATION ===")
        ttsModelsDir.listFiles()?.forEach { f ->
            println("- ${f.name}: Size=${f.length()} bytes (~${String.format("%.2f", f.length().toDouble() / (1024 * 1024))} MB)")
        }
    }
}



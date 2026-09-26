package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Centralized File and Storage Manager for AIRA OS.
 * Provides safe file operations, model caching, and atomic writes.
 */
class FileManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: FileManager? = null

        fun getInstance(context: Context): FileManager {
            return instance ?: synchronized(this) {
                instance ?: FileManager(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun writeTextFile(fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            val tempFile = File(context.filesDir, "$fileName.tmp")
            tempFile.writeText(content)
            if (tempFile.renameTo(file)) {
                true
            } else {
                file.writeText(content)
                tempFile.delete()
                true
            }
        } catch (e: Exception) {
            Logger.e("FileManager", "Error writing file: $fileName", e)
            false
        }
    }

    suspend fun readTextFile(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            Logger.e("FileManager", "Error reading file: $fileName", e)
            null
        }
    }

    fun getInternalDir(subDir: String): File {
        val dir = File(context.filesDir, subDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getCacheDir(subDir: String): File {
        val dir = File(context.cacheDir, subDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun deleteFile(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.delete() else true
    }
}

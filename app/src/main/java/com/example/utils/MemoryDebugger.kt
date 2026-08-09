package com.example.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Utility for real-time memory monitoring and debugging during process startup.
 * Uses ActivityManager.MemoryInfo and Java Runtime heap statistics to track memory consumption.
 */
object MemoryDebugger {
    private const val TAG = "MemoryDebugger"
    private val isMonitoring = AtomicBoolean(false)
    private var monitoringJob: Job? = null

    /**
     * Starts logging process heap usage and system memory info every [intervalMs]
     * for a maximum duration of [durationMs].
     */
    fun startMonitoring(
        context: Context,
        intervalMs: Long = 100L,
        durationMs: Long = 10000L
    ) {
        if (!isMonitoring.compareAndSet(false, true)) {
            Log.d(TAG, "MemoryDebugger is already active.")
            return
        }

        val appContext = context.applicationContext
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val processName = getProcessName(appContext)

        Log.i(TAG, "Starting memory monitoring for process '$processName' (PID: ${Process.myPid()}) every ${intervalMs}ms for ${durationMs}ms")

        monitoringJob = CoroutineScope(Dispatchers.Default).launch {
            val startTime = System.currentTimeMillis()
            val memInfo = ActivityManager.MemoryInfo()
            var sampleIndex = 0

            while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                sampleIndex++
                val runtime = Runtime.getRuntime()
                val maxHeapMb = runtime.maxMemory() / (1024 * 1024)
                val totalHeapMb = runtime.totalMemory() / (1024 * 1024)
                val freeHeapMb = runtime.freeMemory() / (1024 * 1024)
                val usedHeapMb = totalHeapMb - freeHeapMb

                activityManager?.getMemoryInfo(memInfo)
                val availSysMemMb = memInfo.availMem / (1024 * 1024)
                val sysThresholdMb = memInfo.threshold / (1024 * 1024)
                val isLowMemory = memInfo.lowMemory

                Log.d(
                    TAG,
                    "[$processName | Sample #$sampleIndex @ ${System.currentTimeMillis() - startTime}ms] " +
                            "Heap: Used=${usedHeapMb}MB, Allocated=${totalHeapMb}MB, Max=${maxHeapMb}MB | " +
                            "SysMem: Avail=${availSysMemMb}MB, Threshold=${sysThresholdMb}MB, LowMem=$isLowMemory"
                )

                delay(intervalMs)
            }

            Log.i(TAG, "Completed memory monitoring session for process '$processName'")
            isMonitoring.set(false)
        }
    }

    /**
     * Explicitly stops monitoring.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        isMonitoring.set(false)
        Log.i(TAG, "MemoryDebugger monitoring stopped.")
    }

    private fun getProcessName(context: Context): String {
        return try {
            val pid = Process.myPid()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.runningAppProcesses?.find { it.pid == pid }?.processName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}

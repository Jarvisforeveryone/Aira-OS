package com.example.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized manager for handling application and activity lifecycle events.
 */
object LifecycleObserverManager : Application.ActivityLifecycleCallbacks {

    private var runningActivities = 0
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    private val listeners = mutableListOf<AppLifecycleListener>()

    interface AppLifecycleListener {
        fun onAppForegrounded() {}
        fun onAppBackgrounded() {}
    }

    fun registerListener(listener: AppLifecycleListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    fun unregisterListener(listener: AppLifecycleListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        runningActivities++
        if (runningActivities == 1) {
            _isAppInForeground.value = true
            Logger.d("LifecycleObserverManager", "Application moved to foreground")
            synchronized(listeners) {
                listeners.forEach { it.onAppForegrounded() }
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        runningActivities--
        if (runningActivities <= 0) {
            runningActivities = 0
            _isAppInForeground.value = false
            Logger.d("LifecycleObserverManager", "Application moved to background")
            synchronized(listeners) {
                listeners.forEach { it.onAppBackgrounded() }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

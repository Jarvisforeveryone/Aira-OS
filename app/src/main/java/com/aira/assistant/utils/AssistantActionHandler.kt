package com.aira.assistant.utils

interface AssistantActionHandler {
    fun speakText(text: String) {}
    fun addVoiceCommandLog(command: String, matchedTrigger: String?, status: String, details: String) {}
    fun toggleWifiAccessibilityFallback(enable: Boolean): String = "Wi-Fi toggled"
    fun toggleBluetoothAccessibilityFallback(enable: Boolean): String = "Bluetooth toggled"
    fun setSystemAlarm(hour: Int, minute: Int, label: String): String = "Alarm set for $hour:$minute"
    fun toggleFlashlight(enable: Boolean): String = if (enable) "Flashlight turned on" else "Flashlight turned off"
    fun setSoundMode(ringerMode: Int): String = "Sound mode updated"
    fun setSoundMode(mode: String): String = "Sound mode set to $mode"
    fun setDoNotDisturb(enable: Boolean): String = if (enable) "Do Not Disturb enabled" else "Do Not Disturb disabled"
    fun triggerHomeAction(): Boolean = false
    fun triggerBackAction(): Boolean = false
    fun triggerRecentsAction(): Boolean = false
    fun lockDeviceScreen(): String = "Device locked"
    fun checkDeviceAdminActive(): Boolean = false
    fun launchSystemCamera(): Boolean = false
    fun initiatePhoneCall(number: String): Boolean = false
    fun triggerMorningBriefing() {}
}

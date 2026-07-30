package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AiraDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("AiraDeviceAdmin", "Device Policy Administration Enabled for Aira Core")
        Toast.makeText(context, "Admin On ✅", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("AiraDeviceAdmin", "Device Policy Administration Disabled")
        Toast.makeText(context, "Aira Device Policy Admin Deactivated", Toast.LENGTH_SHORT).show()
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {
        super.onPasswordChanged(context, intent)
        Log.d("AiraDeviceAdmin", "Device lock password/PIN updated")
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.w("AiraDeviceAdmin", "Device unlock attempt failed")
    }
}

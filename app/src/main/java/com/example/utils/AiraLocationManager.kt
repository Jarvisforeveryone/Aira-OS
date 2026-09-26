package com.example.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.resume

data class DetectedLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val countryName: String,
    val isGpsLocation: Boolean,
    val sourceDescription: String
)

object AiraLocationManager {

    private const val TAG = "AiraLocationManager"

    suspend fun getBestLocation(
        context: Context,
        okHttpClient: OkHttpClient
    ): DetectedLocation? = withContext(Dispatchers.IO) {
        // Step 1: Check permissions for GPS / Network
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            // Try FusedLocationProviderClient first
            val fusedLoc = getFusedLocation(context)
            if (fusedLoc != null) {
                val geo = reverseGeocode(context, fusedLoc.latitude, fusedLoc.longitude)
                val city = geo.first.ifBlank { "Current Location" }
                val country = geo.second
                Log.i(TAG, "FusedLocation detected: ($city, $country) ${fusedLoc.latitude}, ${fusedLoc.longitude}")
                return@withContext DetectedLocation(
                    latitude = fusedLoc.latitude,
                    longitude = fusedLoc.longitude,
                    cityName = city,
                    countryName = country,
                    isGpsLocation = true,
                    sourceDescription = "GPS / Fused Provider"
                )
            }

            // Try System LocationManager (GPS / Network / Passive)
            val sysLoc = getSystemLocation(context)
            if (sysLoc != null) {
                val geo = reverseGeocode(context, sysLoc.latitude, sysLoc.longitude)
                val city = geo.first.ifBlank { "Current Location" }
                val country = geo.second
                Log.i(TAG, "System Location detected: ($city, $country) ${sysLoc.latitude}, ${sysLoc.longitude}")
                return@withContext DetectedLocation(
                    latitude = sysLoc.latitude,
                    longitude = sysLoc.longitude,
                    cityName = city,
                    countryName = country,
                    isGpsLocation = true,
                    sourceDescription = "System Location Provider"
                )
            }
        }

        // Step 2: Fallback to IP-based geolocation (ip-api.com)
        Log.i(TAG, "GPS / System location unavailable. Attempting IP Geolocation fallback...")
        val ipLoc = getIpLocation(okHttpClient)
        if (ipLoc != null) {
            Log.i(TAG, "IP Geolocation detected: ${ipLoc.cityName}, ${ipLoc.countryName} (${ipLoc.latitude}, ${ipLoc.longitude})")
            return@withContext ipLoc
        }

        Log.w(TAG, "All location detection methods (Fused, System, IP) failed.")
        null
    }

    private suspend fun getFusedLocation(context: Context): Location? {
        return try {
            val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            
            // First check lastLocation
            val lastLoc = suspendCancellableCoroutine<Location?> { cont ->
                try {
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                        .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
                } catch (e: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                }
            }

            if (lastLoc != null && (System.currentTimeMillis() - lastLoc.time) < 30 * 60 * 1000) {
                return lastLoc
            }

            // Request current location with timeout
            withTimeoutOrNull(5000L) {
                val cts = CancellationTokenSource()
                suspendCancellableCoroutine<Location?> { cont ->
                    try {
                        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                            .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                            .addOnFailureListener { if (cont.isActive) cont.resume(lastLoc) }
                            .addOnCanceledListener { if (cont.isActive) cont.resume(lastLoc) }
                    } catch (e: SecurityException) {
                        if (cont.isActive) cont.resume(lastLoc)
                    }
                }
            } ?: lastLoc
        } catch (e: Throwable) {
            Log.w(TAG, "FusedLocationProviderClient query failed: ${e.message}")
            null
        }
    }

    private fun getSystemLocation(context: Context): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOfNotNull(
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else null,
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) LocationManager.NETWORK_PROVIDER else null,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLoc: Location? = null
            for (provider in providers) {
                try {
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLoc == null || loc.time > bestLoc.time) {
                        bestLoc = loc
                    }
                } catch (e: SecurityException) {
                    // Ignore missing permission on specific provider
                }
            }
            bestLoc
        } catch (e: Throwable) {
            Log.w(TAG, "System LocationManager failed: ${e.message}")
            null
        }
    }

    private fun getIpLocation(okHttpClient: OkHttpClient): DetectedLocation? {
        // Try ip-api.com first
        try {
            val request = Request.Builder()
                .url("http://ip-api.com/json/?fields=status,message,country,city,lat,lon")
                .header("User-Agent", "AIRA-Android-Assistant/1.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        if (json.optString("status") == "success") {
                            val lat = json.getDouble("lat")
                            val lon = json.getDouble("lon")
                            val city = json.optString("city", "Local Region")
                            val country = json.optString("country", "")
                            return DetectedLocation(
                                latitude = lat,
                                longitude = lon,
                                cityName = city,
                                countryName = country,
                                isGpsLocation = false,
                                sourceDescription = "IP Geolocation (ip-api.com)"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ip-api.com query failed: ${e.message}")
        }

        // Fallback to ipapi.co
        try {
            val request = Request.Builder()
                .url("https://ipapi.co/json/")
                .header("User-Agent", "AIRA-Android-Assistant/1.0")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val lat = json.optDouble("latitude", Double.NaN)
                        val lon = json.optDouble("longitude", Double.NaN)
                        if (!lat.isNaN() && !lon.isNaN()) {
                            val city = json.optString("city", "Local Region")
                            val country = json.optString("country_name", "")
                            return DetectedLocation(
                                latitude = lat,
                                longitude = lon,
                                cityName = city,
                                countryName = country,
                                isGpsLocation = false,
                                sourceDescription = "IP Geolocation (ipapi.co)"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ipapi.co query failed: ${e.message}")
        }

        return null
    }

    fun reverseGeocode(context: Context, lat: Double, lon: Double): Pair<String, String> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                val country = addr.countryName ?: ""
                Pair(city, country)
            } else {
                Pair("", "")
            }
        } catch (e: Exception) {
            Pair("", "")
        }
    }
}

package com.binaryapp.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.binaryapp.data.remote.SupabaseClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Completely autonomous background location tracker.
 * Prompts for permissions, prompts for GPS enablement, and then continuously
 * pushes location updates to the Supabase backend with zero UI freezing.
 */
class AutoLocationTracker(
    private val activity: AppCompatActivity,
    private val sessionManager: SessionManager
) {

    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    
    // Request updates every 3 minutes (180,000ms), fastest every 30s.
    // 100m displacement threshold — accurate enough without excessive battery drain.
    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 180000L)
        .setMinUpdateIntervalMillis(30000L)
        .setMinUpdateDistanceMeters(100f)
        .build()

    private val permissionLauncher: ActivityResultLauncher<Array<String>>
    private val gpsResolutionLauncher: ActivityResultLauncher<IntentSenderRequest>

    init {
        // ActivityResultLaunchers MUST be registered in init() or onCreate()
        permissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineGranted || coarseGranted) {
                checkGpsAndStart()
            } else {
                Log.w("AutoLocationTracker", "Location permission permanently denied by user.")
            }
        }

        gpsResolutionLauncher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                Log.d("AutoLocationTracker", "User enabled GPS successfully.")
                startLocationUpdates()
            } else {
                Log.w("AutoLocationTracker", "User declined to enable GPS.")
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            val loc = result.lastLocation ?: return
            
            // Only sync to database if the user is authenticated
            if (!sessionManager.isLoggedIn || sessionManager.userId == -1L) {
                stopTracking()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Reverse geocode to get a readable city name efficiently
                    val cityState = LocationHelper.geocodeLocation(activity, loc.latitude, loc.longitude) 
                    
                    val payload = JSONObject().apply {
                        put("user_id", sessionManager.userId)
                        put("latitude", loc.latitude)
                        put("longitude", loc.longitude)
                        put("accuracy", loc.accuracy)
                        put("city_state", cityState)
                        put("last_updated", System.currentTimeMillis())
                    }.toString()

                    // Upsert into Supabase user_locations table
                    SupabaseClient.post("user_locations", payload, upsert = true)
                    Log.d("AutoLocationTracker", "Location synced to backend successfully: $cityState")
                } catch (e: Exception) {
                    Log.e("AutoLocationTracker", "Failed to sync location to backend: ${e.message}")
                }
            }
        }
    }

    /**
     * Entry point to start the automatic flow.
     */
    fun startTrackingFlow() {
        if (!sessionManager.isLoggedIn) return

        val hasFine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            checkGpsAndStart()
        } else {
            // Request standard Android permissions
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun checkGpsAndStart() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // GPS is already ON
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // GPS is OFF, but we can launch the Android system prompt to enable it
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    gpsResolutionLauncher.launch(intentSenderRequest)
                } catch (sendEx: Exception) {
                    Log.e("AutoLocationTracker", "Error showing GPS enablement prompt: ${sendEx.message}")
                }
            } else {
                Log.e("AutoLocationTracker", "Location settings are inadequate, and cannot be fixed automatically.")
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Log.d("AutoLocationTracker", "Live background location tracking active.")
        } catch (e: SecurityException) {
            Log.e("AutoLocationTracker", "Lost location permission while attempting to start updates.")
        }
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        Log.d("AutoLocationTracker", "Live background location tracking stopped.")
    }
}

package com.binaryapp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * FIX #6: LocationHelper - Provides accurate device location using FusedLocationProvider.
 *
 * Strategy:
 * 1. Check runtime permissions (ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION).
 * 2. Request a fresh location from FusedLocationProvider (GPS or network).
 * 3. Reverse-geocode coordinates to City, State via Geocoder.
 * 4. Fall back to last known location if fresh fetch fails.
 * 5. Return "Location unavailable" gracefully if all methods fail or no permission.
 *
 * This replaces the hardcoded "Bangalore, India" throughout the app.
 */
object LocationHelper {

    private const val TAG = "LocationHelper"
    private const val FALLBACK_LOCATION = "Location unavailable"

    /**
     * Fetches the device's current location and reverse-geocodes it to a human-readable string.
     * This is a suspend function; call it from a coroutine (e.g., inside viewModelScope.launch).
     *
     * @param context Application or Activity context.
     * @return A string like "Chennai, Tamil Nadu" or "Location unavailable".
     */
    suspend fun getCurrentLocationString(context: Context): String {
        // Check that we have at least coarse location permission
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "Location permission not granted, returning fallback.")
            return "Location unavailable (no permission)"
        }

        return withContext(Dispatchers.IO) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cancellationTokenSource = CancellationTokenSource()

                // Priority: BALANCED for battery efficiency, HIGH_ACCURACY if fine permission granted
                val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY
                               else Priority.PRIORITY_BALANCED_POWER_ACCURACY

                // Request a fresh current location (not stale cache)
                val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
                    fusedClient.getCurrentLocation(priority, cancellationTokenSource.token)
                        .addOnSuccessListener { loc -> 
                            if (loc != null) {
                                cont.resume(loc)
                            } else {
                                Log.w(TAG, "getCurrentLocation returned null loc. Trying lastLocation.")
                                fusedClient.lastLocation
                                    .addOnSuccessListener { last -> cont.resume(last) }
                                    .addOnFailureListener { cont.resume(null) }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "getCurrentLocation failed: ${e.message}. Trying lastLocation.")
                            // Fall back to last known location
                            fusedClient.lastLocation
                                .addOnSuccessListener { last -> cont.resume(last) }
                                .addOnFailureListener { cont.resume(null) }
                        }

                    cont.invokeOnCancellation { cancellationTokenSource.cancel() }
                }

                if (location == null) {
                    Log.w(TAG, "No location available from FusedLocationProvider.")
                    return@withContext FALLBACK_LOCATION
                }

                // Reverse-geocode the coordinates to city/state
                geocodeLocation(context, location.latitude, location.longitude)

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException accessing location: ${e.message}")
                FALLBACK_LOCATION
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error fetching location: ${e.message}")
                FALLBACK_LOCATION
            }
        }
    }

    /**
     * Converts latitude/longitude into a readable "City, State" string using Geocoder.
     * Falls back to coordinate string if geocoding fails.
     */
    private suspend fun geocodeLocation(context: Context, lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    Log.w(TAG, "Geocoder not available, returning coordinates.")
                    return@withContext "%.4f, %.4f (approx)".format(lat, lon)
                }

                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Use the async API on Android 13+
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { addresses ->
                            val result = buildLocationString(addresses, lat, lon)
                            cont.resume(result)
                        }
                    }
                } else {
                    // Use the synchronous API on older versions
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    buildLocationString(addresses ?: emptyList(), lat, lon)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoding failed: ${e.message}")
                "%.4f, %.4f (approx)".format(lat, lon)
            }
        }
    }

    /**
     * Builds a readable location string from a list of Geocoder Address objects.
     * Prefers City + State; falls back to coordinates if no usable address.
     */
    private fun buildLocationString(
        addresses: List<android.location.Address>,
        lat: Double,
        lon: Double
    ): String {
        val address = addresses.firstOrNull() ?: return "%.4f, %.4f (approx)".format(lat, lon)

        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
            ?: ""
        val state = address.adminArea ?: ""
        val country = address.countryName ?: ""

        return when {
            city.isNotBlank() && state.isNotBlank() -> "$city, $state"
            city.isNotBlank() && country.isNotBlank() -> "$city, $country"
            state.isNotBlank() -> "$state, $country"
            country.isNotBlank() -> country
            else -> "%.4f, %.4f (approx)".format(lat, lon)
        }
    }
}

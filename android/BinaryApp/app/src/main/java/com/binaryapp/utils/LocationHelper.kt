package com.binaryapp.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * LocationHelper — Fast, reliable device location using FusedLocationProvider.
 *
 * Strategy (fastest-first):
 * 1. Check permissions.
 * 2. Try lastLocation (instant, no GPS warmup needed).
 * 3. If lastLocation is null/stale, request a fresh HIGH_ACCURACY fix with a 10s timeout.
 * 4. Reverse-geocode via Geocoder.
 * 5. Graceful fallback at every step.
 */
object LocationHelper {

    private const val TAG = "LocationHelper"
    private const val FALLBACK_LOCATION = "Location unavailable"
    private const val FRESH_LOCATION_TIMEOUT_MS = 10_000L // 10 seconds max wait

    /**
     * Fetches the device's current location as fast as possible and reverse-geocodes it.
     * Call from a coroutine (viewLifecycleOwner.lifecycleScope.launch).
     */
    suspend fun getCurrentLocationString(context: Context): String {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "Location permission not granted.")
            return "Location unavailable (no permission)"
        }

        return withContext(Dispatchers.IO) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)

                // ── FAST PATH: Try lastLocation first (instant, no GPS warmup) ──
                val lastLoc = suspendCancellableCoroutine<android.location.Location?> { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }

                if (lastLoc != null) {
                    Log.d(TAG, "Using lastLocation: ${lastLoc.latitude}, ${lastLoc.longitude}")
                    val result = geocodeLocation(context, lastLoc.latitude, lastLoc.longitude)
                    // Only accept last location if geocoding gave something meaningful
                    if (!result.contains("approx") && result != FALLBACK_LOCATION) {
                        return@withContext result
                    }
                }

                // ── FRESH FIX: Request current location with HIGH_ACCURACY + 10s timeout ──
                Log.d(TAG, "lastLocation null or insufficient, requesting fresh fix...")
                val cancellationTokenSource = CancellationTokenSource()

                val freshLoc = withTimeoutOrNull(FRESH_LOCATION_TIMEOUT_MS) {
                    suspendCancellableCoroutine<android.location.Location?> { cont ->
                        fusedClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cancellationTokenSource.token
                        )
                            .addOnSuccessListener { loc -> cont.resume(loc) }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "getCurrentLocation failed: ${e.message}")
                                cont.resume(null)
                            }

                        cont.invokeOnCancellation { cancellationTokenSource.cancel() }
                    }
                }

                if (freshLoc != null) {
                    Log.d(TAG, "Fresh fix: ${freshLoc.latitude}, ${freshLoc.longitude}")
                    return@withContext geocodeLocation(context, freshLoc.latitude, freshLoc.longitude)
                }

                // ── LAST RESORT: Use lastLocation even if geocoding gives coordinates ──
                if (lastLoc != null) {
                    Log.w(TAG, "Falling back to raw lastLocation coordinates.")
                    return@withContext geocodeLocation(context, lastLoc.latitude, lastLoc.longitude)
                }

                Log.w(TAG, "All location methods failed.")
                FALLBACK_LOCATION

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: ${e.message}")
                FALLBACK_LOCATION
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}")
                FALLBACK_LOCATION
            }
        }
    }

    /**
     * Reverse-geocodes lat/lon to "City, State" string.
     * Falls back to coordinate string if Geocoder unavailable or fails.
     */
    suspend fun geocodeLocation(context: Context, lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) {
                    Log.w(TAG, "Geocoder not present.")
                    return@withContext "%.4f, %.4f (approx)".format(lat, lon)
                }

                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { addresses ->
                            cont.resume(buildLocationString(addresses, lat, lon))
                        }
                    }
                } else {
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

    private fun buildLocationString(
        addresses: List<android.location.Address>,
        lat: Double,
        lon: Double
    ): String {
        val address = addresses.firstOrNull()
            ?: return "%.4f, %.4f (approx)".format(lat, lon)

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

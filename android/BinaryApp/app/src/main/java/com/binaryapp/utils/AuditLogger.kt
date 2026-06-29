package com.binaryapp.utils

import android.content.Context
import android.util.Log
import com.binaryapp.data.remote.SupabaseClient
import com.binaryapp.utils.DeviceUtils
import com.binaryapp.utils.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AuditLogger - Handles saving user activity logs to the Supabase database.
 * Uses fire-and-forget background coroutines so UI performance is unaffected.
 */
object AuditLogger {

    private const val TAG = "AuditLogger"

    /**
     * Logs an event asynchronously.
     * 
     * @param context Application or Activity context.
     * @param userId The ID of the user performing the action (can be null for pre-login actions).
     * @param eventType A clear, uppercase string classifying the event (e.g. "LOGIN_SUCCESS").
     * @param metadata Optional extra details about the event.
     */
    fun logEvent(
        context: Context,
        userId: Long?,
        eventType: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        // Fire and forget in IO thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch location asynchronously off the main thread
                val deviceInfo = DeviceUtils.getDeviceName()
                val location = LocationHelper.getCurrentLocationString(context)

                val metadataJson = SupabaseClient.gson.toJson(metadata)
                
                val logMap = mutableMapOf<String, Any>(
                    "event_type" to eventType,
                    "metadata" to metadataJson,
                    "device_info" to deviceInfo,
                    "location" to location,
                    "created_at" to System.currentTimeMillis()
                )
                
                if (userId != null && userId > 0) {
                    logMap["user_id"] = userId
                }

                // Push to Supabase 'audit_logs' table
                SupabaseClient.post("audit_logs", SupabaseClient.gson.toJson(logMap))
                Log.d(TAG, "Successfully logged event: $eventType")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log event: $eventType - ${e.message}")
                // Since this is an audit log, we just swallow the exception 
                // so it doesn't crash the user's flow.
            }
        }
    }
}

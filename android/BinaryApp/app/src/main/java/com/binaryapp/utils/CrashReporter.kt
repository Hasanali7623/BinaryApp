package com.binaryapp.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.binaryapp.data.remote.SupabaseClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * Enterprise-Grade Crash Reporter
 * - Accurately detects fatal crashes vs normal terminations.
 * - Collects rich telemetry asynchronously.
 * - Supports silent non-fatal logging.
 * - Minimal performance overhead.
 */
object CrashReporter : Application.ActivityLifecycleCallbacks {

    private const val TAG = "CrashReporter"
    private const val PREFS_NAME = "enterprise_crash_prefs"
    private const val KEY_PENDING_FATAL = "pending_fatal_crash"
    
    private var currentActivity: WeakReference<Activity>? = null
    private var sessionUUID: String = UUID.randomUUID().toString()

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        
        val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            handleFatalCrash(application.applicationContext, prefs, thread, exception)
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    private fun handleFatalCrash(
        context: Context,
        prefs: android.content.SharedPreferences,
        thread: Thread,
        exception: Throwable
    ) {
        try {
            val crashData = buildDiagnosticData(context, exception, thread, true)
            val jsonString = SupabaseClient.gson.toJson(crashData)
            
            // Save synchronously because process is dying
            prefs.edit().putString(KEY_PENDING_FATAL, jsonString).commit()
            Log.e(TAG, "Fatal crash intercepted and saved for next boot.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture fatal crash: ${e.message}")
        }
    }

    /**
     * Silently log non-fatal exceptions in the background without disturbing the user.
     */
    fun logNonFatal(context: Context, exception: Throwable, additionalInfo: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = buildDiagnosticData(context, exception, Thread.currentThread(), false)
                val mutableData = data.toMutableMap()
                mutableData["additional_info"] = additionalInfo
                
                val jsonString = SupabaseClient.gson.toJson(mutableData)
                SupabaseClient.post("crash_logs", jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload non-fatal report: ${e.message}")
            }
        }
    }

    private fun buildDiagnosticData(
        context: Context,
        exception: Throwable,
        thread: Thread,
        isFatal: Boolean
    ): Map<String, Any> {
        val sw = StringWriter()
        exception.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        
        val currentScreen = currentActivity?.get()?.javaClass?.simpleName ?: "Unknown"

        return mapOf(
            "app_version" to getAppVersion(context),
            "os_version" to Build.VERSION.RELEASE,
            "device_model" to DeviceUtils.getDeviceName(),
            "error_code" to exception.javaClass.simpleName,
            "exception_message" to (exception.message ?: ""),
            "stack_trace" to stackTrace,
            "thread_name" to thread.name,
            "is_fatal" to isFatal,
            "current_screen" to currentScreen,
            "session_id" to sessionUUID,
            "memory_used_mb" to usedMem,
            "timestamp" to System.currentTimeMillis()
        )
    }

    fun hasPendingFatalCrash(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).contains(KEY_PENDING_FATAL)
    }

    fun submitFatalCrashWithContext(context: Context, userExplanation: String, reproductionSteps: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingCrashJson = prefs.getString(KEY_PENDING_FATAL, null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val type = object : TypeToken<MutableMap<String, Any>>() {}.type
                val crashData: MutableMap<String, Any> = Gson().fromJson(pendingCrashJson, type)

                if (userExplanation.isNotEmpty()) crashData["user_explanation"] = userExplanation
                if (reproductionSteps.isNotEmpty()) crashData["reproduction_steps"] = reproductionSteps

                val finalJsonString = SupabaseClient.gson.toJson(crashData)
                SupabaseClient.post("crash_logs", finalJsonString)

                prefs.edit().remove(KEY_PENDING_FATAL).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload fatal crash log: ${e.message}")
            }
        }
    }

    fun ignoreAndClearCrash(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_PENDING_FATAL).apply()
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // --- ActivityLifecycleCallbacks to track current screen safely ---
    override fun onActivityResumed(activity: Activity) { currentActivity = WeakReference(activity) }
    override fun onActivityPaused(activity: Activity) { if (currentActivity?.get() == activity) currentActivity = null }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

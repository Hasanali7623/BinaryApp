package com.binaryapp.utils

import android.os.Build
import android.provider.Settings

/**
 * Utility class for device information retrieval.
 */
object DeviceUtils {

    /**
     * Get device name/model.
     */
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Get device model string.
     */
    fun getDeviceModel(): String = Build.MODEL

    /**
     * Get device brand.
     */
    fun getDeviceBrand(): String = Build.BRAND.replaceFirstChar { it.uppercase() }

    /**
     * Get Android version.
     */
    fun getAndroidVersion(): String = "Android ${Build.VERSION.RELEASE}"

    /**
     * Get a formatted device info string for sessions.
     */
    fun getDeviceInfo(): String {
        return "${getDeviceName()} | ${getAndroidVersion()}"
    }
}

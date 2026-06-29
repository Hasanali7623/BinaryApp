package com.binaryapp.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user session preferences and authentication state.
 *
 * All session data is stored in private SharedPreferences.
 * Sensitive fields (userId, email) are cleared on logout via clearSession().
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * In-memory location cache — populated once per app session after GPS resolves.
     * Cleared on logout. NOT persisted to disk (stale on next boot is fine; GPS refetches).
     */
    var cachedLocation: String? = null

    companion object {
        private const val PREF_NAME = "binary_app_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_REMEMBER_DEVICE = "remember_device"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        // FIX #2: Store which biometric type the user selected ("face_id" or "fingerprint")
        private const val KEY_BIOMETRIC_TYPE = "biometric_type"
        private const val KEY_DEVICE_TRUSTED = "device_trusted"
        private const val KEY_PENDING_EMAIL = "pending_email"
        private const val KEY_PENDING_USER_ID = "pending_user_id"
        private const val KEY_RESET_EMAIL = "reset_email"
        private const val KEY_USER_ROLE = "user_role"
    }

    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var rememberDevice: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_DEVICE, false)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_DEVICE, value).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    /**
     * FIX #2: Persist which biometric type the user explicitly chose ("face_id" or "fingerprint").
     * This allows the rest of the app to display and invoke the correct method.
     */
    var biometricType: String
        get() = prefs.getString(KEY_BIOMETRIC_TYPE, "fingerprint") ?: "fingerprint"
        set(value) = prefs.edit().putString(KEY_BIOMETRIC_TYPE, value).apply()

    var deviceTrusted: Boolean
        get() = prefs.getBoolean(KEY_DEVICE_TRUSTED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEVICE_TRUSTED, value).apply()

    var pendingEmail: String
        get() = prefs.getString(KEY_PENDING_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PENDING_EMAIL, value).apply()

    var pendingUserId: Long
        get() = prefs.getLong(KEY_PENDING_USER_ID, -1)
        set(value) = prefs.edit().putLong(KEY_PENDING_USER_ID, value).apply()

    var resetEmail: String
        get() = prefs.getString(KEY_RESET_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RESET_EMAIL, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    /**
     * Saves all fields required to restore an authenticated session across process restarts.
     * FIX #3: This is the single source of truth for session persistence.
     */
    fun saveUserSession(userId: Long, email: String, name: String, role: String = "") {
        this.userId = userId
        this.userEmail = email
        this.userName = name
        this.userRole = role
        this.isLoggedIn = true
    }

    /**
     * Clears all session data including sensitive authentication fields.
     * FIX (Additional): Ensures passwords and tokens are not retained after logout.
     */
    fun clearSession() {
        cachedLocation = null  // clear in-memory cache on logout
        prefs.edit().clear().apply()
    }
}

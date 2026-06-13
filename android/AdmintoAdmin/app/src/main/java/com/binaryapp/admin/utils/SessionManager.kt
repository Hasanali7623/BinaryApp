package com.binaryapp.admin.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("admin_session", Context.MODE_PRIVATE)

    var currentUserId: Long
        get() = prefs.getLong("userId", -1L)
        set(v) = prefs.edit().putLong("userId", v).apply()

    var currentOrgId: Long
        get() = prefs.getLong("orgId", -1L)
        set(v) = prefs.edit().putLong("orgId", v).apply()

    var pendingEmail: String
        get() = prefs.getString("pendingEmail", "") ?: ""
        set(v) = prefs.edit().putString("pendingEmail", v).apply()

    var pendingOtp: String
        get() = prefs.getString("pendingOtp", "") ?: ""
        set(v) = prefs.edit().putString("pendingOtp", v).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("isLoggedIn", false)
        set(v) = prefs.edit().putBoolean("isLoggedIn", v).apply()

    var setupStep: Int
        get() = prefs.getInt("setupStep", 0)
        set(v) = prefs.edit().putInt("setupStep", v).apply()

    fun clear() = prefs.edit().clear().apply()
}

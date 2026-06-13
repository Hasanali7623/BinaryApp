package com.binaryapp.admin.utils

import android.util.Patterns

object ValidationUtils {
    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isValidPassword(password: String): Boolean = password.length >= 8

    fun passwordStrength(password: String): Int {
        var score = 0
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return score // 0-5
    }

    fun isValidUrl(url: String): Boolean =
        url.isNotBlank() && !url.contains(" ") && url.matches(Regex("[a-z0-9-]+"))
}

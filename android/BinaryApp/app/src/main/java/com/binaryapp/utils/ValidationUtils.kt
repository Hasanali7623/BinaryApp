package com.binaryapp.utils

import android.util.Patterns

/**
 * Validation utility functions for form inputs.
 */
object ValidationUtils {

    /**
     * Validate email format.
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate password strength.
     * Returns a PasswordStrength enum value.
     */
    fun getPasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.WEAK
        var score = 0
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        if (password.length >= 12) score++
        return when {
            score <= 2 -> PasswordStrength.WEAK
            score == 3 -> PasswordStrength.MEDIUM
            score == 4 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    /**
     * Check individual password requirements.
     */
    fun hasMinLength(password: String) = password.length >= 8
    fun hasUppercase(password: String) = password.any { it.isUpperCase() }
    fun hasNumber(password: String) = password.any { it.isDigit() }
    fun hasSpecialChar(password: String) = password.any { !it.isLetterOrDigit() }

    /**
     * Validate that two passwords match.
     */
    fun passwordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    /**
     * Validate full name.
     */
    fun isValidFullName(name: String): Boolean {
        return name.trim().length >= 2 && name.trim().contains(" ")
    }

    enum class PasswordStrength(val label: String, val progress: Int) {
        WEAK("Weak", 25),
        MEDIUM("Medium", 50),
        STRONG("Strong", 75),
        VERY_STRONG("Very Strong", 100)
    }
}

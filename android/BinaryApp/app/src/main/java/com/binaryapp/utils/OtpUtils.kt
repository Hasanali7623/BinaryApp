package com.binaryapp.utils

import kotlin.random.Random

/**
 * Utility class for OTP generation and validation.
 */
object OtpUtils {

    /**
     * Generate a 6-digit OTP code.
     */
    fun generateOtp(): String {
        return (100000..999999).random().toString()
    }

    /**
     * Generate a 6-character alphanumeric pairing code.
     */
    fun generatePairingCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    /**
     * Validate OTP format (6 digits).
     */
    fun isValidOtpFormat(otp: String): Boolean {
        return otp.length == 6 && otp.all { it.isDigit() }
    }
}

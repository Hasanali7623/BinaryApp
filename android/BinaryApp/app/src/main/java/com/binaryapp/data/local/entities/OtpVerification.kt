package com.binaryapp.data.local.entities

/**
 * OTP Verification entity.
 * Stores OTP codes with expiry information.
 */
data class OtpVerification(
    val id: Long = 0,
    val userId: Long,
    val otpCode: String,
    val expiresAt: Long,
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

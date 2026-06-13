package com.binaryapp.data.local.entities

/**
 * Trusted Device entity.
 * Stores information about trusted devices for bypass verification.
 */
data class TrustedDevice(
    val id: Long = 0,
    val userId: Long,
    val deviceName: String,
    val deviceModel: String,
    val browser: String = "BinaryApp Mobile",
    val location: String = "Unknown",
    val lastLogin: Long = System.currentTimeMillis(),
    val trusted: Boolean = true
)

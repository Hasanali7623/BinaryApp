package com.binaryapp.data.local.entities

/**
 * Session entity.
 * Tracks user login sessions and device information.
 */
data class Session(
    val id: Long = 0,
    val userId: Long,
    val loginTime: Long = System.currentTimeMillis(),
    val logoutTime: Long? = null,
    val deviceInfo: String,
    val location: String = "Unknown",
    val isActive: Boolean = true
)

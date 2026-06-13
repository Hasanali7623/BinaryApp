package com.binaryapp.data.local.entities

/**
 * User entity.
 * Stores user registration and profile information.
 */
data class User(
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val role: String,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

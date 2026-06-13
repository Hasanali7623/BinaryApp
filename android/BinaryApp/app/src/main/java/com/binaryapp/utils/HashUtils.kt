package com.binaryapp.utils

import java.security.MessageDigest

/**
 * Utility class for password hashing operations.
 * Uses SHA-256 with salt for secure password storage.
 */
object HashUtils {

    private const val SALT = "BinaryApp_2024_SecureS@lt!"

    /**
     * Hash a password using SHA-256 with a static salt.
     * In production, use bcrypt or Argon2 with per-user salt.
     */
    fun hashPassword(password: String): String {
        val saltedPassword = "$SALT:$password"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(saltedPassword.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify a password against a stored hash.
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
}

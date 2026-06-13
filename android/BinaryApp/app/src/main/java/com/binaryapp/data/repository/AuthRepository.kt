package com.binaryapp.data.repository

import com.binaryapp.data.local.entities.OtpVerification
import com.binaryapp.data.local.entities.Session
import com.binaryapp.data.local.entities.TrustedDevice
import com.binaryapp.data.local.entities.User
import com.binaryapp.data.remote.SupabaseClient
import com.binaryapp.utils.HashUtils
import com.binaryapp.utils.OtpUtils
import com.google.gson.reflect.TypeToken

/**
 * Authentication Repository.
 * Single source of truth for authentication-related operations.
 * Interacts with Supabase PostgreSQL instead of local SQLite.
 */
class AuthRepository {

    // ─── Registration ──────────────────────────────────────────────────────────

    suspend fun registerUser(
        fullName: String,
        email: String,
        password: String,
        role: String
    ): Result<User> {
        return try {
            val json = SupabaseClient.get("users", mapOf("email" to "eq.$email", "limit" to "1"))
            val list: List<User> = SupabaseClient.gson.fromJson(json, object : TypeToken<List<User>>() {}.type)
            if (list.isNotEmpty()) {
                Result.failure(Exception("Email already registered"))
            } else {
                val passwordHash = HashUtils.hashPassword(password)
                val userMap = mapOf(
                    "fullName" to fullName,
                    "email" to email,
                    "passwordHash" to passwordHash,
                    "role" to role,
                    "isVerified" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                val responseJson = SupabaseClient.post("users", SupabaseClient.gson.toJson(userMap), preferRepresentation = true)
                val insertedList: List<User> = SupabaseClient.gson.fromJson(responseJson, object : TypeToken<List<User>>() {}.type)
                val insertedUser = insertedList.firstOrNull() ?: return Result.failure(Exception("Failed to create user"))
                Result.success(insertedUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val passwordHash = HashUtils.hashPassword(password)
            val json = SupabaseClient.get("users", mapOf(
                "email" to "eq.$email",
                "passwordHash" to "eq.$passwordHash",
                "limit" to "1"
            ))
            val list: List<User> = SupabaseClient.gson.fromJson(json, object : TypeToken<List<User>>() {}.type)
            val user = list.firstOrNull() ?: return Result.failure(Exception("Invalid email or password"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── OTP ───────────────────────────────────────────────────────────────────

    suspend fun generateAndSaveOtp(userId: Long): String {
        val otpCode = OtpUtils.generateOtp()
        val expiresAt = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
        val otpMap = mapOf(
            "userId" to userId,
            "otpCode" to otpCode,
            "expiresAt" to expiresAt,
            "isUsed" to false,
            "createdAt" to System.currentTimeMillis()
        )
        try {
            SupabaseClient.delete("otp_verifications", mapOf("userId" to "eq.$userId"))
        } catch (e: Exception) {
            // Ignore if none existed
        }
        SupabaseClient.post("otp_verifications", SupabaseClient.gson.toJson(otpMap))
        return otpCode
    }

    suspend fun verifyOtp(userId: Long, code: String): Result<Boolean> {
        return try {
            val json = SupabaseClient.get("otp_verifications", mapOf(
                "userId" to "eq.$userId",
                "otpCode" to "eq.$code",
                "isUsed" to "eq.false",
                "limit" to "1"
            ))
            val list: List<OtpVerification> = SupabaseClient.gson.fromJson(json, object : TypeToken<List<OtpVerification>>() {}.type)
            val otp = list.firstOrNull() ?: return Result.failure(Exception("Invalid verification code"))
            if (System.currentTimeMillis() > otp.expiresAt) {
                return Result.failure(Exception("Verification code expired"))
            }
            val updateOtpMap = mapOf("isUsed" to true)
            SupabaseClient.patch("otp_verifications", SupabaseClient.gson.toJson(updateOtpMap), mapOf("id" to "eq.${otp.id}"))
            val updateUserMap = mapOf("isVerified" to true)
            SupabaseClient.patch("users", SupabaseClient.gson.toJson(updateUserMap), mapOf("id" to "eq.$userId"))
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Password Reset ────────────────────────────────────────────────────────

    suspend fun initiatePasswordReset(email: String): Result<Long> {
        return try {
            val json = SupabaseClient.get("users", mapOf("email" to "eq.$email", "limit" to "1"))
            val list: List<User> = SupabaseClient.gson.fromJson(json, object : TypeToken<List<User>>() {}.type)
            val user = list.firstOrNull() ?: return Result.failure(Exception("Email not found"))
            generateAndSaveOtp(user.id)
            Result.success(user.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(email: String, newPassword: String): Result<Boolean> {
        return try {
            val passwordHash = HashUtils.hashPassword(newPassword)
            val updateMap = mapOf("passwordHash" to passwordHash)
            SupabaseClient.patch("users", SupabaseClient.gson.toJson(updateMap), mapOf("email" to "eq.$email"))
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Trusted Device ────────────────────────────────────────────────────────

    suspend fun trustDevice(
        userId: Long,
        deviceName: String,
        deviceModel: String,
        location: String
    ): Result<TrustedDevice> {
        return try {
            val deviceMap = mapOf(
                "userId" to userId,
                "deviceName" to deviceName,
                "deviceModel" to deviceModel,
                "browser" to "BinaryApp Mobile",
                "location" to location,
                "lastLogin" to System.currentTimeMillis(),
                "trusted" to true
            )
            val responseJson = SupabaseClient.post("trusted_devices", SupabaseClient.gson.toJson(deviceMap), preferRepresentation = true)
            val insertedList: List<TrustedDevice> = SupabaseClient.gson.fromJson(responseJson, object : TypeToken<List<TrustedDevice>>() {}.type)
            val insertedDevice = insertedList.firstOrNull() ?: return Result.failure(Exception("Failed to trust device"))
            Result.success(insertedDevice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isDeviceTrusted(userId: Long, deviceModel: String): Boolean {
        return try {
            val json = SupabaseClient.get("trusted_devices", mapOf(
                "userId" to "eq.$userId",
                "deviceModel" to "eq.$deviceModel",
                "trusted" to "eq.true"
            ))
            val list: List<TrustedDevice> = SupabaseClient.gson.fromJson(json, object : TypeToken<List<TrustedDevice>>() {}.type)
            list.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // ─── Sessions ──────────────────────────────────────────────────────────────

    suspend fun createSession(userId: Long, deviceInfo: String, location: String): Long {
        val sessionMap = mapOf(
            "userId" to userId,
            "loginTime" to System.currentTimeMillis(),
            "deviceInfo" to deviceInfo,
            "location" to location,
            "isActive" to true
        )
        val responseJson = SupabaseClient.post("sessions", SupabaseClient.gson.toJson(sessionMap), preferRepresentation = true)
        val insertedList: List<Session> = SupabaseClient.gson.fromJson(responseJson, object : TypeToken<List<Session>>() {}.type)
        val insertedSession = insertedList.firstOrNull() ?: throw Exception("Failed to create session")
        return insertedSession.id
    }

    suspend fun endSession(userId: Long) {
        try {
            val updateMap = mapOf(
                "logoutTime" to System.currentTimeMillis(),
                "isActive" to false
            )
            SupabaseClient.patch("sessions", SupabaseClient.gson.toJson(updateMap), mapOf(
                "userId" to "eq.$userId",
                "isActive" to "eq.true"
            ))
        } catch (e: Exception) {
            // Ignore failure on end session
        }
    }

    suspend fun getActiveSession(userId: Long): Session? {
        return try {
            val json = SupabaseClient.get("sessions", mapOf(
                "userId" to "eq.$userId",
                "isActive" to "eq.true",
                "order" to "loginTime.desc",
                "limit" to "1"
            ))
            val list: List<Session> = SupabaseClient.gson.fromJson(json, object : TypeToken<List<Session>>() {}.type)
            list.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // ─── User Queries ──────────────────────────────────────────────────────────

    suspend fun getUserById(userId: Long): User? {
        return try {
            val json = SupabaseClient.get("users", mapOf("id" to "eq.$userId", "limit" to "1"))
            val list: List<User> = SupabaseClient.gson.fromJson(json, object : TypeToken<List<User>>() {}.type)
            list.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            val json = SupabaseClient.get("users", mapOf("email" to "eq.$email", "limit" to "1"))
            val list: List<User> = SupabaseClient.gson.fromJson(json, object : TypeToken<List<User>>() {}.type)
            list.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
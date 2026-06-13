package com.binaryapp.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_policies")
data class SecurityPolicy(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val requireMfa: Boolean = true,
    val passwordRotation: Boolean = false,
    val sessionTimeout: Int = 30,
    val biometricEnabled: Boolean = false,
    val deviceTrustEnabled: Boolean = true,
    val ipRestrictions: Boolean = false,
    val loginMonitoring: Boolean = true,
    val minPasswordLength: Int = 12,
    val requireUppercase: Boolean = true,
    val requireNumbers: Boolean = true,
    val requireSpecialChars: Boolean = true,
    val passwordHistory: Int = 5,
    val updatedAt: Long = System.currentTimeMillis()
)

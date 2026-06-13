package com.binaryapp.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verifications")
data class Verification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val otpCode: String,
    val expiresAt: Long,
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

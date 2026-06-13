package com.binaryapp.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_users")
data class AdminUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val role: String = "Super Admin",
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

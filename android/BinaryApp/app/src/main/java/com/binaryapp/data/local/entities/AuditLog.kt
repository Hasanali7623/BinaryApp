package com.binaryapp.data.local.entities

data class AuditLog(
    val id: Long = 0,
    val userId: Long?,
    val eventType: String,
    val metadata: String, // Stored as a JSON string
    val deviceInfo: String,
    val location: String,
    val createdAt: Long = System.currentTimeMillis()
)

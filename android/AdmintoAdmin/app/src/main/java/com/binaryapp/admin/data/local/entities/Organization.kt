package com.binaryapp.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "organizations")
data class Organization(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyName: String,
    val domain: String = "",
    val industry: String = "",
    val companySize: String = "",
    val country: String = "",
    val timezone: String = "",
    val logoPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

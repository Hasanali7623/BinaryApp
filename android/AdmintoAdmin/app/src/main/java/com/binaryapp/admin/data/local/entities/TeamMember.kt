package com.binaryapp.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_members")
data class TeamMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val name: String = "",
    val email: String,
    val role: String,
    val department: String = "",
    val invitationStatus: String = "Pending",
    val createdAt: Long = System.currentTimeMillis()
)

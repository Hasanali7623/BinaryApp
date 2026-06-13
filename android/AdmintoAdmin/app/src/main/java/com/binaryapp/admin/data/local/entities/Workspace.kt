package com.binaryapp.admin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class Workspace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val workspaceName: String,
    val workspaceUrl: String,
    val createdAt: Long = System.currentTimeMillis()
)

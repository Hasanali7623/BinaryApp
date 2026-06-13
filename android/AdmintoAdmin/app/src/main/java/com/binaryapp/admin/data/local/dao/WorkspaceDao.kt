package com.binaryapp.admin.data.local.dao

import androidx.room.*
import com.binaryapp.admin.data.local.entities.Workspace

@Dao
interface WorkspaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workspace: Workspace): Long

    @Update
    suspend fun update(workspace: Workspace)

    @Query("SELECT * FROM workspaces WHERE organizationId = :orgId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getByOrg(orgId: Long): Workspace?

    @Query("SELECT * FROM workspaces WHERE id = :id")
    suspend fun getById(id: Long): Workspace?
}

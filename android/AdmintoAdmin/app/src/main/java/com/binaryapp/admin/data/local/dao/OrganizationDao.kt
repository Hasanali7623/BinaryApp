package com.binaryapp.admin.data.local.dao

import androidx.room.*
import com.binaryapp.admin.data.local.entities.Organization

@Dao
interface OrganizationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(org: Organization): Long

    @Update
    suspend fun update(org: Organization)

    @Query("SELECT * FROM organizations WHERE id = :id")
    suspend fun getById(id: Long): Organization?

    @Query("SELECT * FROM organizations ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): Organization?

    @Query("DELETE FROM organizations WHERE id = :id")
    suspend fun delete(id: Long)
}

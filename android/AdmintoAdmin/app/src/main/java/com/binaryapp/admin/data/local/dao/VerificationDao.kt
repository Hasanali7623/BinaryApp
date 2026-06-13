package com.binaryapp.admin.data.local.dao

import androidx.room.*
import com.binaryapp.admin.data.local.entities.Verification

@Dao
interface VerificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(v: Verification): Long

    @Query("SELECT * FROM verifications WHERE email = :email AND isUsed = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForEmail(email: String): Verification?

    @Query("UPDATE verifications SET isUsed = 1 WHERE id = :id")
    suspend fun markUsed(id: Long)

    @Query("DELETE FROM verifications WHERE email = :email")
    suspend fun deleteForEmail(email: String)
}

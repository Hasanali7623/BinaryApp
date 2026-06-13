package com.binaryapp.admin.data.local.dao

import androidx.room.*
import com.binaryapp.admin.data.local.entities.AdminUser

@Dao
interface AdminUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: AdminUser): Long

    @Update
    suspend fun update(user: AdminUser)

    @Query("SELECT * FROM admin_users WHERE id = :id")
    suspend fun getById(id: Long): AdminUser?

    @Query("SELECT * FROM admin_users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): AdminUser?

    @Query("SELECT * FROM admin_users WHERE email = :email AND passwordHash = :hash LIMIT 1")
    suspend fun getByEmailAndPassword(email: String, hash: String): AdminUser?

    @Query("SELECT COUNT(*) FROM admin_users WHERE email = :email")
    suspend fun emailExists(email: String): Int

    @Query("UPDATE admin_users SET isVerified = 1 WHERE id = :id")
    suspend fun markVerified(id: Long)

    @Query("UPDATE admin_users SET passwordHash = :hash WHERE email = :email")
    suspend fun updatePassword(email: String, hash: String)

    @Query("SELECT * FROM admin_users WHERE organizationId = :orgId")
    suspend fun getAllByOrg(orgId: Long): List<AdminUser>
}

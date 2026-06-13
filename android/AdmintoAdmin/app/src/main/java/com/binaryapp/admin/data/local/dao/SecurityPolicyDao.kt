package com.binaryapp.admin.data.local.dao

import androidx.room.*
import com.binaryapp.admin.data.local.entities.SecurityPolicy

@Dao
interface SecurityPolicyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(policy: SecurityPolicy): Long

    @Update
    suspend fun update(policy: SecurityPolicy)

    @Query("SELECT * FROM security_policies WHERE organizationId = :orgId LIMIT 1")
    suspend fun getByOrg(orgId: Long): SecurityPolicy?
}

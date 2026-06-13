package com.binaryapp.admin.data.local.dao

import androidx.room.*
import com.binaryapp.admin.data.local.entities.TeamMember

@Dao
interface TeamMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: TeamMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<TeamMember>)

    @Update
    suspend fun update(member: TeamMember)

    @Query("SELECT * FROM team_members WHERE organizationId = :orgId")
    suspend fun getAllByOrg(orgId: Long): List<TeamMember>

    @Query("SELECT COUNT(*) FROM team_members WHERE organizationId = :orgId")
    suspend fun countByOrg(orgId: Long): Int

    @Query("DELETE FROM team_members WHERE id = :id")
    suspend fun delete(id: Long)
}

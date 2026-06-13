package com.binaryapp.admin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.binaryapp.admin.data.local.dao.*
import com.binaryapp.admin.data.local.entities.*

@Database(
    entities = [
        Organization::class,
        AdminUser::class,
        Workspace::class,
        TeamMember::class,
        SecurityPolicy::class,
        Verification::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun organizationDao(): OrganizationDao
    abstract fun adminUserDao(): AdminUserDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun securityPolicyDao(): SecurityPolicyDao
    abstract fun verificationDao(): VerificationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "admin_app_db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

package com.binaryapp.admin.data.repository

import com.binaryapp.admin.data.local.dao.*
import com.binaryapp.admin.data.local.entities.*
import com.binaryapp.admin.utils.HashUtils
import com.binaryapp.admin.utils.OtpUtils

class AdminRepository(
    private val orgDao: OrganizationDao,
    private val userDao: AdminUserDao,
    private val workspaceDao: WorkspaceDao,
    private val teamMemberDao: TeamMemberDao,
    private val securityPolicyDao: SecurityPolicyDao,
    private val verificationDao: VerificationDao
) {

    // ─── Auth ─────────────────────────────────────────────────────────────────

    suspend fun registerAdmin(
        fullName: String,
        email: String,
        password: String,
        companyName: String,
        companySize: String
    ): Result<Pair<AdminUser, Organization>> {
        return try {
            if (userDao.emailExists(email) > 0)
                return Result.failure(Exception("Email already registered"))

            val org = Organization(companyName = companyName, companySize = companySize)
            val orgId = orgDao.insert(org)

            val user = AdminUser(
                organizationId = orgId,
                fullName = fullName,
                email = email,
                passwordHash = HashUtils.hashPassword(password)
            )
            val userId = userDao.insert(user)
            val savedUser = userDao.getById(userId)!!
            val savedOrg = orgDao.getById(orgId)!!
            Result.success(Pair(savedUser, savedOrg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<AdminUser> {
        return try {
            val hash = HashUtils.hashPassword(password)
            val user = userDao.getByEmailAndPassword(email, hash)
                ?: return Result.failure(Exception("Invalid email or password"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── OTP / Verification ────────────────────────────────────────────────────

    suspend fun generateOtp(email: String): String {
        verificationDao.deleteForEmail(email)
        val code = OtpUtils.generate6Digit()
        val exp = System.currentTimeMillis() + 2 * 60 * 1000L // 2 min
        verificationDao.insert(Verification(email = email, otpCode = code, expiresAt = exp))
        return code
    }

    suspend fun verifyOtp(email: String, code: String): Result<Boolean> {
        return try {
            val v = verificationDao.getLatestForEmail(email)
                ?: return Result.failure(Exception("No verification found"))
            if (System.currentTimeMillis() > v.expiresAt)
                return Result.failure(Exception("CODE_EXPIRED"))
            if (v.otpCode != code)
                return Result.failure(Exception("INVALID_CODE"))
            verificationDao.markUsed(v.id)
            userDao.getByEmail(email)?.let { userDao.markVerified(it.id) }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Password Reset ────────────────────────────────────────────────────────

    suspend fun initiatePasswordReset(email: String): Result<String> {
        return try {
            userDao.getByEmail(email)
                ?: return Result.failure(Exception("Email not found"))
            val otp = generateOtp(email)
            Result.success(otp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Boolean> {
        return try {
            userDao.updatePassword(email, HashUtils.hashPassword(newPassword))
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Organization Setup ────────────────────────────────────────────────────

    suspend fun updateOrganization(org: Organization): Result<Organization> {
        return try {
            orgDao.update(org)
            Result.success(org)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrganization(orgId: Long): Organization? = orgDao.getById(orgId)

    // ─── Workspace ─────────────────────────────────────────────────────────────

    suspend fun createWorkspace(
        orgId: Long,
        name: String,
        url: String
    ): Result<Workspace> {
        return try {
            val ws = Workspace(organizationId = orgId, workspaceName = name, workspaceUrl = url)
            val id = workspaceDao.insert(ws)
            Result.success(ws.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkspace(orgId: Long): Workspace? = workspaceDao.getByOrg(orgId)

    // ─── Team Members ──────────────────────────────────────────────────────────

    suspend fun inviteMembers(members: List<TeamMember>): Result<Boolean> {
        return try {
            teamMemberDao.insertAll(members)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamMembers(orgId: Long): List<TeamMember> =
        teamMemberDao.getAllByOrg(orgId)

    suspend fun getTeamCount(orgId: Long): Int = teamMemberDao.countByOrg(orgId)

    // ─── Security Policy ───────────────────────────────────────────────────────

    suspend fun saveSecurityPolicy(policy: SecurityPolicy): Result<SecurityPolicy> {
        return try {
            val id = securityPolicyDao.insert(policy)
            Result.success(policy.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSecurityPolicy(orgId: Long): SecurityPolicy? =
        securityPolicyDao.getByOrg(orgId)

    // ─── User ─────────────────────────────────────────────────────────────────

    suspend fun getUserById(id: Long): AdminUser? = userDao.getById(id)
    suspend fun getUserByEmail(email: String): AdminUser? = userDao.getByEmail(email)
}

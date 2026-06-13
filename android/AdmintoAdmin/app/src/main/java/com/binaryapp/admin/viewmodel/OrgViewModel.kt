package com.binaryapp.admin.viewmodel

import androidx.lifecycle.*
import com.binaryapp.admin.data.local.entities.*
import com.binaryapp.admin.data.repository.AdminRepository
import kotlinx.coroutines.launch

class OrgViewModel(private val repo: AdminRepository) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val tag: String = "") : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> = _state

    private val _organization = MutableLiveData<Organization?>()
    val organization: LiveData<Organization?> = _organization

    private val _workspace = MutableLiveData<Workspace?>()
    val workspace: LiveData<Workspace?> = _workspace

    private val _teamMembers = MutableLiveData<List<TeamMember>>(emptyList())
    val teamMembers: LiveData<List<TeamMember>> = _teamMembers

    private val _securityPolicy = MutableLiveData<SecurityPolicy?>()
    val securityPolicy: LiveData<SecurityPolicy?> = _securityPolicy

    // Preview list for invite screen
    private val _pendingInvites = MutableLiveData<MutableList<TeamMember>>(mutableListOf())
    val pendingInvites: LiveData<MutableList<TeamMember>> = _pendingInvites

    // Draft states to preserve form data during navigation
    var draftOrgName: String? = null
    var draftIndustry: String? = null
    var draftCompanySize: String? = null
    var draftCountry: String? = null
    var draftTimezone: String? = null

    var draftWorkspaceName: String? = null
    var draftWorkspaceUrl: String? = null

    var draftInviteEmail: String? = null
    var draftInviteRole: String? = null
    var draftInviteDept: String? = null

    // ─── Organization ─────────────────────────────────────────────────────────

    fun loadOrganization(orgId: Long) {
        viewModelScope.launch { _organization.value = repo.getOrganization(orgId) }
    }

    fun updateOrganization(org: Organization) {
        _state.value = State.Loading
        viewModelScope.launch {
            repo.updateOrganization(org).fold(
                onSuccess = {
                    _organization.value = it
                    _state.value = State.Success("org_updated")
                },
                onFailure = { _state.value = State.Error(it.message ?: "Update failed") }
            )
        }
    }

    // ─── Workspace ────────────────────────────────────────────────────────────

    fun createWorkspace(orgId: Long, name: String, url: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            repo.createWorkspace(orgId, name, url).fold(
                onSuccess = {
                    _workspace.value = it
                    _state.value = State.Success("workspace_created")
                },
                onFailure = { _state.value = State.Error(it.message ?: "Failed") }
            )
        }
    }

    fun loadWorkspace(orgId: Long) {
        viewModelScope.launch { _workspace.value = repo.getWorkspace(orgId) }
    }

    // ─── Team Members ─────────────────────────────────────────────────────────

    fun addPendingInvite(member: TeamMember) {
        val list = _pendingInvites.value ?: mutableListOf()
        list.add(member)
        _pendingInvites.value = list
    }

    fun removePendingInvite(index: Int) {
        val list = _pendingInvites.value ?: mutableListOf()
        if (index in list.indices) {
            list.removeAt(index)
            _pendingInvites.value = list
        }
    }

    fun sendInvitations(orgId: Long) {
        _state.value = State.Loading
        viewModelScope.launch {
            val members = _pendingInvites.value ?: emptyList()
            val withOrg = members.map { it.copy(organizationId = orgId) }
            repo.inviteMembers(withOrg).fold(
                onSuccess = {
                    _teamMembers.value = repo.getTeamMembers(orgId)
                    _state.value = State.Success("invitations_sent")
                },
                onFailure = { _state.value = State.Error(it.message ?: "Failed") }
            )
        }
    }

    fun loadTeamMembers(orgId: Long) {
        viewModelScope.launch { _teamMembers.value = repo.getTeamMembers(orgId) }
    }

    // ─── Security Policy ──────────────────────────────────────────────────────

    fun saveSecurityPolicy(policy: SecurityPolicy) {
        _state.value = State.Loading
        viewModelScope.launch {
            repo.saveSecurityPolicy(policy).fold(
                onSuccess = {
                    _securityPolicy.value = it
                    _state.value = State.Success("security_saved")
                },
                onFailure = { _state.value = State.Error(it.message ?: "Failed") }
            )
        }
    }

    fun loadSecurityPolicy(orgId: Long) {
        viewModelScope.launch { _securityPolicy.value = repo.getSecurityPolicy(orgId) }
    }

    fun resetState() { _state.value = State.Idle }
}

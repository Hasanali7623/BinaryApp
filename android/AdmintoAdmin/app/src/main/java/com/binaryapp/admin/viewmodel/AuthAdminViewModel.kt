package com.binaryapp.admin.viewmodel

import androidx.lifecycle.*
import com.binaryapp.admin.data.local.entities.AdminUser
import com.binaryapp.admin.data.repository.AdminRepository
import kotlinx.coroutines.launch

class AuthAdminViewModel(private val repo: AdminRepository) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val tag: String = "") : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> = _state

    private val _currentUser = MutableLiveData<AdminUser?>()
    val currentUser: LiveData<AdminUser?> = _currentUser

    private val _currentOrgId = MutableLiveData<Long>(-1L)
    val currentOrgId: LiveData<Long> = _currentOrgId

    private val _otp = MutableLiveData<String>()
    val otp: LiveData<String> = _otp

    // ─── Register ────────────────────────────────────────────────────────────

    fun register(
        fullName: String,
        email: String,
        password: String,
        companyName: String,
        companySize: String
    ) {
        _state.value = State.Loading
        viewModelScope.launch {
            repo.registerAdmin(fullName, email, password, companyName, companySize).fold(
                onSuccess = { (user, org) ->
                    _currentUser.value = user
                    _currentOrgId.value = org.id
                    val code = repo.generateOtp(email)
                    _otp.value = code
                    _state.value = State.Success("registered")
                },
                onFailure = { _state.value = State.Error(it.message ?: "Registration failed") }
            )
        }
    }

    // ─── Login ───────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            repo.login(email, password).fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _currentOrgId.value = user.organizationId
                    _state.value = State.Success("login_ok")
                },
                onFailure = { _state.value = State.Error(it.message ?: "Login failed") }
            )
        }
    }

    // ─── OTP ─────────────────────────────────────────────────────────────────

    fun verifyOtp(email: String, code: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            repo.verifyOtp(email, code).fold(
                onSuccess = { _state.value = State.Success("otp_verified") },
                onFailure = { e ->
                    val tag = when {
                        e.message == "CODE_EXPIRED" -> "code_expired"
                        e.message == "INVALID_CODE" -> "invalid_code"
                        else -> "verify_failed"
                    }
                    _state.value = State.Error(tag)
                }
            )
        }
    }

    fun resendOtp(email: String) {
        viewModelScope.launch {
            val code = repo.generateOtp(email)
            _otp.value = code
            _state.value = State.Success("otp_resent")
        }
    }

    // ─── Password Reset ───────────────────────────────────────────────────────

    fun initiatePasswordReset(email: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            repo.initiatePasswordReset(email).fold(
                onSuccess = { code ->
                    _otp.value = code
                    _state.value = State.Success("reset_sent")
                },
                onFailure = { _state.value = State.Error(it.message ?: "Email not found") }
            )
        }
    }

    fun resetPassword(email: String, newPassword: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            repo.resetPassword(email, newPassword).fold(
                onSuccess = { _state.value = State.Success("password_reset") },
                onFailure = { _state.value = State.Error(it.message ?: "Failed") }
            )
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch { _currentUser.value = repo.getUserById(userId) }
    }

    fun resetState() { _state.value = State.Idle }
}

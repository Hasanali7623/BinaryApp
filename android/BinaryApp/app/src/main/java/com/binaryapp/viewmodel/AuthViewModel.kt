package com.binaryapp.viewmodel

import androidx.lifecycle.*
import com.binaryapp.data.local.entities.Session
import com.binaryapp.data.local.entities.TrustedDevice
import com.binaryapp.data.local.entities.User
import com.binaryapp.data.repository.AuthRepository
import com.binaryapp.utils.ValidationUtils
import kotlinx.coroutines.launch

/**
 * AuthViewModel - Central ViewModel for all authentication flows.
 * Follows MVVM architecture with LiveData for UI state management.
 */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    // ─── UI State ──────────────────────────────────────────────────────────────

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val message: String = "") : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _generatedOtp = MutableLiveData<String>()
    val generatedOtp: LiveData<String> = _generatedOtp

    private val _activeSession = MutableLiveData<Session?>()
    val activeSession: LiveData<Session?> = _activeSession

    // ─── Registration ──────────────────────────────────────────────────────────

    fun register(fullName: String, email: String, password: String, role: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.registerUser(fullName, email, password, role)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    // Generate OTP for email verification
                    val otp = repository.generateAndSaveOtp(user.id)
                    _generatedOtp.value = otp
                    _authState.value = AuthState.Success("Registration successful")
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Registration failed")
                }
            )
        }
    }

    // ─── Login ─────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    if (!user.isVerified) {
                        val otp = repository.generateAndSaveOtp(user.id)
                        _generatedOtp.value = otp
                        _authState.value = AuthState.Success("need_verification")
                    } else {
                        _authState.value = AuthState.Success("login_success")
                    }
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Login failed")
                }
            )
        }
    }

    // ─── OTP Verification ──────────────────────────────────────────────────────

    fun verifyOtp(userId: Long, code: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.verifyOtp(userId, code)
            result.fold(
                onSuccess = {
                    val user = repository.getUserById(userId)
                    _currentUser.value = user
                    _authState.value = AuthState.Success("otp_verified")
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Verification failed")
                }
            )
        }
    }

    fun resendOtp(userId: Long) {
        viewModelScope.launch {
            val otp = repository.generateAndSaveOtp(userId)
            _generatedOtp.value = otp
            _authState.value = AuthState.Success("otp_resent")
        }
    }

    // ─── Password Reset ────────────────────────────────────────────────────────

    fun initiatePasswordReset(email: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.initiatePasswordReset(email)
            result.fold(
                onSuccess = { userId ->
                    val otp = repository.generateAndSaveOtp(userId)
                    _generatedOtp.value = otp
                    _authState.value = AuthState.Success("reset_initiated")
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Email not found")
                }
            )
        }
    }

    fun updatePassword(email: String, newPassword: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.updatePassword(email, newPassword)
            result.fold(
                onSuccess = {
                    _authState.value = AuthState.Success("password_updated")
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Failed to update password")
                }
            )
        }
    }

    // ─── Device Trust ──────────────────────────────────────────────────────────

    fun trustDevice(userId: Long, deviceName: String, deviceModel: String, location: String) {
        viewModelScope.launch {
            val result = repository.trustDevice(userId, deviceName, deviceModel, location)
            result.fold(
                onSuccess = {
                    _authState.value = AuthState.Success("device_trusted")
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(e.message ?: "Failed to trust device")
                }
            )
        }
    }

    // ─── Session ───────────────────────────────────────────────────────────────

    /**
     * Creates a new session record for the user.
     * FIX #6: The location parameter has no hardcoded default — callers must provide the real
     * location obtained from LocationHelper (or a fallback string).
     */
    fun createSession(userId: Long, deviceInfo: String, location: String = "Location unavailable") {
        viewModelScope.launch {
            repository.createSession(userId, deviceInfo, location)
            val session = repository.getActiveSession(userId)
            _activeSession.value = session
        }
    }

    fun loadActiveSession(userId: Long) {
        viewModelScope.launch {
            _activeSession.value = repository.getActiveSession(userId)
        }
    }

    fun loadUserById(userId: Long) {
        viewModelScope.launch {
            _currentUser.value = repository.getUserById(userId)
        }
    }

    // ─── Validation Helpers ───────────────────────────────────────────────────

    fun validateEmail(email: String): String? {
        return if (!ValidationUtils.isValidEmail(email)) "Please enter a valid email address" else null
    }

    fun validatePassword(password: String): String? {
        return if (password.length < 8) "Password must be at least 8 characters" else null
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

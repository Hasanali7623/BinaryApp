package com.binaryapp.ui.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentLoginBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.SessionManager
import com.binaryapp.viewmodel.AuthViewModel

/**
 * Login Fragment - Entry point for user authentication.
 * Supports email/password login, remember device, social login.
 *
 * Security fixes applied:
 *  - Credentials are cleared on resume and after successful login (Fix #1)
 *  - Login is removed from the back stack after authentication (Fix #1 & #3)
 *  - Unverified accounts are rejected here; OTP belongs to Sign Up only (Fix #4)
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager
        setupUI()
        observeViewModel()
    }

    /**
     * FIX #1: Clear sensitive fields every time the screen becomes visible again
     * (e.g., after the user presses back from another screen).
     * This ensures credentials are never retained in memory or displayed after login.
     */
    override fun onResume() {
        super.onResume()
        clearCredentialFields()
    }

    private fun setupUI() {
        binding.apply {
            // Sign In button
            btnSignIn.setOnClickListener {
                performLogin()
            }

            // Forgot password
            tvForgotPassword.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
            }

            // Create Account
            tvCreateAccount.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            }

            // Google Login (mock)
            btnGoogle.setOnClickListener {
                Toast.makeText(context, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show()
            }

            // Microsoft Login (mock)
            btnMicrosoft.setOnClickListener {
                Toast.makeText(context, "Microsoft Sign-In coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        }
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        authViewModel.login(email, password)
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.btnSignIn.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthViewModel.AuthState.Success -> {
                    binding.btnSignIn.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    handleSuccess(state.message)
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.btnSignIn.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnSignIn.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun handleSuccess(message: String) {
        val user = authViewModel.currentUser.value ?: return
        when (message) {
            "need_verification" -> {
                // FIX #4: Unverified accounts MUST NOT be redirected to OTP verification from Login.
                // OTP continuation belongs only to Sign Up. Show a clear rejection message instead.
                authViewModel.resetState()
                Toast.makeText(
                    context,
                    "Your account is not yet verified. Please complete registration using Sign Up.",
                    Toast.LENGTH_LONG
                ).show()
            }
            "login_success" -> {
                // FIX #1: Wipe credentials from fields BEFORE navigating
                clearCredentialFields()

                // Persist the authenticated session to SharedPreferences
                sessionManager.saveUserSession(user.id, user.email, user.fullName, user.role)

                // FIX #1 & #3: Pop loginFragment inclusive=true so it is fully removed from the
                // back stack. The user cannot press Back to return to the login screen.
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()
                findNavController().navigate(
                    R.id.action_loginFragment_to_trustDeviceFragment,
                    null,
                    navOptions
                )
                authViewModel.resetState()
            }
        }
    }

    /**
     * Clears email and password fields.
     * TextInputLayout's endIconMode="password_toggle" handles the eye icon natively,
     * so we only need to reset the text and the TIL error states here.
     */
    private fun clearCredentialFields() {
        binding.etEmail.setText("")
        binding.etPassword.setText("")
        // Clear any lingering validation errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // FIX #1: Zero out the password field before the view is destroyed
        binding.etPassword.setText("")
        _binding = null
    }
}

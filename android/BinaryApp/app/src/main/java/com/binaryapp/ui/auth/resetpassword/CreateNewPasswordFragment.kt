package com.binaryapp.ui.auth.resetpassword

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentCreateNewPasswordBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.AuditLogger
import com.binaryapp.utils.SessionManager
import com.binaryapp.utils.ValidationUtils
import com.binaryapp.viewmodel.AuthViewModel

/**
 * Create New Password Fragment - Password update with strength meter.
 */
class CreateNewPasswordFragment : Fragment() {

    private var _binding: FragmentCreateNewPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateNewPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager
        setupPasswordStrengthWatcher()
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnUpdatePassword.setOnClickListener {
            updatePassword()
        }
    }


    private fun setupPasswordStrengthWatcher() {
        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                updatePasswordStrength(password)
            }
        })
        // Hide the strict requirements card — only 8 chars minimum needed
        binding.cardRequirements.visibility = android.view.View.GONE
    }

    private fun updatePasswordStrength(password: String) {
        val strength = ValidationUtils.getPasswordStrength(password)
        binding.progressPasswordStrength.progress = strength.progress
        binding.tvStrengthLabel.text = strength.label

        val color = when (strength) {
            ValidationUtils.PasswordStrength.WEAK -> R.color.error_red
            ValidationUtils.PasswordStrength.MEDIUM -> R.color.warning_yellow
            ValidationUtils.PasswordStrength.STRONG -> R.color.neon_blue
            ValidationUtils.PasswordStrength.VERY_STRONG -> R.color.success_green
        }
        binding.tvStrengthLabel.setTextColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun updatePassword() {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (newPassword.length < 8) {
            binding.tilNewPassword.error = "Password must be at least 8 characters"
            return
        }
        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return
        }
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        val email = sessionManager.resetEmail
        if (email.isBlank()) {
            Toast.makeText(context, "Session expired. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        authViewModel.updatePassword(email, newPassword)
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.btnUpdatePassword.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthViewModel.AuthState.Success -> {
                    binding.btnUpdatePassword.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    if (state.message == "password_updated") {
                        val email = sessionManager.resetEmail
                        AuditLogger.logEvent(
                            requireContext(),
                            null, // User ID not immediately available from email, but metadata tracks it
                            "PASSWORD_UPDATED",
                            mapOf("email" to email)
                        )
                        findNavController().navigate(R.id.action_createNewPasswordFragment_to_passwordChangedFragment)
                        authViewModel.resetState()
                    }
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.btnUpdatePassword.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    authViewModel.resetState()
                }
                else -> {
                    binding.btnUpdatePassword.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.binaryapp.ui.auth.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentRegisterBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.SessionManager
import com.binaryapp.utils.ValidationUtils
import com.binaryapp.viewmodel.AuthViewModel

/**
 * Register Fragment - User registration screen.
 * Collects full name, email, password, confirm password, and role.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager
    private var selectedRole = "Task Provider"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            // Back button
            btnBack.setOnClickListener {
                findNavController().navigateUp()
            }

            // Role selection
            cardTaskProvider.setOnClickListener {
                selectedRole = "Task Provider"
                updateRoleSelection()
            }
            cardHarness.setOnClickListener {
                selectedRole = "Harness"
                updateRoleSelection()
            }
            updateRoleSelection()

            // Password toggles
            btnTogglePassword.setOnClickListener { togglePasswordVisibility(true) }
            btnToggleConfirm.setOnClickListener { togglePasswordVisibility(false) }

            // Create Account button
            btnCreateAccount.setOnClickListener {
                performRegistration()
            }

            // Already have account
            tvSignIn.setOnClickListener {
                findNavController().navigateUp()
            }

            // Social logins
            btnGoogle.setOnClickListener {
                Toast.makeText(context, "Google Sign-Up coming soon", Toast.LENGTH_SHORT).show()
            }
            btnMicrosoft.setOnClickListener {
                Toast.makeText(context, "Microsoft Sign-Up coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateRoleSelection() {
        binding.cardTaskProvider.isSelected = selectedRole == "Task Provider"
        binding.cardHarness.isSelected = selectedRole == "Harness"
        // Update visual state
        val selectedAlpha = 1.0f
        val unselectedAlpha = 0.5f
        if (selectedRole == "Task Provider") {
            binding.cardTaskProvider.alpha = selectedAlpha
            binding.cardHarness.alpha = unselectedAlpha
        } else {
            binding.cardTaskProvider.alpha = unselectedAlpha
            binding.cardHarness.alpha = selectedAlpha
        }
    }

    private fun togglePasswordVisibility(isPassword: Boolean) {
        val et = if (isPassword) binding.etPassword else binding.etConfirmPassword
        val btn = if (isPassword) binding.btnTogglePassword else binding.btnToggleConfirm
        val isVisible = et.transformationMethod == null
        if (isVisible) {
            et.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            btn.setImageResource(R.drawable.ic_eye_off)
        } else {
            et.transformationMethod = null
            btn.setImageResource(R.drawable.ic_eye)
        }
        et.setSelection(et.text?.length ?: 0)
    }

    private fun performRegistration() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etWorkEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        var hasError = false

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            hasError = true
        } else binding.tilFullName.error = null

        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilWorkEmail.error = "Please enter a valid email"
            hasError = true
        } else binding.tilWorkEmail.error = null

        if (password.length < 8) {
            binding.tilPassword.error = "Password must be at least 8 characters"
            hasError = true
        } else binding.tilPassword.error = null

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            hasError = true
        } else binding.tilConfirmPassword.error = null

        if (hasError) return
        authViewModel.register(fullName, email, password, selectedRole)
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.btnCreateAccount.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthViewModel.AuthState.Success -> {
                    binding.btnCreateAccount.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    val user = authViewModel.currentUser.value ?: return@observe
                    sessionManager.pendingUserId = user.id
                    sessionManager.pendingEmail = user.email
                    findNavController().navigate(R.id.action_registerFragment_to_emailVerificationFragment)
                    authViewModel.resetState()
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.btnCreateAccount.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnCreateAccount.isEnabled = true
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

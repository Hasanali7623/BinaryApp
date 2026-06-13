package com.binaryapp.ui.auth.forgotpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentForgotPasswordBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.SessionManager
import com.binaryapp.utils.ValidationUtils
import com.binaryapp.viewmodel.AuthViewModel

/**
 * Forgot Password Fragment - Email input for password reset.
 */
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSendResetLink.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (!ValidationUtils.isValidEmail(email)) {
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }
            binding.tilEmail.error = null
            sessionManager.resetEmail = email
            authViewModel.initiatePasswordReset(email)
        }

        binding.tvBackToSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.btnSendResetLink.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthViewModel.AuthState.Success -> {
                    binding.btnSendResetLink.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    if (state.message == "reset_initiated") {
                        findNavController().navigate(R.id.action_forgotPasswordFragment_to_resetLinkSentFragment)
                        authViewModel.resetState()
                    }
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.btnSendResetLink.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    authViewModel.resetState()
                }
                else -> {
                    binding.btnSendResetLink.isEnabled = true
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

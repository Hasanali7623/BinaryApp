package com.binaryapp.ui.auth.resetpassword

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentResetLinkSentBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.SessionManager
import com.binaryapp.viewmodel.AuthViewModel

/**
 * Reset Link Sent Fragment - Confirmation that reset email was sent.
 */
class ResetLinkSentFragment : Fragment() {

    private var _binding: FragmentResetLinkSentBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetLinkSentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager

        val email = sessionManager.resetEmail
        binding.tvEmailSent.text = "We've sent a secure password reset link to:\n$email"

        binding.btnOpenEmailApp.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_EMAIL)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnResendLink.setOnClickListener {
            authViewModel.initiatePasswordReset(email)
            Toast.makeText(context, "Reset link resent!", Toast.LENGTH_SHORT).show()
        }

        binding.tvBackToSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_resetLinkSentFragment_to_loginFragment)
        }

        // For demo purposes, navigate to password reset screen
        binding.btnOpenEmailApp.setOnLongClickListener {
            findNavController().navigate(R.id.action_resetLinkSentFragment_to_createNewPasswordFragment)
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

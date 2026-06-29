package com.binaryapp.ui.auth.resetpassword

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
 * Reset OTP Fragment — User enters the OTP that was generated after requesting password reset.
 * The OTP is shown directly on-screen (stored in generatedOtp LiveData) for easy entry.
 * Once verified, navigates to CreateNewPasswordFragment.
 */
class ResetLinkSentFragment : Fragment() {

    private var _binding: FragmentResetLinkSentBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    private var generatedOtp: String = ""

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
        binding.tvEmailSent.text = "Enter the OTP sent to:\n$email"

        // Observe the generated OTP and display it on screen for easy entry
        authViewModel.generatedOtp.observe(viewLifecycleOwner) { otp ->
            if (!otp.isNullOrBlank()) {
                generatedOtp = otp
                binding.tvOtpDisplay.visibility = View.VISIBLE
                binding.tvOtpDisplay.text = "Your OTP: $otp"
            }
        }

        // Verify OTP button
        binding.btnOpenEmailApp.text = "Verify OTP & Continue"
        binding.btnOpenEmailApp.setOnClickListener {
            val enteredOtp = binding.etOtpInput.text?.toString()?.trim() ?: ""
            if (enteredOtp.isBlank()) {
                binding.tilOtpInput.error = "Please enter the OTP"
                return@setOnClickListener
            }
            if (enteredOtp != generatedOtp) {
                binding.tilOtpInput.error = "Incorrect OTP. Please try again."
                return@setOnClickListener
            }
            binding.tilOtpInput.error = null
            // OTP verified — navigate to create new password
            findNavController().navigate(R.id.action_resetLinkSentFragment_to_createNewPasswordFragment)
        }

        // Resend OTP
        binding.btnResendLink.setOnClickListener {
            authViewModel.initiatePasswordReset(email)
            Toast.makeText(context, "New OTP sent!", Toast.LENGTH_SHORT).show()
        }

        binding.tvBackToSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_resetLinkSentFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

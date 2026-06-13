package com.binaryapp.ui.auth.verify

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentEmailVerificationBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.SessionManager
import com.binaryapp.viewmodel.AuthViewModel

/**
 * Email Verification Fragment - 6-digit OTP input with auto-navigation.
 */
class EmailVerificationFragment : Fragment() {

    private var _binding: FragmentEmailVerificationBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager
    private var countDownTimer: CountDownTimer? = null
    private val otpFields: Array<EditText> get() = arrayOf(
        binding.etOtp1, binding.etOtp2, binding.etOtp3,
        binding.etOtp4, binding.etOtp5, binding.etOtp6
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager
        setupOtpInput()
        setupUI()
        startCountdown()
        observeViewModel()

        // Show generated OTP (in production, this would be sent via email)
        authViewModel.generatedOtp.observe(viewLifecycleOwner) { otp ->
            Toast.makeText(context, "Demo OTP: $otp", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        val email = sessionManager.pendingEmail
        binding.tvEmailHint.text = "We sent a 6-digit verification code to $email"

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnVerify.setOnClickListener { verifyOtp() }

        binding.tvResendCode.setOnClickListener {
            val userId = sessionManager.pendingUserId
            if (userId != -1L) {
                authViewModel.resendOtp(userId)
                startCountdown()
            }
        }
    }

    private fun setupOtpInput() {
        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < otpFields.size - 1) {
                        otpFields[i + 1].requestFocus()
                    }
                    // Auto-verify when all filled
                    if (getOtpCode().length == 6) {
                        verifyOtp()
                    }
                }
            })

            otpFields[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    otpFields[i].text.isEmpty() &&
                    i > 0
                ) {
                    otpFields[i - 1].apply {
                        requestFocus()
                        setText("")
                    }
                    true
                } else false
            }
        }

        // Focus first field
        otpFields[0].requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(otpFields[0], InputMethodManager.SHOW_IMPLICIT)
    }

    private fun getOtpCode(): String = otpFields.joinToString("") { it.text.toString() }

    private fun clearOtpFields() {
        otpFields.forEach { it.setText("") }
        otpFields[0].requestFocus()
    }

    private fun verifyOtp() {
        val code = getOtpCode()
        if (code.length != 6) {
            Toast.makeText(context, "Please enter the complete 6-digit code", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = sessionManager.pendingUserId
        if (userId == -1L) {
            Toast.makeText(context, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }
        authViewModel.verifyOtp(userId, code)
    }

    private fun startCountdown() {
        countDownTimer?.cancel()
        binding.tvResendCode.isEnabled = false

        countDownTimer = object : CountDownTimer(5 * 60 * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                binding.tvTimer.text = "Code expires in %02d:%02d".format(minutes, seconds)
            }
            override fun onFinish() {
                binding.tvTimer.text = "Code expired"
                binding.tvResendCode.isEnabled = true
            }
        }.start()
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    binding.btnVerify.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthViewModel.AuthState.Success -> {
                    binding.btnVerify.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    when (state.message) {
                        "otp_verified" -> {
                            val user = authViewModel.currentUser.value
                            if (user != null) {
                                sessionManager.saveUserSession(user.id, user.email, user.fullName, user.role)
                            }
                            findNavController().navigate(R.id.action_emailVerificationFragment_to_verifySuccessFragment)
                        }
                        "otp_resent" -> {
                            Toast.makeText(context, "New code sent!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    authViewModel.resetState()
                }
                is AuthViewModel.AuthState.Error -> {
                    binding.btnVerify.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    clearOtpFields()
                    findNavController().navigate(R.id.action_emailVerificationFragment_to_verifyFailedFragment)
                    authViewModel.resetState()
                }
                else -> {
                    binding.btnVerify.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}

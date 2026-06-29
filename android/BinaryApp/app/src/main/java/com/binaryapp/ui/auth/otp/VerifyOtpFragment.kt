package com.binaryapp.ui.auth.otp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.data.remote.SupabaseClient
import com.binaryapp.databinding.FragmentVerifyOtpBinding
import com.binaryapp.ui.auth.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Verify OTP Fragment - Verifies MSG91 OTP via Edge Function and logs user in.
 */
class VerifyOtpFragment : Fragment() {

    private var _binding: FragmentVerifyOtpBinding? = null
    private val binding get() = _binding!!
    private var mobileNumber: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mobileNumber = arguments?.getString("mobile") ?: ""
        binding.tvSubtitle.text = "We sent a secure code to $mobileNumber"

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length < 4) {
                binding.etOtp.error = "Enter a valid OTP"
                return@setOnClickListener
            }
            verifyOtp(otp)
        }

        binding.btnResendOtp.setOnClickListener {
            resendOtp()
        }
    }

    private fun verifyOtp(otp: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerifyOtp.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val payload = """{"mobile": "$mobileNumber", "otp": "$otp"}"""
                val response = SupabaseClient.post("functions/v1/msg91-verify-otp", payload)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerifyOtp.isEnabled = true

                    if (response.contains("error")) {
                        Toast.makeText(requireContext(), "Invalid or expired OTP", Toast.LENGTH_SHORT).show()
                    } else {
                        // Extract user data and start session
                        try {
                            val json = JSONObject(response)
                            val userObj = json.getJSONObject("user")
                            val userId = userObj.optLong("id", -1)
                            
                            val sessionManager = (requireActivity() as MainActivity).sessionManager
                            sessionManager.userId = userId
                            sessionManager.userName = "Phone User"
                            sessionManager.userEmail = mobileNumber
                            sessionManager.isLoggedIn = true

                            Toast.makeText(requireContext(), "Authentication Successful!", Toast.LENGTH_SHORT).show()
                            
                            // Navigate to Dashboard
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build()
                            findNavController().navigate(R.id.action_verifyOtpFragment_to_dashboardFragment, null, navOptions)

                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Failed to parse session", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnVerifyOtp.isEnabled = true
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resendOtp() {
        Toast.makeText(requireContext(), "Resending OTP...", Toast.LENGTH_SHORT).show()
        binding.btnResendOtp.isEnabled = false
        
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val payload = """{"mobile": "$mobileNumber"}"""
                SupabaseClient.post("functions/v1/msg91-send-otp", payload)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "OTP resent successfully", Toast.LENGTH_SHORT).show()
                    binding.btnResendOtp.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to resend OTP", Toast.LENGTH_SHORT).show()
                    binding.btnResendOtp.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

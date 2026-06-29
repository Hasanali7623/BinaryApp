package com.binaryapp.ui.auth.otp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.data.remote.SupabaseClient
import com.binaryapp.databinding.FragmentPhoneAuthBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Phone Auth Fragment - Initiates MSG91 OTP flow by calling Edge Function.
 */
class PhoneAuthFragment : Fragment() {

    private var _binding: FragmentPhoneAuthBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhoneAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.isEmpty() || phone.length < 10) {
                binding.etPhone.error = "Enter a valid mobile number with country code"
                return@setOnClickListener
            }

            sendOtp(phone)
        }
    }

    private fun sendOtp(phone: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendOtp.isEnabled = false
        binding.btnSendOtp.text = ""

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Call Supabase Edge Function to send MSG91 OTP
                val payload = """{"mobile": "$phone"}"""
                val response = SupabaseClient.post("functions/v1/msg91-send-otp", payload)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendOtp.isEnabled = true
                    binding.btnSendOtp.text = "Send Secure OTP"
                    
                    if (response.contains("error")) {
                        Toast.makeText(requireContext(), "Failed to send OTP", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "OTP sent successfully!", Toast.LENGTH_SHORT).show()
                        // Navigate to Verify OTP fragment
                        val bundle = Bundle().apply {
                            putString("mobile", phone)
                        }
                        findNavController().navigate(R.id.action_phoneAuthFragment_to_verifyOtpFragment, bundle)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendOtp.isEnabled = true
                    binding.btnSendOtp.text = "Send Secure OTP"
                    Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

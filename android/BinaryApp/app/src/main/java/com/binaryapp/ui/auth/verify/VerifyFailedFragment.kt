package com.binaryapp.ui.auth.verify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentVerifyFailedBinding

/**
 * Verification Failed Fragment - Shown when OTP verification fails.
 */
class VerifyFailedFragment : Fragment() {

    private var _binding: FragmentVerifyFailedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyFailedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnTryAgain.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnResendCode.setOnClickListener {
            // Pop back to email verification
            findNavController().navigate(R.id.action_verifyFailedFragment_to_emailVerificationFragment)
        }

        binding.tvContactSupport.setOnClickListener { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

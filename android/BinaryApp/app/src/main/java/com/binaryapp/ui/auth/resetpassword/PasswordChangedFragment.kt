package com.binaryapp.ui.auth.resetpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentPasswordChangedBinding

/**
 * Password Changed Successfully Fragment.
 */
class PasswordChangedFragment : Fragment() {

    private var _binding: FragmentPasswordChangedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordChangedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackToSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_passwordChangedFragment_to_loginFragment)
        }

        binding.btnReviewSecurity.setOnClickListener {
            findNavController().navigate(R.id.action_passwordChangedFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

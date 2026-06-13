package com.binaryapp.admin.ui.auth.verify

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentVerifyEmailBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.viewmodel.AuthAdminViewModel

class VerifyEmailFragment : Fragment() {
    private var _b: FragmentVerifyEmailBinding? = null
    private val b get() = _b!!
    private val vm: AuthAdminViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentVerifyEmailBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val session = (requireActivity() as MainActivity).sessionManager

        // Display masked email
        val email = session.pendingEmail
        b.tvEmailHint.text = email

        b.btnVerifyEmail.setOnClickListener {
            findNavController().navigate(R.id.action_verifyEmailFragment_to_verifyOrgIdentityFragment)
        }
        b.tvResendCode.setOnClickListener { vm.resendOtp(email) }
        b.tvChangeEmail.setOnClickListener { findNavController().navigateUp() }

        observeState()
    }



    private fun observeState() {
        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthAdminViewModel.State.Loading -> { b.btnVerifyEmail.isEnabled = false; b.progressBar.visibility = View.VISIBLE }
                is AuthAdminViewModel.State.Success -> {
                    b.btnVerifyEmail.isEnabled = true; b.progressBar.visibility = View.GONE
                    if (state.tag == "otp_resent") {
                        Toast.makeText(context, "Verification email resent!", Toast.LENGTH_SHORT).show()
                        vm.resetState()
                    }
                }
                is AuthAdminViewModel.State.Error -> {
                    b.btnVerifyEmail.isEnabled = true; b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> { b.btnVerifyEmail.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

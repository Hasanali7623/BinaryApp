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
import com.binaryapp.admin.databinding.FragmentVerifyOrgIdentityBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.viewmodel.AuthAdminViewModel

class VerifyOrgIdentityFragment : Fragment() {
    private var _b: FragmentVerifyOrgIdentityBinding? = null
    private val b get() = _b!!
    private val vm: AuthAdminViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentVerifyOrgIdentityBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val session = (requireActivity() as MainActivity).sessionManager
        val email = session.pendingEmail

        // Generate org identity OTP
        vm.resendOtp(email)

        setupOtpInputs()

        b.btnVerifyOrg.setOnClickListener {
            val code = getCode()
            if (code.length < 6) { Toast.makeText(context, "Enter 6-digit code", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            vm.verifyOtp(email, code)
        }
        b.tvResendCode.setOnClickListener { vm.resendOtp(email); Toast.makeText(context, "Code resent", Toast.LENGTH_SHORT).show() }
        b.tvChangeEmail.setOnClickListener { findNavController().navigateUp() }

        observeState()
    }

    private fun setupOtpInputs() {
        val fields = listOf(b.otp1, b.otp2, b.otp3, b.otp4, b.otp5, b.otp6)
        fields.forEachIndexed { index, et ->
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b2: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < fields.size - 1) fields[index + 1].requestFocus()
                    if (s?.isEmpty() == true && index > 0) fields[index - 1].requestFocus()
                }
            })
        }
    }

    private fun getCode() = listOf(b.otp1, b.otp2, b.otp3, b.otp4, b.otp5, b.otp6).joinToString("") { it.text.toString() }

    private fun observeState() {
        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthAdminViewModel.State.Loading -> { b.btnVerifyOrg.isEnabled = false }
                is AuthAdminViewModel.State.Success -> {
                    b.btnVerifyOrg.isEnabled = true
                    if (state.tag == "otp_verified") {
                        findNavController().navigate(R.id.action_verifyOrgIdentityFragment_to_orgVerifiedFragment)
                        vm.resetState()
                    }
                }
                is AuthAdminViewModel.State.Error -> {
                    b.btnVerifyOrg.isEnabled = true
                    when (state.message) {
                        "code_expired" -> findNavController().navigate(R.id.action_verifyOrgIdentityFragment_to_codeExpiredFragment)
                        "invalid_code" -> findNavController().navigate(R.id.action_verifyOrgIdentityFragment_to_invalidCodeFragment)
                        "verify_failed" -> findNavController().navigate(R.id.action_verifyOrgIdentityFragment_to_verifyFailedFragment)
                        else -> Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
                else -> b.btnVerifyOrg.isEnabled = true
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

package com.binaryapp.admin.ui.auth.forgotpassword

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentForgotPasswordBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.utils.ValidationUtils
import com.binaryapp.admin.viewmodel.AuthAdminViewModel

class ForgotPasswordFragment : Fragment() {
    private var _b: FragmentForgotPasswordBinding? = null
    private val b get() = _b!!
    private val vm: AuthAdminViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentForgotPasswordBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        b.btnBack.setOnClickListener { findNavController().navigateUp() }
        b.btnSendRecovery.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            if (!ValidationUtils.isValidEmail(email)) {
                b.tilEmail.error = "Valid business email required"
            } else {
                b.tilEmail.error = null
                vm.initiatePasswordReset(email)
            }
        }

        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthAdminViewModel.State.Loading -> { b.btnSendRecovery.isEnabled = false; b.progressBar.visibility = View.VISIBLE }
                is AuthAdminViewModel.State.Success -> {
                    b.btnSendRecovery.isEnabled = true; b.progressBar.visibility = View.GONE
                    val session = (requireActivity() as MainActivity).sessionManager
                    session.pendingEmail = b.etEmail.text.toString().trim()
                    findNavController().navigate(R.id.action_forgotPasswordFragment_to_recoveryEmailSentFragment)
                    vm.resetState()
                }
                is AuthAdminViewModel.State.Error -> {
                    b.btnSendRecovery.isEnabled = true; b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { b.btnSendRecovery.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

package com.binaryapp.admin.ui.auth.forgotpassword

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentRecoveryEmailSentBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.viewmodel.AuthAdminViewModel

class RecoveryEmailSentFragment : Fragment() {
    private var _b: FragmentRecoveryEmailSentBinding? = null
    private val b get() = _b!!
    private val vm: AuthAdminViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentRecoveryEmailSentBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val session = (requireActivity() as MainActivity).sessionManager
        b.tvEmailAddress.text = session.pendingEmail

        b.btnOpenEmail.setOnClickListener {
            // Navigate to create new password for demo purposes
            findNavController().navigate(R.id.action_recoveryEmailSentFragment_to_createNewPasswordFragment)
        }
        b.tvResendEmail.setOnClickListener {
            vm.initiatePasswordReset(session.pendingEmail)
            Toast.makeText(context, "Email resent!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

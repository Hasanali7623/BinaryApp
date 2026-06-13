package com.binaryapp.admin.ui.auth.forgotpassword

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentCreateNewPasswordBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.utils.ValidationUtils
import com.binaryapp.admin.viewmodel.AuthAdminViewModel

class CreateNewPasswordFragment : Fragment() {
    private var _b: FragmentCreateNewPasswordBinding? = null
    private val b get() = _b!!
    private val vm: AuthAdminViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentCreateNewPasswordBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)

        b.btnToggleNew.setOnClickListener {
            val et = b.etNewPassword
            val vis = et.transformationMethod == null
            et.transformationMethod = if (vis) android.text.method.PasswordTransformationMethod.getInstance() else null
            b.btnToggleNew.setImageResource(if (vis) R.drawable.ic_eye_off else R.drawable.ic_eye)
            et.setSelection(et.text?.length ?: 0)
        }
        b.btnToggleConfirm.setOnClickListener {
            val et = b.etConfirmPassword
            val vis = et.transformationMethod == null
            et.transformationMethod = if (vis) android.text.method.PasswordTransformationMethod.getInstance() else null
            b.btnToggleConfirm.setImageResource(if (vis) R.drawable.ic_eye_off else R.drawable.ic_eye)
            et.setSelection(et.text?.length ?: 0)
        }

        // Strength meter
        b.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                val strength = ValidationUtils.passwordStrength(str)
                b.strengthBar.progress = strength * 20
                b.tvStrengthLabel.text = when {
                    strength <= 1 -> "Weak"
                    strength <= 3 -> "Strong"
                    else -> "Enterprise Grade"
                }
                // Validation checklist
                b.checkMinLength.isSelected = str.length >= 12
                b.checkUppercase.isSelected = str.any { it.isUpperCase() }
                b.checkLowercase.isSelected = str.any { it.isLowerCase() }
                b.checkNumber.isSelected = str.any { it.isDigit() }
                b.checkSpecial.isSelected = str.any { !it.isLetterOrDigit() }
            }
        })

        b.btnUpdatePassword.setOnClickListener {
            val newPass = b.etNewPassword.text.toString()
            val confirm = b.etConfirmPassword.text.toString()
            if (newPass.length < 8) { b.tilNewPassword.error = "Min 8 characters"; return@setOnClickListener }
            if (newPass != confirm) { b.tilConfirmPassword.error = "Passwords do not match"; return@setOnClickListener }
            b.tilNewPassword.error = null; b.tilConfirmPassword.error = null
            val session = (requireActivity() as MainActivity).sessionManager
            vm.resetPassword(session.pendingEmail, newPass)
        }

        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthAdminViewModel.State.Loading -> { b.btnUpdatePassword.isEnabled = false; b.progressBar.visibility = View.VISIBLE }
                is AuthAdminViewModel.State.Success -> {
                    b.btnUpdatePassword.isEnabled = true; b.progressBar.visibility = View.GONE
                    findNavController().navigate(R.id.action_createNewPasswordFragment_to_passwordChangedFragment)
                    vm.resetState()
                }
                is AuthAdminViewModel.State.Error -> {
                    b.btnUpdatePassword.isEnabled = true; b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { b.btnUpdatePassword.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

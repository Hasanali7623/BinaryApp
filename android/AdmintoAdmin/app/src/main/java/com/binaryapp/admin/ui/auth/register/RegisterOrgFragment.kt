package com.binaryapp.admin.ui.auth.register

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentRegisterOrgBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.utils.ValidationUtils
import com.binaryapp.admin.viewmodel.AuthAdminViewModel

class RegisterOrgFragment : Fragment() {
    private var _b: FragmentRegisterOrgBinding? = null
    private val b get() = _b!!
    private val vm: AuthAdminViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentRegisterOrgBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        setupDropdowns()
        setupUI()
        observeState()
    }

    private fun setupDropdowns() {
        val sizes = listOf("1-10", "11-50", "51-200", "201-500", "500-1000", "1000+")
        b.spinnerCompanySize.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sizes))
    }

    private fun setupUI() {
        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.btnTogglePassword.setOnClickListener {
            val et = b.etPassword
            val vis = et.transformationMethod == null
            et.transformationMethod = if (vis) android.text.method.PasswordTransformationMethod.getInstance() else null
            b.btnTogglePassword.setImageResource(if (vis) R.drawable.ic_eye_off else R.drawable.ic_eye)
            et.setSelection(et.text?.length ?: 0)
        }
        b.btnToggleConfirm.setOnClickListener {
            val et = b.etConfirmPassword
            val vis = et.transformationMethod == null
            et.transformationMethod = if (vis) android.text.method.PasswordTransformationMethod.getInstance() else null
            b.btnToggleConfirm.setImageResource(if (vis) R.drawable.ic_eye_off else R.drawable.ic_eye)
            et.setSelection(et.text?.length ?: 0)
        }

        b.btnCreateOrg.setOnClickListener { attemptRegister() }
        b.tvSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_registerOrgFragment_to_loginFragment)
        }
    }

    private fun attemptRegister() {
        val name = b.etFullName.text.toString().trim()
        val email = b.etEmail.text.toString().trim()
        val company = b.etCompanyName.text.toString().trim()
        val size = b.spinnerCompanySize.text.toString().trim()
        val password = b.etPassword.text.toString()
        val confirm = b.etConfirmPassword.text.toString()
        var hasErr = false

        if (name.isEmpty()) { b.tilFullName.error = "Required"; hasErr = true } else b.tilFullName.error = null
        if (!ValidationUtils.isValidEmail(email)) { b.tilEmail.error = "Valid email required"; hasErr = true } else b.tilEmail.error = null
        if (company.isEmpty()) { b.tilCompanyName.error = "Required"; hasErr = true } else b.tilCompanyName.error = null
        if (password.length < 8) { b.tilPassword.error = "Min 8 characters"; hasErr = true } else b.tilPassword.error = null
        if (password != confirm) { b.tilConfirmPassword.error = "Passwords do not match"; hasErr = true } else b.tilConfirmPassword.error = null
        if (!b.cbTerms.isChecked) { Toast.makeText(context, "Accept terms to continue", Toast.LENGTH_SHORT).show(); hasErr = true }

        if (!hasErr) vm.register(name, email, password, company, size)
    }

    private fun observeState() {
        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthAdminViewModel.State.Loading -> { b.btnCreateOrg.isEnabled = false; b.progressBar.visibility = View.VISIBLE }
                is AuthAdminViewModel.State.Success -> {
                    b.btnCreateOrg.isEnabled = true; b.progressBar.visibility = View.GONE
                    val session = (requireActivity() as MainActivity).sessionManager
                    vm.currentUser.value?.let { session.pendingEmail = it.email }
                    vm.currentOrgId.value?.let { session.currentOrgId = it }
                    vm.otp.value?.let { session.pendingOtp = it }
                    findNavController().navigate(R.id.action_registerOrgFragment_to_verifyEmailFragment)
                    vm.resetState()
                }
                is AuthAdminViewModel.State.Error -> {
                    b.btnCreateOrg.isEnabled = true; b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { b.btnCreateOrg.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

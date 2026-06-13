package com.binaryapp.admin.ui.auth.login

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentLoginBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.viewmodel.AuthAdminViewModel

class LoginFragment : Fragment() {
    private var _b: FragmentLoginBinding? = null
    private val b get() = _b!!
    private val vm: AuthAdminViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentLoginBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        setupUI()
        observeState()
    }

    private fun setupUI() {
        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.btnTogglePassword.setOnClickListener {
            val et = b.etPassword
            val visible = et.transformationMethod == null
            et.transformationMethod = if (visible)
                android.text.method.PasswordTransformationMethod.getInstance() else null
            b.btnTogglePassword.setImageResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye)
            et.setSelection(et.text?.length ?: 0)
        }

        b.btnSignIn.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            val password = b.etPassword.text.toString()
            var err = false
            if (email.isEmpty()) { b.tilEmail.error = "Email required"; err = true } else b.tilEmail.error = null
            if (password.isEmpty()) { b.tilPassword.error = "Password required"; err = true } else b.tilPassword.error = null
            if (!err) vm.login(email, password)
        }

        b.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
        b.tvCreateOrg.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerOrgFragment)
        }
    }

    private fun observeState() {
        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthAdminViewModel.State.Loading -> {
                    b.btnSignIn.isEnabled = false
                    b.progressBar.visibility = View.VISIBLE
                }
                is AuthAdminViewModel.State.Success -> {
                    b.btnSignIn.isEnabled = true
                    b.progressBar.visibility = View.GONE
                    val session = (requireActivity() as MainActivity).sessionManager
                    vm.currentUser.value?.let { user ->
                        session.currentUserId = user.id
                        session.currentOrgId = user.organizationId
                        session.isLoggedIn = true
                    }
                    findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                    vm.resetState()
                }
                is AuthAdminViewModel.State.Error -> {
                    b.btnSignIn.isEnabled = true
                    b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { b.btnSignIn.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

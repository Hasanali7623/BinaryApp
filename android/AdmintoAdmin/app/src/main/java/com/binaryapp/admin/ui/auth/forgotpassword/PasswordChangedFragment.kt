package com.binaryapp.admin.ui.auth.forgotpassword

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentPasswordChangedBinding

class PasswordChangedFragment : Fragment() {
    private var _b: FragmentPasswordChangedBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentPasswordChangedBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        b.btnReturnToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_passwordChangedFragment_to_loginFragment)
        }
        b.tvReviewSecurity.setOnClickListener {
            findNavController().navigate(R.id.action_passwordChangedFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

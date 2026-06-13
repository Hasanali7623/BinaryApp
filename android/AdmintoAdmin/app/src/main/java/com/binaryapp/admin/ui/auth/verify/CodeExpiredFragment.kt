package com.binaryapp.admin.ui.auth.verify

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentCodeExpiredBinding

class CodeExpiredFragment : Fragment() {
    private var _b: FragmentCodeExpiredBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentCodeExpiredBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        b.btnRequestNewCode.setOnClickListener { findNavController().navigateUp() }
        b.tvBackToVerification.setOnClickListener { findNavController().navigateUp() }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

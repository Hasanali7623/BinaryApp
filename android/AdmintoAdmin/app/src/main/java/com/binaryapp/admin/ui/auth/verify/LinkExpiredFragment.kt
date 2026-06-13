package com.binaryapp.admin.ui.auth.verify

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentLinkExpiredBinding

class LinkExpiredFragment : Fragment() {
    private var _b: FragmentLinkExpiredBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentLinkExpiredBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        b.btnRequestNewLink.setOnClickListener { findNavController().navigate(R.id.action_linkExpiredFragment_to_loginFragment) }
        b.tvBackToSignIn.setOnClickListener { findNavController().navigate(R.id.action_linkExpiredFragment_to_loginFragment) }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

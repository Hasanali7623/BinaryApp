package com.binaryapp.admin.ui.auth.verify

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentEmailVerifiedBinding

class EmailVerifiedFragment : Fragment() {
    private var _b: FragmentEmailVerifiedBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentEmailVerifiedBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        b.btnContinueSetup.setOnClickListener {
            findNavController().navigate(R.id.action_emailVerifiedFragment_to_orgSetupFragment)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

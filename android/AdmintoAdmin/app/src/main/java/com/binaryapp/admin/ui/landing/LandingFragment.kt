package com.binaryapp.admin.ui.landing

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentLandingBinding

class LandingFragment : Fragment() {
    private var _b: FragmentLandingBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentLandingBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        var selectedOption = "create"

        b.cardCreateOrg.setOnClickListener {
            selectedOption = "create"
            b.cardCreateOrg.alpha = 1f; b.cardJoinOrg.alpha = 0.5f
        }
        b.cardJoinOrg.setOnClickListener {
            selectedOption = "join"
            b.cardJoinOrg.alpha = 1f; b.cardCreateOrg.alpha = 0.5f
        }
        b.btnContinue.setOnClickListener {
            if (selectedOption == "create")
                findNavController().navigate(R.id.action_landingFragment_to_registerOrgFragment)
            else
                findNavController().navigate(R.id.action_landingFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

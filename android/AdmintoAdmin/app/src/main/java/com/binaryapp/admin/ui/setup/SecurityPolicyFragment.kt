package com.binaryapp.admin.ui.setup

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.data.local.entities.SecurityPolicy
import com.binaryapp.admin.databinding.FragmentSecurityPolicyBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.viewmodel.OrgViewModel

class SecurityPolicyFragment : Fragment() {
    private var _b: FragmentSecurityPolicyBinding? = null
    private val b get() = _b!!
    private val vm: OrgViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSecurityPolicyBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val session = (requireActivity() as MainActivity).sessionManager
        val orgId = session.currentOrgId

        // Load existing or set defaults
        vm.loadSecurityPolicy(orgId)
        vm.securityPolicy.observe(viewLifecycleOwner) { policy ->
            policy?.let { applyPolicy(it) }
        }

        b.btnApplyPolicies.setOnClickListener { savePolicy(orgId) }
        b.tvViewDefault.setOnClickListener {
            Toast.makeText(context, "Default policies applied", Toast.LENGTH_SHORT).show()
            applyDefaultPolicy()
        }

        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OrgViewModel.State.Loading -> { b.btnApplyPolicies.isEnabled = false; b.progressBar.visibility = View.VISIBLE }
                is OrgViewModel.State.Success -> {
                    b.btnApplyPolicies.isEnabled = true; b.progressBar.visibility = View.GONE
                    session.setupStep = 4
                    findNavController().navigate(R.id.action_securityPolicyFragment_to_dashboardFragment)
                    vm.resetState()
                }
                is OrgViewModel.State.Error -> {
                    b.btnApplyPolicies.isEnabled = true; b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { b.btnApplyPolicies.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    private fun applyPolicy(p: SecurityPolicy) {
        b.switchMfa.isChecked = p.requireMfa
        b.switchPasswordRotation.isChecked = p.passwordRotation
        b.switchBiometric.isChecked = p.biometricEnabled
        b.switchDeviceTrust.isChecked = p.deviceTrustEnabled
        b.switchIpRestrictions.isChecked = p.ipRestrictions
        b.switchLoginMonitoring.isChecked = p.loginMonitoring
        b.sliderSessionTimeout.value = p.sessionTimeout.toFloat()
        b.sliderMinLength.value = p.minPasswordLength.toFloat()
        b.tvSessionValue.text = "${p.sessionTimeout} Minutes"
        b.tvMinLengthValue.text = "${p.minPasswordLength} Characters"
    }

    private fun applyDefaultPolicy() {
        b.switchMfa.isChecked = true
        b.switchPasswordRotation.isChecked = false
        b.switchBiometric.isChecked = false
        b.switchDeviceTrust.isChecked = true
        b.switchIpRestrictions.isChecked = false
        b.switchLoginMonitoring.isChecked = true
        b.sliderSessionTimeout.value = 30f
        b.sliderMinLength.value = 12f
        b.tvSessionValue.text = "30 Minutes"
        b.tvMinLengthValue.text = "12 Characters"
    }

    private fun savePolicy(orgId: Long) {
        val policy = SecurityPolicy(
            organizationId = orgId,
            requireMfa = b.switchMfa.isChecked,
            passwordRotation = b.switchPasswordRotation.isChecked,
            sessionTimeout = b.sliderSessionTimeout.value.toInt(),
            biometricEnabled = b.switchBiometric.isChecked,
            deviceTrustEnabled = b.switchDeviceTrust.isChecked,
            ipRestrictions = b.switchIpRestrictions.isChecked,
            loginMonitoring = b.switchLoginMonitoring.isChecked,
            minPasswordLength = b.sliderMinLength.value.toInt()
        )
        vm.saveSecurityPolicy(policy)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

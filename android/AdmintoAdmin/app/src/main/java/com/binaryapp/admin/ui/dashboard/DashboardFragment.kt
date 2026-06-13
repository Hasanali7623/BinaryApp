package com.binaryapp.admin.ui.dashboard

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentDashboardBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.viewmodel.OrgViewModel

class DashboardFragment : Fragment() {
    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!
    private val vm: OrgViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentDashboardBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val session = (requireActivity() as MainActivity).sessionManager
        val orgId = session.currentOrgId

        vm.loadOrganization(orgId)
        vm.loadWorkspace(orgId)
        vm.loadTeamMembers(orgId)
        vm.loadSecurityPolicy(orgId)

        vm.organization.observe(viewLifecycleOwner) { org ->
            org?.let {
                b.tvOrgName.text = it.companyName
                b.tvOrgDomain.text = it.domain.ifEmpty { "${it.companyName.lowercase()}@binaryapp.io" }
            }
        }

        vm.workspace.observe(viewLifecycleOwner) { ws ->
            ws?.let { b.tvWorkspaceUrl.text = "${it.workspaceUrl}.binaryapp.io" }
        }

        vm.teamMembers.observe(viewLifecycleOwner) { members ->
            b.tvMemberCount.text = "${members.size} Users"
        }

        vm.securityPolicy.observe(viewLifecycleOwner) { policy ->
            policy?.let {
                b.tvSecurityLevel.text = when {
                    it.requireMfa && it.deviceTrustEnabled && it.loginMonitoring -> "Enterprise"
                    it.requireMfa -> "Standard"
                    else -> "Basic"
                }
                b.tvMfaStatus.text = if (it.requireMfa) "Enabled" else "Disabled"
            }
        }

        b.btnGoToDashboard.setOnClickListener {
            // In production this would navigate to the admin dashboard module
            Toast.makeText(context, "Welcome to Admin Dashboard! Module pending.", Toast.LENGTH_LONG).show()
        }
        b.btnOrgSettings.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_orgSetupFragment)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

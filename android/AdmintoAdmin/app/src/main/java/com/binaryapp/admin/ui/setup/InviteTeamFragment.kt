package com.binaryapp.admin.ui.setup

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.data.local.entities.TeamMember
import com.binaryapp.admin.databinding.FragmentInviteTeamBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.utils.ValidationUtils
import com.binaryapp.admin.viewmodel.OrgViewModel

class InviteTeamFragment : Fragment() {
    private var _b: FragmentInviteTeamBinding? = null
    private val b get() = _b!!
    private val vm: OrgViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentInviteTeamBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val session = (requireActivity() as MainActivity).sessionManager

        val roles = listOf("Super Admin", "Admin", "Security Manager", "IT Manager", "Auditor", "Viewer")
        val depts = listOf("Engineering", "Security", "IT Operations", "Compliance", "Management", "Support")

        b.spinnerRole.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles))
        b.spinnerDepartment.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, depts))

        vm.draftInviteEmail?.let { b.etInviteEmail.setText(it) }
        vm.draftInviteRole?.let { b.spinnerRole.setText(it, false) }
        vm.draftInviteDept?.let { b.spinnerDepartment.setText(it, false) }

        b.btnAddToPreview.setOnClickListener {
            val email = b.etInviteEmail.text.toString().trim()
            val role = b.spinnerRole.text.toString()
            val dept = b.spinnerDepartment.text.toString()
            if (!ValidationUtils.isValidEmail(email)) { b.tilInviteEmail.error = "Valid email required"; return@setOnClickListener }
            if (role.isEmpty()) { Toast.makeText(context, "Select a role", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            b.tilInviteEmail.error = null
            val member = TeamMember(organizationId = session.currentOrgId, email = email, role = role, department = dept)
            vm.addPendingInvite(member)
            b.etInviteEmail.setText("")
            updatePreviewList()
        }

        b.btnSendInvitations.setOnClickListener {
            val invites = vm.pendingInvites.value ?: emptyList()
            if (invites.isEmpty()) { Toast.makeText(context, "Add at least one member", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            vm.sendInvitations(session.currentOrgId)
        }
        b.tvSkipForNow.setOnClickListener {
            session.setupStep = 3
            findNavController().navigate(R.id.action_inviteTeamFragment_to_securityPolicyFragment)
        }

        vm.pendingInvites.observe(viewLifecycleOwner) { updatePreviewList() }

        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OrgViewModel.State.Loading -> { b.btnSendInvitations.isEnabled = false; b.progressBar.visibility = View.VISIBLE }
                is OrgViewModel.State.Success -> {
                    b.btnSendInvitations.isEnabled = true; b.progressBar.visibility = View.GONE
                    session.setupStep = 3
                    findNavController().navigate(R.id.action_inviteTeamFragment_to_securityPolicyFragment)
                    vm.resetState()
                }
                is OrgViewModel.State.Error -> {
                    b.btnSendInvitations.isEnabled = true; b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { b.btnSendInvitations.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    private fun updatePreviewList() {
        val invites = vm.pendingInvites.value ?: emptyList()
        b.tvMemberCount.text = "${invites.size} member(s) to invite"
        b.containerPreviewList.removeAllViews()
        invites.forEachIndexed { index, member ->
            val chip = layoutInflater.inflate(R.layout.item_team_member_preview, b.containerPreviewList, false)
            val tvName = chip.findViewById<android.widget.TextView>(R.id.tvMemberEmail)
            val tvRole = chip.findViewById<android.widget.TextView>(R.id.tvMemberRole)
            val btnRemove = chip.findViewById<android.widget.ImageView>(R.id.btnRemoveMember)
            tvName.text = member.email
            tvRole.text = member.role
            btnRemove.setOnClickListener { vm.removePendingInvite(index) }
            b.containerPreviewList.addView(chip)
        }
    }

    override fun onDestroyView() {
        vm.draftInviteEmail = b.etInviteEmail.text.toString()
        vm.draftInviteRole = b.spinnerRole.text.toString()
        vm.draftInviteDept = b.spinnerDepartment.text.toString()
        super.onDestroyView()
        _b = null
    }
}

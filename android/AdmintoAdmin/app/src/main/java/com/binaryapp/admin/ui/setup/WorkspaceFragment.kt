package com.binaryapp.admin.ui.setup

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentWorkspaceBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.utils.ValidationUtils
import com.binaryapp.admin.viewmodel.OrgViewModel

class WorkspaceFragment : Fragment() {
    private var _b: FragmentWorkspaceBinding? = null
    private val b get() = _b!!
    private val vm: OrgViewModel by activityViewModels { (requireActivity() as MainActivity).factory }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentWorkspaceBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val session = (requireActivity() as MainActivity).sessionManager

        vm.draftWorkspaceName?.let { b.etWorkspaceName.setText(it) }
        vm.draftWorkspaceUrl?.let { b.etWorkspaceUrl.setText(it) }

        // Auto-populate URL from workspace name
        b.etWorkspaceName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (b.etWorkspaceName.hasFocus()) {
                    val slug = s.toString().lowercase().replace(Regex("[^a-z0-9]"), "-").trimEnd('-')
                    b.etWorkspaceUrl.setText(slug)
                }
                val slugUrl = b.etWorkspaceUrl.text.toString().ifEmpty { "acme" }
                b.tvUrlPreview.text = "$slugUrl.binaryapp.io"
                b.tvPreviewName.text = s.toString().ifEmpty { "Acme Technologies" }
            }
        })

        // Load org info for domain preview
        vm.loadOrganization(session.currentOrgId)
        vm.organization.observe(viewLifecycleOwner) { org ->
            org?.let {
                b.etOrgDomain.setText(it.domain.ifEmpty { "${it.companyName.lowercase().replace(" ","")}@binaryapp.io" })
                b.tvPreviewOrg.text = it.companyName
            }
        }

        b.btnCreateWorkspace.setOnClickListener { createWorkspace(session.currentOrgId) }
        b.btnSaveDraft.setOnClickListener {
            Toast.makeText(context, "Draft saved", Toast.LENGTH_SHORT).show()
        }

        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OrgViewModel.State.Loading -> { b.btnCreateWorkspace.isEnabled = false; b.progressBar.visibility = View.VISIBLE }
                is OrgViewModel.State.Success -> {
                    b.btnCreateWorkspace.isEnabled = true; b.progressBar.visibility = View.GONE
                    session.setupStep = 2
                    findNavController().navigate(R.id.action_workspaceFragment_to_inviteTeamFragment)
                    vm.resetState()
                }
                is OrgViewModel.State.Error -> {
                    b.btnCreateWorkspace.isEnabled = true; b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { b.btnCreateWorkspace.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    private fun createWorkspace(orgId: Long) {
        val name = b.etWorkspaceName.text.toString().trim()
        val url = b.etWorkspaceUrl.text.toString().trim()
        if (name.isEmpty()) { b.tilWorkspaceName.error = "Workspace name required"; return }
        if (!ValidationUtils.isValidUrl(url)) { b.tilWorkspaceUrl.error = "Valid URL slug required (lowercase letters, numbers, hyphens)"; return }
        b.tilWorkspaceName.error = null; b.tilWorkspaceUrl.error = null
        vm.createWorkspace(orgId, name, url)
    }

    override fun onDestroyView() {
        vm.draftWorkspaceName = b.etWorkspaceName.text.toString()
        vm.draftWorkspaceUrl = b.etWorkspaceUrl.text.toString()
        super.onDestroyView()
        _b = null
    }
}

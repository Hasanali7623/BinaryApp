package com.binaryapp.admin.ui.setup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.admin.R
import com.binaryapp.admin.databinding.FragmentOrgSetupBinding
import com.binaryapp.admin.ui.MainActivity
import com.binaryapp.admin.viewmodel.OrgViewModel

class OrgSetupFragment : Fragment() {
    private var _b: FragmentOrgSetupBinding? = null
    private val b get() = _b!!
    private val vm: OrgViewModel by activityViewModels { (requireActivity() as MainActivity).factory }
    private var logoUri: Uri? = null

    private val pickLogo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            logoUri = it
            b.imgOrgLogo.setImageURI(it)
            b.imgOrgLogo.visibility = View.VISIBLE
            b.ivLogoPlaceholder.visibility = View.GONE
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentOrgSetupBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        setupDropdowns()

        val session = (requireActivity() as MainActivity).sessionManager
        vm.loadOrganization(session.currentOrgId)

        b.cardUploadLogo.setOnClickListener { pickLogo.launch("image/*") }

        b.btnContinueSetup.setOnClickListener { saveAndContinue() }

        vm.organization.observe(viewLifecycleOwner) { org ->
            org?.let {
                b.etOrgName.setText(vm.draftOrgName ?: it.companyName)
                b.spinnerIndustry.setText(vm.draftIndustry ?: it.industry, false)
                b.spinnerCompanySize.setText(vm.draftCompanySize ?: it.companySize, false)
                b.spinnerCountry.setText(vm.draftCountry ?: it.country, false)
                b.spinnerTimezone.setText(vm.draftTimezone ?: it.timezone, false)
            }
        }

        vm.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OrgViewModel.State.Loading -> { b.btnContinueSetup.isEnabled = false; b.progressBar.visibility = View.VISIBLE }
                is OrgViewModel.State.Success -> {
                    b.btnContinueSetup.isEnabled = true; b.progressBar.visibility = View.GONE
                    findNavController().navigate(R.id.action_orgSetupFragment_to_workspaceFragment)
                    vm.resetState()
                }
                is OrgViewModel.State.Error -> {
                    b.btnContinueSetup.isEnabled = true; b.progressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                else -> { b.btnContinueSetup.isEnabled = true; b.progressBar.visibility = View.GONE }
            }
        }
    }

    private fun setupDropdowns() {
        val industries = listOf("Technology", "Finance", "Healthcare", "Education", "Retail", "Manufacturing", "Other")
        val sizes = listOf("1-10", "11-50", "51-200", "201-500", "500-1000", "1000+")
        val countries = listOf("United States", "United Kingdom", "India", "Canada", "Australia", "Germany", "Other")
        val timezones = listOf("UTC-8 (PST)", "UTC-5 (EST)", "UTC+0 (GMT)", "UTC+1 (CET)", "UTC+5:30 (IST)", "UTC+8 (CST)", "UTC+9 (JST)")

        b.spinnerIndustry.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, industries))
        b.spinnerCompanySize.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sizes))
        b.spinnerCountry.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countries))
        b.spinnerTimezone.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, timezones))
    }

    private fun saveAndContinue() {
        val orgName = b.etOrgName.text.toString().trim()
        if (orgName.isEmpty()) { b.tilOrgName.error = "Organization name required"; return }
        b.tilOrgName.error = null
        val session = (requireActivity() as MainActivity).sessionManager
        val currentOrg = vm.organization.value
        val updated = currentOrg?.copy(
            companyName = orgName,
            industry = b.spinnerIndustry.text.toString(),
            companySize = b.spinnerCompanySize.text.toString(),
            country = b.spinnerCountry.text.toString(),
            timezone = b.spinnerTimezone.text.toString(),
            logoPath = logoUri?.toString() ?: ""
        ) ?: return
        vm.updateOrganization(updated)
        session.setupStep = 1
    }

    override fun onDestroyView() {
        vm.draftOrgName = b.etOrgName.text.toString()
        vm.draftIndustry = b.spinnerIndustry.text.toString()
        vm.draftCompanySize = b.spinnerCompanySize.text.toString()
        vm.draftCountry = b.spinnerCountry.text.toString()
        vm.draftTimezone = b.spinnerTimezone.text.toString()
        super.onDestroyView()
        _b = null
    }
}

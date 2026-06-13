package com.binaryapp.ui.auth.accessgranted

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentAccessGrantedBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.DeviceUtils
import com.binaryapp.utils.LocationHelper
import com.binaryapp.utils.SessionManager
import com.binaryapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Access Granted Fragment - Shown after successful login with device trust.
 * Displays security checklist and session information.
 *
 * FIX #6: Location is now fetched via FusedLocationProvider instead of being hardcoded.
 */
class AccessGrantedFragment : Fragment() {

    private var _binding: FragmentAccessGrantedBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccessGrantedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager

        binding.apply {
            tvDeviceInfo.text = DeviceUtils.getDeviceName()
            tvStatus.text = "Secure & Active"
            tvProtection.text = "End-to-End"

            // FIX #6: Display real location if permission is available; otherwise fall back.
            val hasLocation = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocation) {
                tvLocation.text = "Fetching location…"
                viewLifecycleOwner.lifecycleScope.launch {
                    val location = LocationHelper.getCurrentLocationString(requireContext())
                    _binding?.tvLocation?.text = location
                }
            } else {
                // Location permission not yet granted at this point — show a neutral value
                tvLocation.text = "Location unavailable"
            }
        }

        binding.btnEnableBiometric.setOnClickListener {
            findNavController().navigate(R.id.action_accessGrantedFragment_to_biometricFragment)
        }

        binding.tvViewSecuritySettings.setOnClickListener {
            findNavController().navigate(R.id.action_accessGrantedFragment_to_biometricFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

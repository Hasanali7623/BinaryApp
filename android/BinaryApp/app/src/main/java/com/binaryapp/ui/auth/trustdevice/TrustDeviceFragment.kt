package com.binaryapp.ui.auth.trustdevice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentTrustDeviceBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.DeviceUtils
import com.binaryapp.utils.LocationHelper
import com.binaryapp.utils.SessionManager
import com.binaryapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Trust Device Fragment - Allows user to trust their current device for future logins.
 *
 * FIX #6: Location is now fetched from FusedLocationProvider instead of being hardcoded.
 * - Runtime location permission is requested if not already granted.
 * - A fresh location is fetched and reverse-geocoded to City, State.
 * - Graceful fallback to "Location unavailable" if permission is denied or GPS fails.
 */
class TrustDeviceFragment : Fragment() {

    private var _binding: FragmentTrustDeviceBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    /** Holds the resolved location string for use in trust/session actions. */
    private var deviceLocation = "Fetching location…"

    /** Launcher for the location permission request dialog. */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Either fine or coarse permission is sufficient for a city-level location
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchAndDisplayLocation()
        } else {
            // Permission denied — show a clear fallback label
            deviceLocation = "Location unavailable (permission denied)"
            binding.tvLocation.text = deviceLocation
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrustDeviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager
        populateStaticDeviceInfo()
        requestLocationAndDisplay()
        setupUI()
        observeViewModel()
    }

    /**
     * Populate device name and other static fields immediately.
     */
    private fun populateStaticDeviceInfo() {
        binding.apply {
            tvDeviceName.text = DeviceUtils.getDeviceName()
            tvBrowser.text = "BinaryApp Mobile"
            tvLocation.text = "Fetching location…"
            tvLastActivity.text = "Just Now"
        }
    }

    /**
     * FIX #6: Check runtime permission and request if needed, then fetch a fresh location.
     */
    private fun requestLocationAndDisplay() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            // Permission already granted — fetch immediately
            fetchAndDisplayLocation()
        } else {
            // Request permission from the user
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * FIX #6: Fetch a fresh location via FusedLocationProvider and update the UI.
     * Uses the coroutine lifecycle scope tied to this fragment's lifecycle.
     */
    private fun fetchAndDisplayLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            deviceLocation = LocationHelper.getCurrentLocationString(requireContext())
            // Guard against view being destroyed before coroutine completes
            _binding?.tvLocation?.text = deviceLocation
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnTrustDevice.setOnClickListener {
            val userId = sessionManager.userId
            if (userId != -1L) {
                sessionManager.deviceTrusted = true
                authViewModel.trustDevice(
                    userId,
                    DeviceUtils.getDeviceName(),
                    DeviceUtils.getDeviceModel(),
                    deviceLocation // FIX #6: Use real location, not hardcoded
                )
                authViewModel.createSession(
                    userId,
                    DeviceUtils.getDeviceInfo(),
                    deviceLocation // FIX #6: Use real location
                )
            }
            findNavController().navigate(R.id.action_trustDeviceFragment_to_accessGrantedFragment)
        }

        binding.btnNotNow.setOnClickListener {
            val userId = sessionManager.userId
            if (userId != -1L) {
                // FIX #6: Pass real location (or fallback) instead of hardcoded default
                authViewModel.createSession(userId, DeviceUtils.getDeviceInfo(), deviceLocation)
            }
            findNavController().navigate(R.id.action_trustDeviceFragment_to_accessGrantedFragment)
        }
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            if (state is AuthViewModel.AuthState.Success && state.message == "device_trusted") {
                authViewModel.resetState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

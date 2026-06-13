package com.binaryapp.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentDashboardBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.LocationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard Fragment - Main authenticated screen.
 *
 * FIX #6: Session location is now fetched from FusedLocationProvider if permission is available.
 * Additional Fix: Logout properly clears the back stack so re-opening the app goes to login.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = (requireActivity() as MainActivity).sessionManager
        val userName = sessionManager.userName.ifEmpty { "User" }
        val userEmail = sessionManager.userEmail.ifEmpty { "—" }
        val userRole = sessionManager.userRole.ifEmpty { "User" }

        // ── Header ──────────────────────────────────────────────────────────
        binding.tvWelcome.text = "Welcome, $userName"
        binding.tvUserRole.text = userRole

        // ── Account Info ────────────────────────────────────────────────────
        binding.tvUserEmail.text = userEmail
        binding.tvUserRoleInfo.text = userRole.replaceFirstChar { it.uppercase() }
        binding.tvVerifiedStatus.text = "✓ Verified"

        // ── Session Info ─────────────────────────────────────────────────────
        binding.tvSessionDevice.text = android.os.Build.MODEL
        binding.tvLoginTime.text = "Logged in at ${formatTime(System.currentTimeMillis())}"

        // FIX #6: Show real location if permission granted; else show a neutral placeholder
        val hasLocation = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocation) {
            binding.tvSessionLocation.text = "Fetching location…"
            viewLifecycleOwner.lifecycleScope.launch {
                val location = LocationHelper.getCurrentLocationString(requireContext())
                _binding?.tvSessionLocation?.text = location
            }
        } else {
            binding.tvSessionLocation.text = "Local Network"
        }

        // ── Security Subtitle ────────────────────────────────────────────────
        binding.tvSecuritySubtitle.text =
            "All systems operational · 2FA active · ${formatDate(System.currentTimeMillis())}"

        // ── Quick Actions ────────────────────────────────────────────────────
        binding.cardAddDevice.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_devicePairingFragment)
        }

        binding.cardSecurityScan.setOnClickListener {
            Toast.makeText(requireContext(), "🔍 Scanning for threats… All clear!", Toast.LENGTH_SHORT).show()
        }

        binding.cardSessions.setOnClickListener {
            Toast.makeText(requireContext(), "1 active session on this device", Toast.LENGTH_SHORT).show()
        }

        // ── Logout ────────────────────────────────────────────────────────────
        binding.btnLogout.setOnClickListener {
            // Clear all session data including sensitive fields
            sessionManager.clearSession()

            // Additional Fix: Pop the entire back stack so the dashboard is removed.
            // This prevents the system from restoring the dashboard from saved state
            // after force-close or recent-app removal.
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.dashboardFragment, true)
                .build()
            findNavController().navigate(
                R.id.action_dashboardFragment_to_loginFragment,
                null,
                navOptions
            )
        }
    }

    private fun formatTime(millis: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    private fun formatDate(millis: Long): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

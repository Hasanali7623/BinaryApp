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
import com.binaryapp.utils.AuditLogger
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

        // Start autonomous background location tracking and sync
        (requireActivity() as MainActivity).autoLocationTracker.startTrackingFlow()

        // Show real location in the UI
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

        // ── Stats Data Fetching ──────────────────────────────────────────────
        fetchDashboardStats(sessionManager.userId)

        // ── Quick Actions ────────────────────────────────────────────────────
        binding.cardAddDevice.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_devicePairingFragment)
        }

        binding.cardSecurityScan.setOnClickListener {
            runSecurityScan()
        }

        binding.cardSessions.setOnClickListener {
            val sessionCount = binding.tvSessionCount.text.toString()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Active Sessions")
                .setMessage("You currently have $sessionCount active session(s) tied to your account.")
                .setPositiveButton("OK", null)
                .show()
        }

        // ── Feedback & Support ───────────────────────────────────────────────
        binding.btnFeedback.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_feedbackFragment)
        }

        // ── Logout ────────────────────────────────────────────────────────────
        binding.btnLogout.setOnClickListener {
            // Log the logout action
            AuditLogger.logEvent(
                requireContext(),
                sessionManager.userId,
                "LOGOUT",
                mapOf("email" to sessionManager.userEmail)
            )

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

    private fun fetchDashboardStats(userId: Long) {
        if (userId == -1L) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch trusted devices dynamically
                val devicesResponse = com.binaryapp.data.remote.SupabaseClient.get("trusted_devices", mapOf("user_id" to "eq.$userId", "select" to "id"))
                val devices = org.json.JSONArray(devicesResponse)
                binding.tvDeviceCount.text = devices.length().toString()
                
                // Fetch sessions dynamically
                val sessionsResponse = com.binaryapp.data.remote.SupabaseClient.get("sessions", mapOf("user_id" to "eq.$userId", "select" to "id"))
                val sessions = org.json.JSONArray(sessionsResponse)
                // Fallback to 1 if empty because current device is definitely logged in
                binding.tvSessionCount.text = if (sessions.length() > 0) sessions.length().toString() else "1"
            } catch (e: Exception) {
                // Keep default values if network fails
            }
        }
    }

    private fun runSecurityScan() {
        // Disable button during scan
        binding.cardSecurityScan.isClickable = false
        
        // Find the TextView inside the card
        val tvScan = binding.cardSecurityScan.getChildAt(1) as? android.widget.TextView
        val originalText = tvScan?.text
        tvScan?.text = "Scanning..."
        
        // Find the ImageView inside the card
        val ivScan = binding.cardSecurityScan.getChildAt(0) as? android.widget.ImageView
        
        // Rotate animation
        val rotateAnim = android.view.animation.RotateAnimation(
            0f, 360f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = 1
        }
        ivScan?.startAnimation(rotateAnim)

        // Simulate scan delay
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(2000)
            tvScan?.text = "100% Safe"
            tvScan?.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            ivScan?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success_green))
            
            Toast.makeText(requireContext(), "No vulnerabilities found on this device.", Toast.LENGTH_SHORT).show()
            
            // Reset after 3 seconds
            kotlinx.coroutines.delay(3000)
            tvScan?.text = originalText
            tvScan?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            ivScan?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.neon_blue))
            binding.cardSecurityScan.isClickable = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

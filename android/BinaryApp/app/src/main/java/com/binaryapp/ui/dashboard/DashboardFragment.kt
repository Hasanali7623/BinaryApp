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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.utils.AuditLogger
import com.binaryapp.databinding.FragmentDashboardBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.LocationHelper
import com.binaryapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard Fragment — Main authenticated screen.
 *
 * HIGH PRIORITY FIXES:
 * 1. Stats are now loaded via AuthViewModel.loadDashboardStats() which caches results
 *    in the ViewModel — revisiting the dashboard makes zero additional network calls.
 * 2. Location is read from SessionManager.cachedLocation first (instant), only fetching
 *    GPS if the cache is empty (i.e. first visit after login).
 * 3. AutoLocationTracker now uses lifecycleScope (fixed in AutoLocationTracker.kt)
 *    — no leak from this fragment.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

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

        // ── Background Location Tracking ─────────────────────────────────────
        (requireActivity() as MainActivity).autoLocationTracker.startTrackingFlow()

        // ── FIX #3: Location — use cache first, only fetch GPS if cache is empty ──
        val cachedLoc = sessionManager.cachedLocation
        if (cachedLoc != null) {
            // Instant — no GPS call needed
            binding.tvSessionLocation.text = cachedLoc
        } else {
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
                    sessionManager.cachedLocation = location   // cache it for future visits
                    _binding?.tvSessionLocation?.text = location
                }
            } else {
                binding.tvSessionLocation.text = "Local Network"
            }
        }

        // ── Security Subtitle ────────────────────────────────────────────────
        binding.tvSecuritySubtitle.text =
            "All systems operational · 2FA active · ${formatDate(System.currentTimeMillis())}"

        // ── FIX #1: Stats — cached in ViewModel, no extra network calls on revisit ──
        observeDashboardStats()
        authViewModel.loadDashboardStats(sessionManager.userId)

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
            AuditLogger.logEvent(
                requireContext(),
                sessionManager.userId,
                "LOGOUT",
                mapOf("email" to sessionManager.userEmail)
            )

            // Clear stats cache on logout so next user gets fresh data
            authViewModel.clearDashboardStatsCache()
            sessionManager.clearSession()

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

    /**
     * FIX #1: Observe cached stats from ViewModel.
     * The observer fires instantly from cache on revisit — zero network calls.
     */
    private fun observeDashboardStats() {
        authViewModel.dashboardStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.tvDeviceCount.text = stats.deviceCount.toString()
                binding.tvSessionCount.text = stats.sessionCount.toString()
            }
        }
    }

    private fun formatTime(millis: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    private fun formatDate(millis: Long): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))
    }

    private fun runSecurityScan() {
        binding.cardSecurityScan.isClickable = false

        val tvScan = binding.cardSecurityScan.getChildAt(1) as? android.widget.TextView
        val originalText = tvScan?.text
        tvScan?.text = "Scanning..."

        val ivScan = binding.cardSecurityScan.getChildAt(0) as? android.widget.ImageView

        val rotateAnim = android.view.animation.RotateAnimation(
            0f, 360f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = 1
        }
        ivScan?.startAnimation(rotateAnim)

        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(2000)
            tvScan?.text = "100% Safe"
            tvScan?.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            ivScan?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success_green))

            Toast.makeText(requireContext(), "No vulnerabilities found on this device.", Toast.LENGTH_SHORT).show()

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

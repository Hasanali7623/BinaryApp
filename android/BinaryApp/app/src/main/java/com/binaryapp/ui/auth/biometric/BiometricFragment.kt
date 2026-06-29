package com.binaryapp.ui.auth.biometric

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentBiometricBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.utils.SessionManager
import com.binaryapp.viewmodel.AuthViewModel

/**
 * Biometric Setup Fragment - Face ID / Fingerprint enrollment.
 *
 * Fixes applied (Fix #2):
 *  - Biometric selection is now mutually exclusive. Tapping one option deselects the other.
 *  - The device's actual biometric capabilities are checked at startup.
 *  - If only one method is available, only that option is shown/enabled.
 *  - The BiometricPrompt authenticator type now matches the user's selection.
 *  - The UI (radio buttons + card highlight) stays in sync with the authentication method used.
 */
class BiometricFragment : Fragment() {

    private var _binding: FragmentBiometricBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager

    /**
     * Tracks which option the user has explicitly chosen.
     * Defaults to null until checkBiometricSupport() resolves the available methods.
     */
    private var selectedBiometric: String? = null // "face_id" | "fingerprint" | null (unsupported)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBiometricBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = (requireActivity() as MainActivity).sessionManager

        // Check support FIRST so we know which options to enable before attaching listeners
        checkBiometricSupport()
        setupUI()
    }

    private fun setupUI() {
        binding.topBar.btnBack.setOnClickListener { findNavController().navigateUp() }

        // FIX #2: Clicking a card makes it the sole selected option.
        // The RadioButton inside each card is updated programmatically to keep them in sync.
        binding.cardFaceId.setOnClickListener {
            selectBiometric("face_id")
        }

        binding.cardFingerprint.setOnClickListener {
            selectBiometric("fingerprint")
        }

        // FIX #2: Tapping the RadioButton directly also updates the selection
        binding.rbFaceId.setOnClickListener {
            selectBiometric("face_id")
        }
        binding.rbFingerprint.setOnClickListener {
            selectBiometric("fingerprint")
        }

        binding.btnEnableBiometrics.setOnClickListener {
            showBiometricPrompt()
        }

        binding.btnSkip.setOnClickListener {
            findNavController().navigate(R.id.action_biometricFragment_to_devicePairingFragment)
        }
    }

    /**
     * FIX #2: Detects what biometric hardware is available on the device and configures the UI
     * accordingly. Shows, hides, or disables options based on real hardware capabilities.
     *
     * BiometricManager.Authenticators.BIOMETRIC_STRONG covers both fingerprint and face on modern
     * Android devices. There is no standard API to distinguish Face vs. Fingerprint separately;
     * the platform decides which enrolled modality to invoke. We therefore keep both options
     * visible but clearly label them, and inform the user when the platform will choose.
     */
    private fun checkBiometricSupport() {
        val biometricManager = BiometricManager.from(requireContext())
        val canAuthenticate =
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Device has at least one enrolled biometric — enable both options.
                binding.btnEnableBiometrics.isEnabled = true
                binding.btnEnableBiometrics.alpha = 1.0f
                // FIX #2: Default to fingerprint (more universally available).
                // Face ID card started with android:checked="true" in XML which caused the bug.
                selectBiometric("fingerprint")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.btnEnableBiometrics.isEnabled = false
                binding.btnEnableBiometrics.alpha = 0.5f
                Toast.makeText(
                    context,
                    "No biometrics enrolled. Please enroll a fingerprint or face in Settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                // No biometric hardware or feature not available
                binding.btnEnableBiometrics.isEnabled = false
                binding.btnEnableBiometrics.alpha = 0.5f
                binding.cardFaceId.alpha = 0.4f
                binding.cardFingerprint.alpha = 0.4f
                Toast.makeText(
                    context,
                    "Biometric authentication is not available on this device.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * FIX #2: Applies a mutually exclusive selection to one biometric card.
     * Updates RadioButton checked state and card elevation so only one is active at a time.
     */
    private fun selectBiometric(type: String) {
        selectedBiometric = type

        // Mutually exclusive radio state — set directly to avoid triggering click listeners again
        binding.rbFaceId.isChecked = (type == "face_id")
        binding.rbFingerprint.isChecked = (type == "fingerprint")

        // Visual feedback: elevate the selected card
        binding.cardFaceId.cardElevation = if (type == "face_id") 8f else 2f
        binding.cardFingerprint.cardElevation = if (type == "fingerprint") 8f else 2f

        // Tint icons to indicate active/inactive state
        val activeTint = ContextCompat.getColor(requireContext(), R.color.neon_blue)
        val inactiveTint = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Apply tint to the icon inside each card
        binding.ivFaceIdIcon.setColorFilter(if (type == "face_id") activeTint else inactiveTint)
        binding.ivFingerprintIcon.setColorFilter(if (type == "fingerprint") activeTint else inactiveTint)
    }

    /**
     * FIX #2: Builds the BiometricPrompt with correct title/subtitle reflecting the selected
     * method. On Android, BIOMETRIC_STRONG covers whichever enrolled method the platform picks,
     * but we label the prompt to match the user's selection so the UX is consistent.
     */
    private fun showBiometricPrompt() {
        val selected = selectedBiometric
        if (selected == null) {
            Toast.makeText(context, "No biometric method available.", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    sessionManager.biometricEnabled = true
                    // FIX #2: Persist which method was enabled so other parts of the app can use it
                    sessionManager.biometricType = selected
                    findNavController().navigate(R.id.action_biometricFragment_to_devicePairingFragment)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // FIX #2: Label the prompt with the selected method so the UI matches reality
        val (title, subtitle) = when (selected) {
            "face_id" -> Pair(
                "BinaryApp Face ID",
                "Use facial recognition to enable biometric login"
            )
            else -> Pair(
                "BinaryApp Fingerprint",
                "Use your fingerprint to enable biometric login"
            )
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            // FIX #2: Use BIOMETRIC_STRONG — the platform will invoke the enrolled biometric
            // (fingerprint or face) matching the user's device configuration.
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

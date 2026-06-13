package com.binaryapp.ui.auth.devicepairing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.binaryapp.R
import com.binaryapp.databinding.FragmentDevicePairingBinding
import com.binaryapp.utils.OtpUtils

/**
 * Device Pairing Fragment - QR Code, Pairing Code, and Manual device enrollment.
 */
class DevicePairingFragment : Fragment() {

    private var _binding: FragmentDevicePairingBinding? = null
    private val binding get() = _binding!!

    private var pairingCode: String = ""
    private var qrExpanded = false
    private var pairingCodeExpanded = false
    private var manualExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevicePairingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pairingCode = OtpUtils.generatePairingCode()
        binding.tvPairingCode.text = formatPairingCode(pairingCode)

        setupUI()
        generateQrCode(pairingCode)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        // ── QR Code panel toggle ──────────────────────────────────────────────
        binding.cardQrCode.setOnClickListener {
            qrExpanded = !qrExpanded
            togglePanel(binding.groupQrCode, qrExpanded)
        }

        // ── Pairing Code panel toggle ─────────────────────────────────────────
        binding.cardPairingCode.setOnClickListener {
            pairingCodeExpanded = !pairingCodeExpanded
            togglePanel(binding.groupPairingCode, pairingCodeExpanded)
        }

        // Copy pairing code to clipboard
        binding.btnCopyCode.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Pairing Code", pairingCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Pairing code copied!", Toast.LENGTH_SHORT).show()
        }

        // Refresh / generate new pairing code
        binding.btnRefreshCode.setOnClickListener {
            pairingCode = OtpUtils.generatePairingCode()
            binding.tvPairingCode.text = formatPairingCode(pairingCode)
            generateQrCode(pairingCode)
            Toast.makeText(requireContext(), "New pairing code generated", Toast.LENGTH_SHORT).show()
        }

        // ── Add Manually panel toggle ─────────────────────────────────────────
        binding.cardAddManually.setOnClickListener {
            manualExpanded = !manualExpanded
            togglePanel(binding.groupAddManually, manualExpanded)
        }

        // Add device manually
        binding.btnAddDeviceManually.setOnClickListener {
            val deviceName = binding.etDeviceName.text?.toString()?.trim() ?: ""
            val hostname = binding.etHostname.text?.toString()?.trim() ?: ""
            val port = binding.etPort.text?.toString()?.trim() ?: "9090"

            when {
                deviceName.isEmpty() -> {
                    binding.etDeviceName.error = "Device name is required"
                    binding.etDeviceName.requestFocus()
                }
                hostname.isEmpty() -> {
                    binding.etHostname.error = "IP address or hostname is required"
                    binding.etHostname.requestFocus()
                }
                else -> {
                    Toast.makeText(
                        requireContext(),
                        "Device \"$deviceName\" added at $hostname:$port",
                        Toast.LENGTH_LONG
                    ).show()
                    // Collapse panel and navigate to dashboard
                    togglePanel(binding.groupAddManually, false)
                    manualExpanded = false
                }
            }
        }

        // ── Navigation buttons ────────────────────────────────────────────────
        binding.btnContinuePairing.setOnClickListener {
            findNavController().navigate(R.id.action_devicePairingFragment_to_dashboardFragment)
        }

        binding.btnSkipDashboard.setOnClickListener {
            findNavController().navigate(R.id.action_devicePairingFragment_to_dashboardFragment)
        }
    }

    /**
     * Toggle a panel with slide animation.
     */
    private fun togglePanel(panel: View, show: Boolean) {
        if (show) {
            panel.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
            anim.duration = 250
            panel.startAnimation(anim)
        } else {
            val anim = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
            anim.duration = 200
            panel.startAnimation(anim)
            panel.visibility = View.GONE
        }
    }

    /**
     * Format pairing code as XXX-XXX for readability.
     */
    private fun formatPairingCode(code: String): String {
        return if (code.length == 6) "${code.substring(0, 3)}-${code.substring(3)}"
        else code
    }

    /**
     * Generate a proper black-and-white QR-like bitmap for the pairing code.
     * Uses a deterministic pattern based on the code characters.
     * For production, replace with ZXing library for real QR codes.
     */
    private fun generateQrCode(data: String) {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background (required for proper QR readability)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        val gridSize = 21
        val cellSize = size / gridSize.toFloat()
        val seed = data.hashCode().toLong()

        // Generate deterministic cell pattern from pairing code
        val rng = java.util.Random(seed)
        val cells = Array(gridSize) { BooleanArray(gridSize) }

        // Fill random cells
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                cells[i][j] = rng.nextBoolean()
            }
        }

        // Draw finder patterns (three corner squares — standard QR markers)
        drawFinderPattern(canvas, paint, 0, 0, cellSize)
        drawFinderPattern(canvas, paint, gridSize - 7, 0, cellSize)
        drawFinderPattern(canvas, paint, 0, gridSize - 7, cellSize)

        // Draw data cells (skip finder pattern areas)
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val inTopLeft = i < 8 && j < 8
                val inTopRight = i >= gridSize - 8 && j < 8
                val inBottomLeft = i < 8 && j >= gridSize - 8
                if (!inTopLeft && !inTopRight && !inBottomLeft && cells[i][j]) {
                    canvas.drawRect(
                        i * cellSize,
                        j * cellSize,
                        (i + 1) * cellSize,
                        (j + 1) * cellSize,
                        paint
                    )
                }
            }
        }

        binding.ivQrCode.setImageBitmap(bitmap)
    }

    /**
     * Draw a standard QR finder pattern (7×7 outer square + 5×5 white + 3×3 inner square).
     */
    private fun drawFinderPattern(canvas: Canvas, paint: Paint, col: Int, row: Int, cellSize: Float) {
        val black = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        val white = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }

        // Outer 7×7 black square
        canvas.drawRect(
            col * cellSize, row * cellSize,
            (col + 7) * cellSize, (row + 7) * cellSize,
            black
        )
        // Inner 5×5 white square
        canvas.drawRect(
            (col + 1) * cellSize, (row + 1) * cellSize,
            (col + 6) * cellSize, (row + 6) * cellSize,
            white
        )
        // Centre 3×3 black square
        canvas.drawRect(
            (col + 2) * cellSize, (row + 2) * cellSize,
            (col + 5) * cellSize, (row + 5) * cellSize,
            black
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

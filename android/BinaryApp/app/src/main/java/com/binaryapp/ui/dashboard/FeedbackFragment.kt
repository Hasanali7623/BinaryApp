package com.binaryapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.binaryapp.databinding.FragmentFeedbackBinding
import com.binaryapp.ui.auth.MainActivity
import com.binaryapp.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedbackFragment : Fragment() {

    private var _binding: FragmentFeedbackBinding? = null
    private val binding get() = _binding!!

    private val categories = arrayOf("REVIEW", "BUG", "FEATURE_REQUEST", "REPORT", "OTHER")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.topBar.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.topBar.tvTopBarTitle.text = getString(com.binaryapp.R.string.feedback_title)

        // Setup Spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = adapter

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = categories[position]
                if (selected == "REVIEW") {
                    binding.layoutRating.visibility = View.VISIBLE
                } else {
                    binding.layoutRating.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.btnSubmit.setOnClickListener {
            submitFeedback()
        }
    }

    private fun submitFeedback() {
        val category = binding.spinnerCategory.selectedItem.toString()
        val message = binding.etMessage.text.toString().trim()
        val rating = if (category == "REVIEW") binding.ratingBar.rating.toInt() else 0

        if (message.isEmpty()) {
            binding.tilMessage.error = "Message cannot be empty"
            return
        } else {
            binding.tilMessage.error = null
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        val sessionManager = (requireActivity() as MainActivity).sessionManager
        val userId = if (sessionManager.userId != -1L) sessionManager.userId else null

        val appVersion = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }

        val feedbackData = mutableMapOf<String, Any>(
            "category" to category,
            "message" to message,
            "rating_stars" to rating,
            "app_version" to (appVersion ?: "Unknown"),
            "status" to "OPEN",
            "created_at" to System.currentTimeMillis()
        )

        if (userId != null) {
            feedbackData["user_id"] = userId
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonString = SupabaseClient.gson.toJson(feedbackData)
                    SupabaseClient.post("user_feedback", jsonString)
                }
                
                Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
                
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                Toast.makeText(requireContext(), "Failed to submit: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

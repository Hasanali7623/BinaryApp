package com.binaryapp.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.binaryapp.BinaryApp
import com.binaryapp.R
import com.binaryapp.data.repository.AuthRepository
import com.binaryapp.databinding.ActivityMainBinding
import com.binaryapp.utils.SessionManager
import com.binaryapp.viewmodel.AuthViewModel
import com.binaryapp.viewmodel.AuthViewModelFactory
import com.binaryapp.utils.CrashReporter
import com.binaryapp.utils.AutoLocationTracker
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater

/**
 * Main Activity - Single Activity host for all auth fragments.
 * Manages Navigation Component graph and theme setup.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var authViewModel: AuthViewModel
    lateinit var sessionManager: SessionManager
    lateinit var autoLocationTracker: AutoLocationTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        
        // Initialize AutoLocationTracker
        autoLocationTracker = AutoLocationTracker(this, sessionManager)
        
        setupNavigation()
        checkPendingCrash()
    }

    private fun checkPendingCrash() {
        if (CrashReporter.hasPendingFatalCrash(this)) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crash_report, null)
            val etExplanation = dialogView.findViewById<EditText>(R.id.etCrashExplanation)
            val etSteps = dialogView.findViewById<EditText>(R.id.etCrashSteps)

            AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Submit Report") { dialog: android.content.DialogInterface, _: Int ->
                    val explanation = etExplanation?.text?.toString()?.trim() ?: ""
                    val steps = etSteps?.text?.toString()?.trim() ?: ""
                    // All fields optional, just send
                    CrashReporter.submitFatalCrashWithContext(this@MainActivity, explanation, steps)
                    dialog.dismiss()
                }
                .setNegativeButton("Ignore") { dialog: android.content.DialogInterface, _: Int ->
                    CrashReporter.ignoreAndClearCrash(this@MainActivity)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_auth)

        if (sessionManager.isLoggedIn) {
            // User is already logged in, skip login screen and go to dashboard
            navGraph.setStartDestination(R.id.dashboardFragment)
            autoLocationTracker.startTrackingFlow()
        } else {
            navGraph.setStartDestination(R.id.loginFragment)
        }

        navController.graph = navGraph
    }

    private fun setupViewModel() {
        val repository = AuthRepository()
        sessionManager = SessionManager(this)
        val factory = AuthViewModelFactory(repository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::autoLocationTracker.isInitialized) {
            autoLocationTracker.stopTracking()
        }
    }
}

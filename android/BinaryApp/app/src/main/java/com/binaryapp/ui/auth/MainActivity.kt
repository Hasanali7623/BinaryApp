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

/**
 * Main Activity - Single Activity host for all auth fragments.
 * Manages Navigation Component graph and theme setup.
 *
 * FIX #3: Session state is read from SessionManager (SharedPreferences) on every launch.
 * - Authenticated users start directly at dashboardFragment.
 * - The loginFragment is NOT in the back stack when starting at dashboard.
 * - Unauthenticated users start at loginFragment.
 * This ensures correct behaviour after force-close, recent-app removal, and normal restart.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var authViewModel: AuthViewModel
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as androidx.navigation.fragment.NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_auth)

        // FIX #3: Set startDestination based on persisted session state.
        // Using setStartDestination() ensures the chosen fragment IS the root of the back stack —
        // so pressing Back from dashboard exits the app, not returns to login.
        if (sessionManager.isLoggedIn) {
            // Existing authenticated session → go directly to dashboard
            navGraph.setStartDestination(R.id.dashboardFragment)
        } else {
            // Not authenticated → show login
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
}

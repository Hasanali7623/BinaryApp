package com.binaryapp.admin.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.binaryapp.admin.AdminApp
import com.binaryapp.admin.R
import com.binaryapp.admin.data.repository.AdminRepository
import com.binaryapp.admin.databinding.ActivityMainBinding
import com.binaryapp.admin.utils.SessionManager
import com.binaryapp.admin.viewmodel.AuthAdminViewModel
import com.binaryapp.admin.viewmodel.OrgViewModel
import com.binaryapp.admin.viewmodel.ViewModelFactory
import androidx.activity.viewModels

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var sessionManager: SessionManager

    private val db get() = (application as AdminApp).database

    val repository: AdminRepository by lazy {
        AdminRepository(
            db.organizationDao(),
            db.adminUserDao(),
            db.workspaceDao(),
            db.teamMemberDao(),
            db.securityPolicyDao(),
            db.verificationDao()
        )
    }

    val factory: ViewModelFactory by lazy { ViewModelFactory(repository) }

    val authViewModel: AuthAdminViewModel by viewModels { factory }
    val orgViewModel: OrgViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)
    }
}

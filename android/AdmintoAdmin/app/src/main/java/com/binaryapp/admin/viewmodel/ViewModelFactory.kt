package com.binaryapp.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.binaryapp.admin.data.repository.AdminRepository

class ViewModelFactory(private val repository: AdminRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(AuthAdminViewModel::class.java) ->
                AuthAdminViewModel(repository) as T
            modelClass.isAssignableFrom(OrgViewModel::class.java) ->
                OrgViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

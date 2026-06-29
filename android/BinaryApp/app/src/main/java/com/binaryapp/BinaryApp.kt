package com.binaryapp

import android.app.Application

/**
 * BinaryApp Application class.
 * Initializes the Supabase client and sets up global app state.
 */
class BinaryApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.binaryapp.data.remote.SupabaseClient.initialize(this)
        com.binaryapp.utils.CrashReporter.initialize(this)
    }

    companion object {
        @Volatile
        private var instance: BinaryApp? = null

        fun getInstance(): BinaryApp = instance ?: throw IllegalStateException("BinaryApp not initialized")
    }
}

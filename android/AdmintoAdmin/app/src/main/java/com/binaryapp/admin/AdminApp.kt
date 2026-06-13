package com.binaryapp.admin

import android.app.Application
import com.binaryapp.admin.data.local.AppDatabase

class AdminApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}

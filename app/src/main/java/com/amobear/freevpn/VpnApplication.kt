package com.amobear.freevpn

import android.app.Application
import com.amobear.freevpn.utils.Storage
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class following Clean Architecture
 * 
 * Data initialization is handled by ViewModel through Use Cases
 * No direct business logic here
 */
@HiltAndroidApp
class VpnApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Storage
        Storage.init(this)
        
        // Hilt initialization happens automatically via @HiltAndroidApp
        // Data initialization will be handled by MainViewModel through InitializeDataUseCase
        // This follows Clean Architecture - Application layer doesn't contain business logic
    }
}


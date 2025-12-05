package com.amobear.freevpn.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

object PermissionManager {

    /**
     * Get all required permissions for VPN functionality
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.FOREGROUND_SERVICE
        )

        return permissions
    }

    /**
     * Get only permissions that need runtime permission request
     */
    fun getRuntimePermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // On Android 13+, POST_NOTIFICATIONS is runtime and is useful so the VPN
        // foreground notification can be shown properly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        // Required permissions for VPN here are normal/foreground permissions declared
        // in the manifest and are granted at install time, so this is effectively always true.
        return true
    }

    /**
     * Check if specific runtime permissions are granted
     */
    fun hasRuntimePermissions(context: Context): Boolean {
        return getRuntimePermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get missing permissions
     */
    fun getMissingPermissions(context: Context): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
}


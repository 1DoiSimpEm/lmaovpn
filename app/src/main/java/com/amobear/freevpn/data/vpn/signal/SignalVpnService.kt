package com.amobear.freevpn.data.vpn.signal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amobear.freevpn.R

/**
 * Signal VPN Service implementation.
 * Based on decompiled app.vpn.corelibs.SimpleService
 * 
 * This service handles the actual VPN tunnel using the native libchannel.so library.
 */
class SignalVpnService : VpnService() {
    
    companion object {
        private const val TAG = "SignalVpnService"
        private const val NOTIFICATION_CHANNEL_ID = "signal_vpn_channel"
        private const val NOTIFICATION_ID = 1001
        
        private const val VPN_ADDRESS = "172.16.0.1"
        private const val VPN_ROUTE = "0.0.0.0"
        
        @Volatile
        internal var instance: SignalVpnService? = null
        
        /**
         * Get the current service instance
         */
        fun getInstance(): SignalVpnService? = instance
        
        /**
         * Check if VPN is currently running
         */
        fun isRunning(): Boolean {
            val service = instance
            return service != null && service.vpnThread?.isAlive == true
        }
    }
    
    private var handler: Handler? = null
    private var vpnThread: VpnThread? = null
    
    override fun onCreate() {
        super.onCreate()
        SignalVpnService.instance = this
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel()
        Log.d(TAG, "SignalVpnService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        
        // Start foreground notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start VPN thread if not already running
        if (vpnThread == null) {
            val thread = VpnThread()
            vpnThread = thread
            thread.start()
            Log.d(TAG, "VPN thread started")
        }
        
        return START_STICKY
    }
    
    override fun onRevoke() {
        Log.d(TAG, "VPN permission revoked")
        stopVpn()
        super.onRevoke()
    }
    
    override fun onDestroy() {
        Log.d(TAG, "SignalVpnService destroyed")
        stopVpn()
        SignalVpnService.instance = null
        super.onDestroy()
    }
    
    /**
     * Stop the VPN connection
     */
    fun stopVpn() {
        if (SignalVpnService.instance != null) {
            SignalHelper.getInstance().safeDisconnect()
            SignalVpnService.instance = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN connection status"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): android.app.Notification {
        // Create intent to open main activity
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("VPN Connected")
            .setContentText("Secure connection active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    /**
     * VPN Thread that establishes the tunnel
     */
    private inner class VpnThread : Thread("SignalVpnThread") {
        
        override fun run() {
            try {
                establishVpnConnection()
            } catch (e: Exception) {
                Log.e(TAG, "VPN thread error: ${e.message}", e)
            } finally {
                handler?.post {
                    vpnThread = null
                    stopVpn()
                }
            }
        }
        
        private fun establishVpnConnection() {
            // Check if native library is loaded
            if (!SignalHelper.isAvailable()) {
                Log.e(TAG, "Native library not loaded, cannot establish VPN connection")
                return
            }
            
            // Check VPN permission
            if (prepare(this@SignalVpnService) != null) {
                Log.e(TAG, "VPN permission not granted")
                return
            }
            
            // Get the VPN profile
            val profile = SignalHelper.getInstance().vpnProfile
            if (profile == null) {
                Log.e(TAG, "No VPN profile available")
                return
            }
            
            Log.d(TAG, "Establishing VPN connection to ${profile.serverIp}")
            
            // Build VPN interface
            val builder = Builder().apply {
                // Configure VPN interface
                profile.configureIntent?.let { setConfigureIntent(it) }
                setMtu(profile.tunMtu)
                setSession(profile.sessionName)
                
                // Add VPN address and route
                addAddress(VPN_ADDRESS, 24)
                addRoute(VPN_ROUTE, 0)
                
                // Add DNS servers
                profile.dnsServers.forEach { dns ->
                    try {
                        addDnsServer(dns)
                        Log.d(TAG, "Added DNS server: $dns")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to add DNS server $dns: ${e.message}")
                    }
                }
                
                // Disallow our own app from VPN
                try {
                    addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to disallow package: ${e.message}")
                }
                
                // Handle split tunneling - disallow specified apps
                profile.allowedApps.forEach { pkg ->
                    try {
                        addDisallowedApplication(pkg)
                        Log.d(TAG, "Disallowed app from VPN: $pkg")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to disallow app $pkg: ${e.message}")
                    }
                }
            }
            
            // Establish the VPN interface
            val vpnInterface: ParcelFileDescriptor?
            try {
                vpnInterface = builder.establish()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to establish VPN interface: ${e.message}", e)
                return
            }
            
            if (vpnInterface == null) {
                Log.e(TAG, "VPN interface is null")
                return
            }
            
            try {
                // Get file descriptor
                val fd = vpnInterface.detachFd()
                
                Log.d(TAG, "Connecting to server: ${profile.serverIp}")
                Log.d(TAG, "ObsKey: ${profile.obsKey.take(10)}...")
                Log.d(TAG, "ObsAlgo: ${profile.obsAlgo}")
                Log.d(TAG, "AuthId: ${profile.authId}")
                Log.d(TAG, "UDP ports: ${profile.udpPorts.contentToString()}")
                Log.d(TAG, "TCP ports: ${profile.tcpPorts.contentToString()}")
                
                // Connect using native library (use safe wrapper)
                val connected = SignalHelper.getInstance().safeConnect(
                    fd = fd,
                    serverIp = profile.serverIp,
                    udpPorts = profile.udpPorts,
                    tcpPorts = profile.tcpPorts,
                    authId = profile.authId,
                    authToken = profile.authToken,
                    obsKey = profile.obsKey,
                    isBt = profile.isBt,
                    obsAlgo = profile.obsAlgo
                )
                
                if (connected) {
                    Log.d(TAG, "Native connect() succeeded")
                } else {
                    Log.e(TAG, "Native connect() failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Native connect failed: ${e.message}", e)
            } finally {
                try {
                    vpnInterface.close()
                } catch (e: Exception) {
                    // Ignore close errors
                }
            }
        }
    }
}


package com.amobear.freevpn.data.vpn.signal

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.signallab.lib.SignalHelper as NativeSignalHelper
import com.signallab.lib.PingTarget
import com.signallab.lib.PingResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Signal VPN native library integration.
 * This wraps the native libchannel.so library methods.
 * 
 * Uses SignalHelperNative (com.signallab.lib) to match JNI naming convention
 * in libchannel.so
 */
@Singleton
class SignalHelper @Inject constructor() {
    
    companion object {
        private const val TAG = "SignalHelper"
        
        /** Whether native library was loaded successfully */
        @Volatile
        var isNativeLoaded: Boolean = false
            private set
        
        /** Singleton instance for Service access (managed by Hilt) */
        @Volatile
        private var singletonInstance: SignalHelper? = null
        
        init {
            try {
                // Load library through NativeSignalHelper which has correct package name
                // Just accessing the companion will trigger static init and load library
                NativeSignalHelper.getInstance() // This will trigger static init and load library
                isNativeLoaded = true
                Log.d(TAG, "Successfully loaded libchannel.so")
            } catch (e: UnsatisfiedLinkError) {
                isNativeLoaded = false
                Log.e(TAG, "Failed to load libchannel.so: ${e.message}")
                Log.e(TAG, "Make sure libchannel.so is placed in app/src/main/lib/{abi}/ folder")
            } catch (e: Exception) {
                isNativeLoaded = false
                Log.e(TAG, "Error loading native library: ${e.message}")
            }
        }
        
        /**
         * Check if native library is available
         */
        fun isAvailable(): Boolean = isNativeLoaded
        
        /**
         * Get instance (for Service access, instance should be set by Hilt)
         */
        fun getInstance(): SignalHelper {
            return singletonInstance ?: throw IllegalStateException("SignalHelper instance not set. Make sure it's injected via Hilt.")
        }
    }
    
    init {
        // Set instance when created by Hilt
        singletonInstance = this
    }
    
    /** Native helper instance */
    private val nativeHelper = NativeSignalHelper.getInstance()
    
    /** Current VPN profile being used */
    @Volatile
    var vpnProfile: SignalVpnProfile? = null
        set(value) {
            field = value
        }

    /**
     * Connect to VPN server using native implementation.
     * 
     * @param fd File descriptor from VpnService.Builder.establish()
     * @param serverIp VPN server IP address
     * @param udpPorts UDP ports configuration (field 'k' in VpnProfile)
     * @param tcpPorts TCP ports configuration (field 'j' in VpnProfile)
     * @param authId Authentication ID (field 'l' in VpnProfile)
     * @param authToken Authentication token (field 'm' in VpnProfile)
     * @param obsKey Obfuscation key (field 'f' in VpnProfile)
     * @param isBt Whether BitTorrent is enabled (field 'i' in VpnProfile)
     * @param obsAlgo Obfuscation algorithm (field 'a'/f2484a in VpnProfile)
     */
    fun connect(
        fd: Int,
        serverIp: String,
        udpPorts: IntArray,
        tcpPorts: IntArray,
        authId: Long,
        authToken: Long,
        obsKey: String,
        isBt: Boolean,
        obsAlgo: Int
    ) {
        nativeHelper.connect(fd, serverIp, udpPorts, tcpPorts, authId, authToken, obsKey, isBt, obsAlgo)
    }
    
    /**
     * Disconnect from current VPN connection
     */
    fun disconnect() {
        nativeHelper.disconnect()
    }
    
    /**
     * Get traffic statistics
     * @return Array of [uploadBytes, downloadBytes]
     */
    fun getStat(): LongArray {
        return nativeHelper.getStat()
    }
    
    /**
     * Safe connect that checks if native library is loaded first
     */
    fun safeConnect(
        fd: Int,
        serverIp: String,
        udpPorts: IntArray,
        tcpPorts: IntArray,
        authId: Long,
        authToken: Long,
        obsKey: String,
        isBt: Boolean,
        obsAlgo: Int
    ): Boolean {
        if (!isNativeLoaded) {
            Log.e(TAG, "Cannot connect: native library not loaded")
            return false
        }
        return try {
            connect(fd, serverIp, udpPorts, tcpPorts, authId, authToken, obsKey, isBt, obsAlgo)
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Connect failed - JNI method not found: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Safe disconnect that checks if native library is loaded first
     */
    fun safeDisconnect(): Boolean {
        if (!isNativeLoaded) {
            Log.w(TAG, "Cannot disconnect: native library not loaded")
            return false
        }
        return try {
            disconnect()
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Disconnect failed - JNI method not found: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Safe get stats that returns empty array if native library not loaded
     */
    fun safeGetStat(): LongArray {
        if (!isNativeLoaded) {
            return longArrayOf(0L, 0L)
        }
        return try {
            getStat()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "GetStat failed - JNI method not found: ${e.message}", e)
            longArrayOf(0L, 0L)
        } catch (e: Exception) {
            Log.e(TAG, "GetStat failed: ${e.message}", e)
            longArrayOf(0L, 0L)
        }
    }
    
    /**
     * Start VPN service with the given profile
     * 
     * @param context Application context
     * @param profile VPN profile containing connection parameters
     * @param serviceClass The VpnService class to start
     */
    fun startVpn(context: Context, profile: SignalVpnProfile, serviceClass: Class<out VpnService>) {
        vpnProfile = profile
        
        // Check if VPN permission is granted
        if (VpnService.prepare(context) != null) {
            Log.w(TAG, "VPN permission not granted yet")
            return
        }
        
        // Start the VPN service
        val intent = Intent(context, serviceClass)
        context.startService(intent)
        Log.d(TAG, "Started VPN service: ${serviceClass.simpleName}")
    }
    
    /**
     * Test ping to multiple servers
     * 
     * @param servers List of servers to ping
     * @param ports Ports to use for ping
     * @param timeout Ping timeout
     * @return List of ping results in milliseconds (-1 for failed)
     */
    fun testPing(servers: List<PingTarget>, ports: IntArray, timeout: Int): List<PingResult> {
        return nativeHelper.testPing(servers, ports, timeout)
    }
    
    /**
     * Clear the current VPN profile
     */
    fun clearProfile() {
        vpnProfile = null
    }
}


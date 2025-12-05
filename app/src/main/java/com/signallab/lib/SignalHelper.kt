package com.signallab.lib

import android.util.Log

/**
 * Native wrapper class matching the original package structure.
 * This class matches the JNI naming convention in libchannel.so
 * 
 * Original decompiled package: com.signallab.lib.SignalHelper
 * Class name MUST be SignalHelper to match JNI naming
 * 
 * JNI will look for: Java_com_signallab_lib_SignalHelper_<method>
 */
class SignalHelper private constructor() {
    
    companion object {
        private const val TAG = "SignalHelper"
        
        @Volatile
        private var _instance: SignalHelper? = null
        
        init {
            try {
                System.loadLibrary("channel")
                Log.d(TAG, "Successfully loaded libchannel.so")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load libchannel.so: ${e.message}")
                throw e
            }
        }
        
        fun getInstance(): SignalHelper {
            synchronized(SignalHelper::class.java) {
                if (_instance == null) {
                    _instance = SignalHelper()
                }
                return _instance!!
            }
        }
    }
    
    /**
     * Connect to VPN server using native implementation.
     * 
     * JNI signature: Java_com_signallab_lib_SignalHelper_connect
     */
    external fun connect(
        fd: Int,
        serverIp: String,
        udpPorts: IntArray,
        tcpPorts: IntArray,
        authId: Long,
        authToken: Long,
        obsKey: String,
        isBt: Boolean,
        obsAlgo: Int
    )
    
    /**
     * Disconnect from current VPN connection
     * JNI signature: Java_com_signallab_lib_SignalHelper_disconnect
     */
    external fun disconnect()
    
    /**
     * Get traffic statistics
     * JNI signature: Java_com_signallab_lib_SignalHelper_getStat
     * @return Array of [uploadBytes, downloadBytes]
     */
    external fun getStat(): LongArray
    
    /**
     * Send ping to servers for latency testing
     * JNI signature: Java_com_signallab_lib_SignalHelper_sendPing
     */
    private external fun sendPing(
        count: Int,
        ips: Array<String>,
        keys: Array<String>,
        ports: IntArray,
        results: IntArray
    )
    
    /**
     * Test ping to multiple servers
     */
    fun testPing(servers: List<PingTarget>, ports: IntArray, timeout: Int): List<PingResult> {
        if (servers.isEmpty() || ports.isEmpty()) {
            return emptyList()
        }
        
        val size = servers.size
        val ips = Array(size) { servers[it].ip }
        val keys = Array(size) { servers[it].key }
        val results = IntArray(size)
        
        try {
            sendPing(timeout, ips, keys, ports, results)
        } catch (e: Exception) {
            Log.e(TAG, "Ping failed: ${e.message}")
            return servers.map { PingResult(it.ip, -1) }
        }
        
        return servers.mapIndexed { index, target ->
            PingResult(target.ip, results[index])
        }
    }
}

/**
 * Target for ping test
 */
data class PingTarget(
    val ip: String,
    val key: String
)

/**
 * Result of ping test
 */
data class PingResult(
    val ip: String,
    val latencyMs: Int
)


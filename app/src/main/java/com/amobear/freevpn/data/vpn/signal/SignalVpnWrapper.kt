package com.amobear.freevpn.data.vpn.signal

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.amobear.freevpn.domain.model.SignalServer
import com.amobear.freevpn.domain.model.SignalServerResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signal VPN Wrapper/Controller.
 * Based on decompiled app.vpn.controllers.VpnWrapper
 * 
 * This class manages the VPN connection lifecycle, server selection,
 * and state management.
 * 
 * Managed as singleton by Hilt to avoid memory leaks.
 */
@Singleton
class SignalVpnWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signalHelper: SignalHelper
) {
    
    companion object {
        private const val TAG = "SignalVpnWrapper"
        
        /** Server selection mode - auto select best server */
        const val MODE_AUTO = 0
        
        /** Server selection mode - manual selection */
        const val MODE_MANUAL = 1
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    /** Current connection status */
    private val _connectionStatus = MutableStateFlow(ConnStatus.IDLE)
    val connectionStatus: StateFlow<ConnStatus> = _connectionStatus.asStateFlow()
    
    /** Currently selected server */
    private val _currentServer = MutableStateFlow<SignalServer?>(null)
    val currentServer: StateFlow<SignalServer?> = _currentServer.asStateFlow()
    
    /** Server list response from API */
    private var serverListResponse: SignalServerResponse? = null
    
    /** Free servers list */
    private val freeServers = mutableListOf<SignalServer>()
    
    /** VIP servers list */
    private val vipServers = mutableListOf<SignalServer>()
    
    /** Tried servers map (IP -> Server) to avoid reconnecting to failed servers */
    private val triedServers = HashMap<String, SignalServer>()
    
    /** Server selection mode */
    var selectionMode: Int = MODE_AUTO
        private set
    
    /** Is reconnecting after failure */
    private var isReconnecting = false
    
    /** Listeners for VPN events */
    private val listeners = mutableListOf<VpnListener>()
    
    /** Auth credentials */
    private var authId: Long = 0
    private var authToken: Long = 0
    
    /** Connection start time */
    private var connectionStartTime: Long = 0
    
    /**
     * VPN Connection Status
     */
    enum class ConnStatus {
        IDLE,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECT,
        FAIL,
        ERROR
    }
    
    /**
     * Interface for VPN event callbacks
     */
    interface VpnListener {
        fun onPrepare() {}
        fun onConnecting() {}
        fun onConnected(server: SignalServer?) {}
        fun onDisconnected() {}
        fun onError() {}
        fun onFailed() {}
        fun onStatusChanged(status: ConnStatus) {}
        fun onServerChanged(server: SignalServer?) {}
        fun onRefreshing(isRefreshing: Boolean) {}
    }
    
    /**
     * Update server list from API response
     */
    fun updateServerList(response: SignalServerResponse) {
        serverListResponse = response
        
        freeServers.clear()
        vipServers.clear()
        
        response.servers.forEach { server ->
            if (server.isVip) {
                vipServers.add(server)
            } else {
                freeServers.add(server)
            }
        }
        
        Log.d(TAG, "Updated server list: ${freeServers.size} free, ${vipServers.size} VIP")
    }
    
    /**
     * Set authentication credentials
     */
    fun setAuthCredentials(authId: Long, authToken: Long) {
        this.authId = authId
        this.authToken = authToken
    }
    
    /**
     * Check if VPN permission is needed
     * @return Intent to request permission, or null if already granted
     */
    fun prepareVpn(): Intent? {
        return VpnService.prepare(context)
    }
    
    /**
     * Check if VPN is currently connected
     */
    fun isConnected(): Boolean {
        return SignalVpnService.isRunning() && 
               (_connectionStatus.value == ConnStatus.CONNECTED || 
                _connectionStatus.value == ConnStatus.IDLE)
    }
    
    /**
     * Check if VPN is connecting
     */
    fun isConnecting(): Boolean {
        return SignalVpnService.isRunning() && _connectionStatus.value == ConnStatus.CONNECTING
    }
    
    /**
     * Select a server manually
     */
    fun selectServer(server: SignalServer) {
        _currentServer.value = server
        selectionMode = MODE_MANUAL
        notifyServerChanged(server)
        Log.d(TAG, "Server selected: ${server.ip} (${server.country})")
    }
    
    /**
     * Select best available server automatically
     */
    fun selectBestServer(isVip: Boolean = false): SignalServer? {
        val servers = if (isVip) vipServers else freeServers
        
        // Find first running server that hasn't been tried
        val server = servers
            .filter { it.isRunning && !triedServers.containsKey(it.ip) }
            .minByOrNull { it.load }
        
        if (server != null) {
            _currentServer.value = server
            selectionMode = MODE_AUTO
            notifyServerChanged(server)
            Log.d(TAG, "Auto-selected server: ${server.ip} (${server.country})")
        }
        
        return server
    }
    
    /**
     * Connect to VPN
     */
    fun connect() {
        try {
            // Check native library availability
            if (!SignalHelper.isAvailable()) {
                Log.e(TAG, "Native library not available")
                setStatus(ConnStatus.ERROR)
                notifyError()
                return
            }
            
            // Check VPN permission
            val prepareIntent = prepareVpn()
            if (prepareIntent != null) {
                notifyPrepare()
                return
            }
            
            // Check if already connecting
            if (isConnecting()) {
                Log.d(TAG, "Already connecting")
                notifyConnecting()
                return
            }
            
            // Check if already connected
            if (isConnected()) {
                Log.d(TAG, "Already connected")
                notifyConnecting()
                return
            }
            
            // Check server list
            if (freeServers.isEmpty() && vipServers.isEmpty()) {
                Log.e(TAG, "No servers available")
                setStatus(ConnStatus.ERROR)
                notifyError()
                return
            }
            
            // Clear tried servers
            triedServers.clear()
            
            // Get or select server
            var server = _currentServer.value
            if (server == null || !server.isRunning) {
                server = selectBestServer(false)
            }
            
            if (server == null) {
                Log.e(TAG, "No available server")
                setStatus(ConnStatus.ERROR)
                notifyError()
                return
            }
            
            // Create VPN profile
            val profile = createVpnProfile(server)
            if (profile == null) {
                Log.e(TAG, "Failed to create VPN profile")
                setStatus(ConnStatus.ERROR)
                notifyError()
                return
            }
            
            // Store profile in SignalHelper for service access
            signalHelper.vpnProfile = profile
            
            // Reset reconnecting flag
            isReconnecting = false
            
            // Notify connecting
            connectionStartTime = 0
            notifyConnecting()
            setStatus(ConnStatus.CONNECTING)
            
            // Start VPN
            startVpnConnection(profile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Connect error: ${e.message}", e)
            setStatus(ConnStatus.ERROR)
            notifyError()
        }
    }
    
    /**
     * Disconnect from VPN
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting...")
        setStatus(ConnStatus.DISCONNECTING)
        
        signalHelper.safeDisconnect()
        SignalVpnService.getInstance()?.stopVpn()
        
        connectionStartTime = 0
        setStatus(ConnStatus.DISCONNECT)
        notifyDisconnected()
    }
    
    /**
     * Toggle VPN connection
     */
    fun toggle() {
        if (isConnected() || isConnecting()) {
            disconnect()
        } else {
            connect()
        }
    }
    
    /**
     * Called when VPN connection is established
     */
    fun onConnected() {
        connectionStartTime = System.currentTimeMillis()
        setStatus(ConnStatus.CONNECTED)
        notifyConnected(_currentServer.value)
        Log.d(TAG, "VPN connected")
    }
    
    /**
     * Called when VPN connection fails
     */
    fun onConnectionFailed() {
        val server = _currentServer.value
        if (server != null) {
            triedServers[server.ip] = server
        }
        
        // Try next server if not reconnecting too many times
        if (triedServers.size < (freeServers.size + vipServers.size)) {
            Log.d(TAG, "Connection failed, trying next server...")
            isReconnecting = true
            
            val nextServer = selectBestServer(server?.isVip ?: false)
            if (nextServer != null) {
                val profile = createVpnProfile(nextServer)
                if (profile != null) {
                    startVpnConnection(profile)
                    return
                }
            }
        }
        
        // No more servers to try
        setStatus(ConnStatus.FAIL)
        notifyFailed()
        Log.e(TAG, "VPN connection failed, no more servers to try")
    }
    
    /**
     * Add VPN event listener
     */
    fun addListener(listener: VpnListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    /**
     * Remove VPN event listener
     */
    fun removeListener(listener: VpnListener) {
        listeners.remove(listener)
    }
    
    /**
     * Get current server IP
     */
    fun getCurrentServerIp(): String? = _currentServer.value?.ip
    
    /**
     * Get connection duration in milliseconds
     */
    fun getConnectionDuration(): Long {
        return if (connectionStartTime > 0) {
            System.currentTimeMillis() - connectionStartTime
        } else {
            0
        }
    }
    
    /**
     * Get traffic statistics
     * @return Pair of (uploadBytes, downloadBytes)
     */
    fun getTrafficStats(): Pair<Long, Long> {
        val stats = signalHelper.safeGetStat()
        return if (stats.size >= 2) {
            Pair(stats[0], stats[1])
        } else {
            Pair(0L, 0L)
        }
    }
    
    /**
     * Check if native library is available
     */
    fun isNativeLibraryAvailable(): Boolean = SignalHelper.isAvailable()
    
    // ==================== Private Methods ====================
    
    private fun createVpnProfile(server: SignalServer): SignalVpnProfile? {
        val response = serverListResponse ?: return null
        
        val config = SignalServerConfig(
            dnsServers = response.config.dnsServers,
            udpPorts = response.config.udpPorts,
            tcpPorts = response.config.tcpPorts,
            tunMtu = response.config.tunMtu ?: 1400
        )
        
        return SignalVpnProfile.fromServerResponse(
            serverIp = server.ip,
            obsKey = server.obsKey,
            obsAlgo = server.obsAlgo,
            isBt = server.isBt,
            config = config,
            authId = authId,
            authToken = authToken,
            sessionName = "FreeVPN - ${server.country}",
            allowedApps = emptyList() // TODO: Add split tunneling support
        )
    }
    
    private fun startVpnConnection(profile: SignalVpnProfile) {
        try {
            signalHelper.startVpn(
                context = context,
                profile = profile,
                serviceClass = SignalVpnService::class.java
            )
            
            // Start connection check
            handler.postDelayed({
                checkConnectionStatus()
            }, 3000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN: ${e.message}", e)
            onConnectionFailed()
        }
    }
    
    private fun checkConnectionStatus() {
        if (SignalVpnService.isRunning()) {
            // Check if actually connected by getting stats
            val stats = getTrafficStats()
            if (stats.first > 0 || stats.second > 0) {
                onConnected()
            } else if (_connectionStatus.value == ConnStatus.CONNECTING) {
                // Still connecting, check again
                handler.postDelayed({
                    checkConnectionStatus()
                }, 1000)
            }
        } else if (_connectionStatus.value == ConnStatus.CONNECTING) {
            onConnectionFailed()
        }
    }
    
    private fun setStatus(status: ConnStatus) {
        _connectionStatus.value = status
        handler.post {
            listeners.forEach { it.onStatusChanged(status) }
        }
    }
    
    private fun notifyPrepare() {
        handler.post { listeners.forEach { it.onPrepare() } }
    }
    
    private fun notifyConnecting() {
        handler.post { listeners.forEach { it.onConnecting() } }
    }
    
    private fun notifyConnected(server: SignalServer?) {
        handler.post { listeners.forEach { it.onConnected(server) } }
    }
    
    private fun notifyDisconnected() {
        handler.post { listeners.forEach { it.onDisconnected() } }
    }
    
    private fun notifyError() {
        handler.post { listeners.forEach { it.onError() } }
    }
    
    private fun notifyFailed() {
        handler.post { listeners.forEach { it.onFailed() } }
    }
    
    private fun notifyServerChanged(server: SignalServer?) {
        handler.post { listeners.forEach { it.onServerChanged(server) } }
    }
}


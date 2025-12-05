package com.amobear.freevpn.domain.usecase

import android.content.Intent
import com.amobear.freevpn.data.vpn.signal.SignalVpnWrapper
import com.amobear.freevpn.domain.model.SignalServer
import com.amobear.freevpn.domain.model.SignalServerResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for connecting to VPN using Signal protocol.
 * This provides a clean interface for the presentation layer to interact
 * with the Signal VPN system.
 */
@Singleton
class ConnectSignalVpnUseCase @Inject constructor(
    private val vpnWrapper: SignalVpnWrapper
) {
    
    /**
     * Connection state for UI binding
     */
    sealed class ConnectionState {
        data object Idle : ConnectionState()
        data object Connecting : ConnectionState()
        data class Connected(val server: SignalServer?) : ConnectionState()
        data object Disconnected : ConnectionState()
        data class Error(val message: String?) : ConnectionState()
        data object Failed : ConnectionState()
        data object RequiresPermission : ConnectionState()
    }
    
    /**
     * Observe connection status as Flow
     */
    fun observeConnectionState(): Flow<ConnectionState> {
        return vpnWrapper.connectionStatus.map { status ->
            when (status) {
                SignalVpnWrapper.ConnStatus.IDLE -> ConnectionState.Idle
                SignalVpnWrapper.ConnStatus.CONNECTING -> ConnectionState.Connecting
                SignalVpnWrapper.ConnStatus.CONNECTED -> ConnectionState.Connected(vpnWrapper.currentServer.value)
                SignalVpnWrapper.ConnStatus.DISCONNECTING,
                SignalVpnWrapper.ConnStatus.DISCONNECT -> ConnectionState.Disconnected
                SignalVpnWrapper.ConnStatus.FAIL -> ConnectionState.Failed
                SignalVpnWrapper.ConnStatus.ERROR -> ConnectionState.Error(null)
            }
        }
    }
    
    /**
     * Observe current server as Flow
     */
    fun observeCurrentServer(): Flow<SignalServer?> {
        return vpnWrapper.currentServer
    }
    
    /**
     * Initialize with server list response
     */
    fun initialize(response: SignalServerResponse, authId: Long, authToken: Long) {
        vpnWrapper.updateServerList(response)
        vpnWrapper.setAuthCredentials(authId, authToken)
    }
    
    /**
     * Check if VPN permission is needed
     * @return Intent to request permission, or null if already granted
     */
    fun checkVpnPermission(): Intent? {
        return vpnWrapper.prepareVpn()
    }
    
    /**
     * Connect to VPN with auto-selected server
     */
    fun connect() {
        vpnWrapper.connect()
    }
    
    /**
     * Connect to specific server
     */
    fun connect(server: SignalServer) {
        vpnWrapper.selectServer(server)
        vpnWrapper.connect()
    }
    
    /**
     * Disconnect from VPN
     */
    fun disconnect() {
        vpnWrapper.disconnect()
    }
    
    /**
     * Toggle VPN connection
     */
    fun toggle() {
        vpnWrapper.toggle()
    }
    
    /**
     * Check if VPN is connected
     */
    fun isConnected(): Boolean {
        return vpnWrapper.isConnected()
    }
    
    /**
     * Check if VPN is connecting
     */
    fun isConnecting(): Boolean {
        return vpnWrapper.isConnecting()
    }
    
    /**
     * Get current server
     */
    fun getCurrentServer(): SignalServer? {
        return vpnWrapper.currentServer.value
    }
    
    /**
     * Select a server (without connecting)
     */
    fun selectServer(server: SignalServer) {
        vpnWrapper.selectServer(server)
    }
    
    /**
     * Get connection duration in milliseconds
     */
    fun getConnectionDuration(): Long {
        return vpnWrapper.getConnectionDuration()
    }
    
    /**
     * Get traffic statistics
     * @return Pair of (uploadBytes, downloadBytes)
     */
    fun getTrafficStats(): Pair<Long, Long> {
        return vpnWrapper.getTrafficStats()
    }
    
    /**
     * Add listener for VPN events
     */
    fun addListener(listener: SignalVpnWrapper.VpnListener) {
        vpnWrapper.addListener(listener)
    }
    
    /**
     * Remove listener
     */
    fun removeListener(listener: SignalVpnWrapper.VpnListener) {
        vpnWrapper.removeListener(listener)
    }
}


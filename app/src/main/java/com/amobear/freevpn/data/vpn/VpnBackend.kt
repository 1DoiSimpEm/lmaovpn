package com.amobear.freevpn.data.vpn

import android.util.Log
import com.amobear.freevpn.data.vpn.models.ConnectionParams
import com.amobear.freevpn.data.vpn.usecases.SettingsForConnection
import com.amobear.freevpn.models.config.VpnProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

abstract class VpnBackend(
    protected val settingsForConnection: SettingsForConnection,
    val vpnProtocol: VpnProtocol,
    protected val mainScope: CoroutineScope,
) : VpnStateSource {
    protected var lastConnectionParams: ConnectionParams? = null
    
    val internalVpnProtocolState = MutableStateFlow<VpnState>(VpnState.Disabled)
    override val selfStateFlow: MutableStateFlow<VpnState> = internalVpnProtocolState
    
    protected var vpnProtocolState: VpnState
        get() = internalVpnProtocolState.value
        set(value) {
            val hasChanged = internalVpnProtocolState.value != value
            internalVpnProtocolState.value = value
            if (hasChanged) {
                Log.d("VpnBackend", "State changed: $value")
            }
        }

    abstract suspend fun connect(connectionParams: ConnectionParams)
    
    abstract suspend fun closeVpnTunnel(withStateChange: Boolean = true)
    
    suspend fun disconnect() {
        if (vpnProtocolState != VpnState.Disabled)
            vpnProtocolState = VpnState.Disconnecting
        
        closeVpnTunnel()
    }
    
    protected suspend fun waitForDisconnect() {
        withTimeoutOrNull(DISCONNECT_WAIT_TIMEOUT) {
            do {
                delay(200)
            } while (selfStateFlow.value != VpnState.Disabled)
        }
        if (selfStateFlow.value == VpnState.Disconnecting)
            internalVpnProtocolState.value = VpnState.Disabled
    }
    
    override fun setSelfState(value: VpnState) {
        internalVpnProtocolState.value = value
    }
    
    /**
     * Set connection params for tracking purposes when service is started externally
     * (e.g., from Repository using profile.getStartServiceIntent())
     */
    fun setConnectionParamsForTracking(connectionParams: ConnectionParams?) {
        lastConnectionParams = connectionParams
        Log.d("VpnBackend", "Set connection params for tracking: ${connectionParams?.info}")
    }
    
    companion object {
        private const val DISCONNECT_WAIT_TIMEOUT = 3000L
    }
}


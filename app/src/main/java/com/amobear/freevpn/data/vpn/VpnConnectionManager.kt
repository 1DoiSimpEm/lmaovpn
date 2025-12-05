package com.amobear.freevpn.data.vpn

import android.util.Log
import com.amobear.freevpn.data.vpn.models.ConnectionParams
import com.amobear.freevpn.data.vpn.models.SimpleConnectIntent
import com.amobear.freevpn.utils.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnConnectionManager @Inject constructor(
    private val scope: CoroutineScope,
) {
    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Disabled)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()
    
    private var activeBackend: VpnBackend? = null
    
    fun onRestoreProcess(connectIntent: SimpleConnectIntent, reason: String): Boolean {
        val stateKey = Storage.getString("VPN_STATE", null)
        val shouldReconnect = stateKey != VpnState.Disabled.toString() &&
            stateKey != VpnState.Disconnecting.toString()
        
        if (shouldReconnect) {
            Log.d("VpnConnectionManager", "Restoring connection: $reason")
            // For now, just return true to indicate we should try to restore
            // Actual restoration logic can be added later
            return true
        }
        return false
    }
    
    fun onVpnServiceDestroyed(connectionParamsUuid: UUID?) {
        ConnectionParams.readIntentFromStore(expectedUuid = connectionParamsUuid)?.let {
            Log.d("VpnConnectionManager", "onDestroy called for current VpnService")
            ConnectionParams.deleteFromStore("service destroyed")
        }
    }
}


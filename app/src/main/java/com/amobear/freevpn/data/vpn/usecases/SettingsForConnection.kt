package com.amobear.freevpn.data.vpn.usecases

import com.amobear.freevpn.data.vpn.models.SimpleConnectIntent
import com.amobear.freevpn.settings.data.SplitTunnelingSettings

class SettingsForConnection {
    data class ConnectionSettings(
        val mtuSize: Int = 1375,
        val splitTunneling: SplitTunnelingSettings = SplitTunnelingSettings(),
        val lanConnections: Boolean = false,
        val lanConnectionsAllowDirect: Boolean = false,
        val customDns: CustomDnsSettings = CustomDnsSettings()
    ) {
        data class CustomDnsSettings(
            val toggleEnabled: Boolean = false,
            val rawDnsList: List<String> = emptyList()
        ) {
            val effectiveDnsList: List<String> get() = if (toggleEnabled) rawDnsList else emptyList()
            val effectiveEnabled get() = effectiveDnsList.isNotEmpty()
        }
    }
    
    companion object {
        fun getForSync(connectIntent: SimpleConnectIntent): ConnectionSettings {
            return ConnectionSettings()
        }
        
        fun getFor(connectIntent: SimpleConnectIntent): ConnectionSettings {
            return ConnectionSettings()
        }
    }
}


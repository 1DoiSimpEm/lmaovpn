package com.amobear.freevpn.data.vpn.usecases

class ComputeAllowedIPs {
    operator fun invoke(settings: SettingsForConnection.ConnectionSettings): List<String> {
        // Return default: all IPs allowed
        return listOf("0.0.0.0/0", "::/0")
    }
}


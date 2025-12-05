package com.amobear.freevpn.data.vpn.signal

import android.app.PendingIntent

/**
 * VPN Profile for Signal protocol connection.
 * Based on decompiled VpnProfile from app.vpn.model
 * 
 * Field mapping from decompiled code:
 * - a (f2484a) -> obsAlgo (int)
 * - b -> allowedApps (List<String>)
 * - c -> configureIntent (PendingIntent)
 * - d -> dnsServers (List<String>)
 * - e -> serverIp (String)
 * - f -> obsKey (String)
 * - g -> tunMtu (int)
 * - h -> sessionName (String)
 * - i -> isBt (boolean)
 * - j -> tcpPorts (int[])
 * - k -> udpPorts (int[])
 * - l -> authId (long)
 * - m -> authToken (long)
 */
data class SignalVpnProfile(
    /** Field 'a' (f2484a): Obfuscation algorithm type (0 = none, others TBD) */
    val obsAlgo: Int = 0,
    
    /** Field 'b': List of allowed apps to bypass VPN (split tunneling) */
    val allowedApps: MutableList<String> = mutableListOf(),
    
    /** Field 'c': Pending intent for VPN configuration UI */
    val configureIntent: PendingIntent? = null,
    
    /** Field 'd': DNS servers to use for the VPN connection */
    val dnsServers: MutableList<String> = mutableListOf(),
    
    /** Field 'e': VPN server IP address */
    val serverIp: String = "",
    
    /** Field 'f': Obfuscation key for the connection */
    val obsKey: String = "",
    
    /** Field 'g': TUN interface MTU size */
    val tunMtu: Int = 1400,
    
    /** Field 'h': Session/profile name */
    val sessionName: String = "FreeVPN",
    
    /** Field 'i': Whether BitTorrent is allowed */
    val isBt: Boolean = false,
    
    /** Field 'j': TCP ports configuration */
    val tcpPorts: IntArray = intArrayOf(),
    
    /** Field 'k': UDP ports configuration */  
    val udpPorts: IntArray = intArrayOf(),
    
    /** Field 'l': Auth ID from registration */
    val authId: Long = 0,
    
    /** Field 'm': Auth token from registration */
    val authToken: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as SignalVpnProfile
        
        if (obsAlgo != other.obsAlgo) return false
        if (allowedApps != other.allowedApps) return false
        if (configureIntent != other.configureIntent) return false
        if (dnsServers != other.dnsServers) return false
        if (serverIp != other.serverIp) return false
        if (obsKey != other.obsKey) return false
        if (tunMtu != other.tunMtu) return false
        if (sessionName != other.sessionName) return false
        if (isBt != other.isBt) return false
        if (!tcpPorts.contentEquals(other.tcpPorts)) return false
        if (!udpPorts.contentEquals(other.udpPorts)) return false
        if (authId != other.authId) return false
        if (authToken != other.authToken) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = obsAlgo
        result = 31 * result + allowedApps.hashCode()
        result = 31 * result + (configureIntent?.hashCode() ?: 0)
        result = 31 * result + dnsServers.hashCode()
        result = 31 * result + serverIp.hashCode()
        result = 31 * result + obsKey.hashCode()
        result = 31 * result + tunMtu
        result = 31 * result + sessionName.hashCode()
        result = 31 * result + isBt.hashCode()
        result = 31 * result + tcpPorts.contentHashCode()
        result = 31 * result + udpPorts.contentHashCode()
        result = 31 * result + authId.hashCode()
        result = 31 * result + authToken.hashCode()
        return result
    }
    
    companion object {
        /**
         * Create a SignalVpnProfile from server response data
         */
        fun fromServerResponse(
            serverIp: String,
            obsKey: String,
            obsAlgo: Int,
            isBt: Boolean,
            config: SignalServerConfig,
            authId: Long,
            authToken: Long,
            sessionName: String = "FreeVPN",
            allowedApps: List<String> = emptyList()
        ): SignalVpnProfile {
            return SignalVpnProfile(
                obsAlgo = obsAlgo,
                allowedApps = allowedApps.toMutableList(),
                dnsServers = config.dnsServers.toMutableList(),
                serverIp = serverIp,
                obsKey = obsKey,
                tunMtu = config.tunMtu,
                sessionName = sessionName,
                isBt = isBt,
                tcpPorts = config.tcpPorts.toIntArray(),
                udpPorts = config.udpPorts.toIntArray(),
                authId = authId,
                authToken = authToken
            )
        }
    }
}

/**
 * Server configuration from API response
 */
data class SignalServerConfig(
    val dnsServers: List<String> = emptyList(),
    val udpPorts: List<Int> = emptyList(),
    val tcpPorts: List<Int> = emptyList(),
    val tunMtu: Int = 1400
)


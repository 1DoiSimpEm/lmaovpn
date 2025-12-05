package com.amobear.freevpn.data.network.api

import com.google.gson.annotations.SerializedName

/**
 * VPN Gate API Response Model
 * VPN Gate provides free VPN servers operated by volunteers
 * API endpoint: https://www.vpngate.net/api/iphone/
 */
data class VpnGateResponse(
    @SerializedName("ServerList")
    val serverList: List<VpnGateServer>? = null
)

/**
 * VPN Gate Server Model
 * Each server entry contains connection information
 */
data class VpnGateServer(
    @SerializedName("HostName")
    val hostName: String? = null,
    
    @SerializedName("IP")
    val ip: String? = null,
    
    @SerializedName("Score")
    val score: Long = 0L,
    
    @SerializedName("Ping")
    val ping: Long = 0L,
    
    @SerializedName("Speed")
    val speed: Long = 0L, // Speed in Mbps
    
    @SerializedName("CountryLong")
    val countryLong: String? = null, // Full country name
    
    @SerializedName("CountryShort")
    val countryShort: String? = null, // Country code (2 letters)
    
    @SerializedName("NumVpnSessions")
    val numVpnSessions: Long = 0L,
    
    @SerializedName("Uptime")
    val uptime: Long = 0L,
    
    @SerializedName("TotalUsers")
    val totalUsers: Long = 0L,
    
    @SerializedName("TotalTraffic")
    val totalTraffic: Long = 0L,
    
    @SerializedName("LogType")
    val logType: Long = 0L,
    
    @SerializedName("Operator")
    val operator: String? = null,
    
    @SerializedName("Message")
    val message: String? = null,
    
    @SerializedName("OpenVPN_Data")
    val openVpnData: String? = null, // Base64 encoded OpenVPN config
    
    @SerializedName("Config_Data")
    val configData: String? = null // OpenVPN config in text format
)


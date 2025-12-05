package com.amobear.freevpn.data.network.api

import com.google.gson.annotations.SerializedName

/**
 * Response model for VPN server list API
 */
data class VpnServerListResponse(
    @SerializedName("LogicalServers")
    val logicalServers: List<LogicalServer>? = null
)

/**
 * Logical server model from ProtonVPN API
 */
data class LogicalServer(
    @SerializedName("ID")
    val id: String,
    
    @SerializedName("Name")
    val name: String,
    
    @SerializedName("EntryCountry")
    val entryCountry: String,
    
    @SerializedName("ExitCountry")
    val exitCountry: String,
    
    @SerializedName("HostCountry")
    val hostCountry: String? = null,
    
    @SerializedName("Tier")
    val tier: Int, // 0 = free, 1 = basic, 2 = plus
    
    @SerializedName("Load")
    val load: Float = 0f,
    
    @SerializedName("Status")
    val status: Int = 1, // 1 = online, 0 = offline
    
    @SerializedName("Servers")
    val servers: List<PhysicalServer>? = null,
    
    @SerializedName("City")
    val city: String? = null,
    
    @SerializedName("State")
    val state: String? = null,
    
    @SerializedName("Features")
    val features: Int = 0,
    
    @SerializedName("Location")
    val location: ServerLocation? = null
)

/**
 * Physical server (connecting domain) model
 */
data class PhysicalServer(
    @SerializedName("ID")
    val id: String,
    
    @SerializedName("EntryIP")
    val entryIP: String? = null,
    
    @SerializedName("Domain")
    val domain: String? = null,
    
    @SerializedName("Status")
    val status: Int = 1,
    
    @SerializedName("Label")
    val label: String? = null
)

/**
 * Server location model
 */
data class ServerLocation(
    @SerializedName("Latitude")
    val latitude: Double = 0.0,
    
    @SerializedName("Longitude")
    val longitude: Double = 0.0
)

/**
 * Response model for server loads API
 */
data class VpnLoadsResponse(
    @SerializedName("LogicalServers")
    val logicalServers: List<ServerLoad>? = null
)

/**
 * Server load model
 */
data class ServerLoad(
    @SerializedName("ID")
    val id: String,
    
    @SerializedName("Load")
    val load: Float = 0f
)


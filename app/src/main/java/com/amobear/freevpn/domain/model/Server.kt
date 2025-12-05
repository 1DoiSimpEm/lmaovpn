package com.amobear.freevpn.domain.model

data class Server(
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val username: String? = null,
    val password: String? = null,
    val ovpnConfig: String? = null,
    val isPremium: Boolean = false,
    val latency: Long = 0L, // in milliseconds
    val speed: Double = 0.0, // in Mbps
    val isFavorite: Boolean = false,
    val isIPv6Supported: Boolean = false
)


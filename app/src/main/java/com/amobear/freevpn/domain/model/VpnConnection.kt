package com.amobear.freevpn.domain.model

data class VpnConnection(
    val serverId: String,
    val serverName: String,
    val state: ConnectionState,
    val startTime: Long = 0L,
    val duration: Long = 0L,
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val uploadSpeed: Double = 0.0, // bytes per second
    val downloadSpeed: Double = 0.0, // bytes per second
    val ipAddress: String? = null
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    AUTH_FAILED,
    NO_NETWORK
}


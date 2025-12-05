package com.amobear.freevpn.domain.model

data class SignalServerConfig(
    val dnsServers: List<String> = emptyList(),
    val udpPorts: List<Int> = emptyList(),
    val tcpPorts: List<Int> = emptyList(),
    val tunMtu: Int? = null
)

data class SignalServer(
    val load: Int = 0,
    val isBt: Boolean = false,
    val obsKey: String = "",
    val isVip: Boolean = false,
    val country: String = "",
    val obsAlgo: Int = 0,
    val ip: String = "",
    val area: String = "",
    val isRunning: Boolean = false
)

data class SignalServerResponse(
    val config: SignalServerConfig = SignalServerConfig(),
    val list: String? = null,
    val servers: List<SignalServer> = emptyList()
)


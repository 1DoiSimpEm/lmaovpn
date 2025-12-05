package com.amobear.freevpn.data.network.api

import com.google.gson.annotations.SerializedName

data class SignalServerResponse(
    @SerializedName("config") val config: SignalServerConfig? = null,
    @SerializedName("list") val list: String? = null,
    @SerializedName("server") val servers: List<SignalServerItem>? = null
)

data class SignalServerConfig(
    @SerializedName("dns_server") val dnsServers: List<String>? = null,
    @SerializedName("udp") val udpPorts: List<Int>? = null,
    @SerializedName("tcp") val tcpPorts: List<Int>? = null,
    @SerializedName("tun_mtu") val tunMtu: Int? = null
)

data class SignalServerItem(
    @SerializedName("load") val load: Int = 0,
    @SerializedName("is_bt") val isBt: Boolean = false,
    @SerializedName("obs_key") val obsKey: String? = null,
    @SerializedName("is_vip") val isVip: Boolean = false,
    @SerializedName("country") val country: String? = null,
    @SerializedName("obs_algo") val obsAlgo: Int = 0,
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("area") val area: String? = null,
    @SerializedName("is_running") val isRunning: Boolean = false
)


package com.amobear.freevpn.data.network.api.dto

import com.google.gson.annotations.SerializedName

data class SignalResponseDto(
    @SerializedName("config") val config: SignalConfigDto? = null,
    @SerializedName("list") val list: String? = null,
    @SerializedName("server") val servers: List<SignalServerDto>? = null
)

data class SignalConfigDto(
    @SerializedName("dns_server") val dnsServer: List<String>? = null,
    @SerializedName("udp") val udp: List<Int>? = null,
    @SerializedName("tcp") val tcp: List<Int>? = null,
    @SerializedName("tun_mtu") val tunMtu: Int? = null
)

data class SignalServerDto(
    @SerializedName("load") val load: Int? = null,
    @SerializedName("is_bt") val isBt: Boolean? = null,
    @SerializedName("obs_key") val obsKey: String? = null,
    @SerializedName("is_vip") val isVip: Boolean? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("obs_algo") val obsAlgo: Int? = null,
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("area") val area: String? = null,
    @SerializedName("is_running") val isRunning: Boolean? = null
)


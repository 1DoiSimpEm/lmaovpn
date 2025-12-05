package com.amobear.freevpn.domain.model

data class SpeedTestResult(
    val serverId: String,
    val downloadSpeed: Double, // Mbps
    val uploadSpeed: Double, // Mbps
    val latency: Long, // milliseconds
    val timestamp: Long = System.currentTimeMillis()
)


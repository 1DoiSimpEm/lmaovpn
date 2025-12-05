package com.amobear.freevpn.domain.model

data class TrafficStats(
    val uploadBytes: Long,
    val downloadBytes: Long,
    val uploadSpeed: Double, // bytes per second
    val downloadSpeed: Double, // bytes per second
    val totalBytes: Long = uploadBytes + downloadBytes
) {
    fun uploadMB(): Double = uploadBytes / (1024.0 * 1024.0)
    fun downloadMB(): Double = downloadBytes / (1024.0 * 1024.0)
    fun totalMB(): Double = totalBytes / (1024.0 * 1024.0)

    fun uploadSpeedMbps(): Double = (uploadSpeed * 8) / (1024.0 * 1024.0)
    fun downloadSpeedMbps(): Double = (downloadSpeed * 8) / (1024.0 * 1024.0)
}


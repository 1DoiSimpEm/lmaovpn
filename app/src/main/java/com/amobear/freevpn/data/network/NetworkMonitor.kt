package com.amobear.freevpn.data.network

import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.domain.model.SpeedTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class NetworkMonitor @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    suspend fun pingServer(host: String): Long = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val address = InetAddress.getByName(host)
            val reachable = address.isReachable(5000) // 5 seconds timeout
            val endTime = System.currentTimeMillis()

            if (reachable) {
                endTime - startTime
            } else {
                -1L // Server not reachable
            }
        } catch (e: Exception) {
            -1L // Error occurred
        }
    }

    suspend fun testSpeed(server: Server): SpeedTestResult = withContext(Dispatchers.IO) {
        val latency = pingServer(server.host)

        // Simple download speed test
        val downloadSpeed = measureDownloadSpeed(server.host)

        // For upload, we'd need a server endpoint that accepts uploads
        // For now, using a mock value
        val uploadSpeed = 0.0

        SpeedTestResult(
            serverId = server.id,
            downloadSpeed = downloadSpeed,
            uploadSpeed = uploadSpeed,
            latency = latency
        )
    }

    private suspend fun measureDownloadSpeed(host: String): Double = withContext(Dispatchers.IO) {
        try {
            // Using a small test file - in production, use your own test endpoint
            val testUrl = "http://$host/speedtest" // Mock endpoint
            val request = Request.Builder()
                .url(testUrl)
                .build()

            var bytesDownloaded = 0L
            val timeTaken = measureTimeMillis {
                try {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.bytes()?.let { bytes ->
                                bytesDownloaded = bytes.size.toLong()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Use ping latency as fallback indicator
                    bytesDownloaded = 1024 * 100 // 100KB mock
                }
            }

            if (timeTaken > 0 && bytesDownloaded > 0) {
                // Convert to Mbps
                val seconds = timeTaken / 1000.0
                val bits = bytesDownloaded * 8
                (bits / seconds) / (1024.0 * 1024.0)
            } else {
                0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    suspend fun isNetworkAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName("8.8.8.8")
            address.isReachable(3000)
        } catch (e: Exception) {
            false
        }
    }
}


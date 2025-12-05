package com.amobear.freevpn.data.local

import com.amobear.freevpn.data.local.dao.ServerDao
import com.amobear.freevpn.data.local.entity.ServerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SampleDataInitializer @Inject constructor(
    private val serverDao: ServerDao
) {
    suspend fun initializeSampleServers() = withContext(Dispatchers.IO) {
        // Check if servers already exist (from API sync)
        val existingCount = serverDao.getServerCount()
        if (existingCount > 0) {
            // Servers already exist, skip sample data initialization
            return@withContext
        }
        val sampleServers = listOf(
            // Local Mac VPN Server (for testing)
            // IP detected: 192.168.100.241
            // Make sure to run setup-vpn-server.sh first and start the server
            ServerEntity(
                id = "local-mac-server",
                name = "Local Mac Server",
                countryCode = "VN",
                countryName = "Local",
                host = "192.168.100.241", // Your Mac's IP - update if it changes
                port = 1194,
                protocol = "udp",
                username = null, // Certificate-based auth (no username/password needed)
                password = null,
                isPremium = false,
                latency = 0
            ),
            // Quick test server: Public VPN 219 (Japan)
            ServerEntity(
                id = "quick-vpn-219",
                name = "Public VPN 219 (JP)",
                countryCode = "JP",
                countryName = "Japan",
                host = "public-vpn-219.opengw.net",
                port = 1194,
                protocol = "udp",
                // Many public VPN Gate-style servers accept simple "vpn"/"vpn" credentials.
                // These can be changed later via your own backend or UI if needed.
                username = "vpn",
                password = "vpn",
                isPremium = false,
                latency = 0
            ),
            ServerEntity(
                id = "us-1",
                name = "US Server 1",
                countryCode = "US",
                countryName = "United States",
                host = "us1.example.com",
                port = 1194,
                protocol = "udp",
                username = "vpnuser",
                password = "vpnpass",
                isPremium = false,
                latency = 45
            ),
            ServerEntity(
                id = "uk-1",
                name = "UK Server 1",
                countryCode = "GB",
                countryName = "United Kingdom",
                host = "uk1.example.com",
                port = 1194,
                protocol = "udp",
                username = "vpnuser",
                password = "vpnpass",
                isPremium = false,
                latency = 72
            ),
            ServerEntity(
                id = "de-1",
                name = "Germany Server 1",
                countryCode = "DE",
                countryName = "Germany",
                host = "de1.example.com",
                port = 1194,
                protocol = "udp",
                username = "vpnuser",
                password = "vpnpass",
                isPremium = false,
                latency = 38
            ),
            ServerEntity(
                id = "jp-1",
                name = "Japan Server 1",
                countryCode = "JP",
                countryName = "Japan",
                host = "jp1.example.com",
                port = 1194,
                protocol = "udp",
                username = "vpnuser",
                password = "vpnpass",
                isPremium = false,
                latency = 120
            ),
            ServerEntity(
                id = "sg-1",
                name = "Singapore Server 1",
                countryCode = "SG",
                countryName = "Singapore",
                host = "sg1.example.com",
                port = 1194,
                protocol = "udp",
                username = "vpnuser",
                password = "vpnpass",
                isPremium = false,
                latency = 98
            ),
            ServerEntity(
                id = "ca-1",
                name = "Canada Server 1",
                countryCode = "CA",
                countryName = "Canada",
                host = "ca1.example.com",
                port = 1194,
                protocol = "udp",
                username = "vpnuser",
                password = "vpnpass",
                isPremium = false,
                latency = 55
            )
        )

        serverDao.insertServers(sampleServers)
    }
}


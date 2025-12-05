package com.amobear.freevpn.data.network

import android.util.Log
import android.util.Base64
import com.amobear.freevpn.data.network.api.VpnApiService
import com.amobear.freevpn.domain.model.Server
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for fetching VPN servers from VPN Gate API
 * VPN Gate provides free VPN servers operated by volunteers
 * No authentication required
 */
@Singleton
class VpnApiClient @Inject constructor(
    private val vpnApiService: VpnApiService
) {
    companion object {
        private const val TAG = "VpnApiClient"
        // VPN Gate API base URL (public, no authentication required)
        const val VPN_GATE_API_BASE_URL = "https://www.vpngate.net/"
    }

    /**
     * Fetch free VPN servers from VPN Gate API
     * VPN Gate API returns CSV format, not JSON
     * @return List of free VPN servers, or empty list if error
     */
    suspend fun fetchFreeServers(): List<Server> {
        return try {
            Log.d(TAG, "Fetching free VPN servers from VPN Gate API...")
            
            val response = vpnApiService.getServers()
            
            if (response.isSuccessful && response.body() != null) {
                val csvData = response.body()!!.string()
                Log.d(TAG, "Received CSV data, length: ${csvData.length}")
                
                // Parse CSV data
                val servers = parseCsvResponse(csvData)
                Log.d(TAG, "Parsed ${servers.size} servers from CSV")
                
                servers
            } else {
                Log.e(TAG, "API call failed: ${response.code()} - ${response.message()}")
                try {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error response body: $errorBody")
                } catch (e: Exception) {
                    // Ignore
                }
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching servers from VPN Gate API", e)
            emptyList()
        }
    }
    
    /**
     * Parse CSV response from VPN Gate API
     * Format: *vpn_servers\n#HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,...
     */
    private fun parseCsvResponse(csvData: String): List<Server> {
        val lines = csvData.lines()
        if (lines.isEmpty()) {
            Log.w(TAG, "Empty CSV response")
            return emptyList()
        }
        
        // Find header line (starts with #)
        var headerIndex = -1
        val headerMap = mutableMapOf<String, Int>()
        
        for (i in lines.indices) {
            if (lines[i].startsWith("#")) {
                headerIndex = i
                val headers = lines[i].removePrefix("#").split(",")
                headers.forEachIndexed { index, header ->
                    headerMap[header.trim()] = index
                }
                // Log all available headers for debugging
                Log.d(TAG, "CSV Headers found: ${headers.joinToString(", ")}")
                break
            }
        }
        
        if (headerIndex == -1 || headerMap.isEmpty()) {
            Log.w(TAG, "Could not find CSV header")
            return emptyList()
        }
        
        // Parse data rows (skip header and any lines before it)
        val servers = mutableListOf<Server>()
        
        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("*") || line.startsWith("#")) {
                continue
            }
            
            val values = parseCsvLine(line)
            if (values.isEmpty()) continue
            
            try {
                val server = parseCsvRowToServer(values, headerMap)
                if (server != null) {
                    servers.add(server)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse CSV row: $line", e)
            }
        }
        
        return servers
    }
    
    /**
     * Parse a CSV line, handling quoted fields
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        result.add(current.toString().trim())
        return result
    }
    
    /**
     * Parse a CSV row to Server domain model
     */
    private fun parseCsvRowToServer(
        values: List<String>,
        headerMap: Map<String, Int>
    ): Server? {
        fun getValue(columnName: String): String? {
            val index = headerMap[columnName] ?: return null
            return if (index < values.size) values[index] else null
        }
        
        val hostName = getValue("HostName") ?: return null
        val ip = getValue("IP") ?: return null
        val countryLong = getValue("CountryLong") ?: "Unknown"
        val countryShort = getValue("CountryShort") ?: "UN"
        val score = getValue("Score")?.toLongOrNull() ?: 0L
        val ping = getValue("Ping")?.toLongOrNull() ?: 0L
        val speed = getValue("Speed")?.toLongOrNull() ?: 0L
        val operator = getValue("Operator") ?: ""
        
        // Try multiple possible field names for OpenVPN config
        val openVpnConfigBase64 = getValue("OpenVPN_ConfigData_Base64") 
            ?: getValue("OpenVPN_Data")
            ?: getValue("Config_Data")
            ?: ""
        
        // Log if config found
        if (openVpnConfigBase64.isNotEmpty()) {
            Log.d(TAG, "Found OpenVPN config data for $ip, length: ${openVpnConfigBase64.length}")
        } else {
            Log.w(TAG, "No OpenVPN config data found for $ip. Available fields: ${headerMap.keys.joinToString(", ")}")
        }
        
        // Decode OpenVPN config
        val ovpnConfig = if (openVpnConfigBase64.isNotEmpty()) {
            try {
                val decoded = String(Base64.decode(openVpnConfigBase64, Base64.DEFAULT))
                Log.d(TAG, "Successfully decoded OpenVPN config for $ip, config length: ${decoded.length}")
                decoded
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode OpenVPN config for $ip", e)
                // Try as plain text if base64 decode fails
                if (openVpnConfigBase64.contains("remote") || openVpnConfigBase64.contains("proto")) {
                    Log.d(TAG, "Config appears to be plain text, using directly")
                    openVpnConfigBase64
                } else {
                    null
                }
            }
        } else {
            null
        }
        
        // Extract port and protocol from OpenVPN config
        var port = 1194 // Default OpenVPN UDP port
        var protocol = "UDP" // Default protocol
        
        ovpnConfig?.let { config ->
            // Try to extract port from "remote" line
            val remoteMatch = Regex("remote\\s+[^\\s]+\\s+(\\d+)", RegexOption.IGNORE_CASE).find(config)
            remoteMatch?.groupValues?.get(1)?.toIntOrNull()?.let { port = it }
            
            // Try to extract protocol
            val protoMatch = Regex("proto\\s+(\\w+)", RegexOption.IGNORE_CASE).find(config)
            protoMatch?.groupValues?.get(1)?.uppercase()?.let { protocol = it }
        }
        
        // Create server ID from IP
        val serverId = "vpngate_${ip.replace(".", "_")}"
        
        // Create server name
        val serverName = buildString {
            append(hostName)
            if (operator.isNotBlank()) {
                append(" ($operator)")
            }
        }
        
        return Server(
            id = serverId,
            name = serverName,
            countryCode = countryShort.uppercase(),
            countryName = countryLong,
            host = hostName,
            port = port,
            protocol = protocol,
            username = null, // VPN Gate uses OpenVPN config file
            password = null,
            ovpnConfig = ovpnConfig,
            isPremium = false, // All VPN Gate servers are free
            latency = ping,
            speed = speed.toDouble(), // Speed in Mbps
            isFavorite = false
        )
    }

}


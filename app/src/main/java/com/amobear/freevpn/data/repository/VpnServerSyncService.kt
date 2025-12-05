package com.amobear.freevpn.data.repository

import android.util.Log
import com.amobear.freevpn.data.local.dao.ServerDao
import com.amobear.freevpn.data.local.entity.ServerEntity
import com.amobear.freevpn.data.network.VpnApiClient
import com.amobear.freevpn.domain.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to sync VPN servers from API to local database
 */
@Singleton
class VpnServerSyncService @Inject constructor(
    private val vpnApiClient: VpnApiClient,
    private val serverDao: ServerDao
) {
    companion object {
        private const val TAG = "VpnServerSyncService"
    }

    /**
     * Sync servers from API to local database
     * @param forceRefresh Force refresh even if servers exist locally
     * @return Number of servers synced
     */
    suspend fun syncServersFromApi(forceRefresh: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Starting server sync from API (forceRefresh=$forceRefresh)")
            
            // Check if we already have servers (unless forcing refresh)
            if (!forceRefresh) {
                val existingCount = serverDao.getServerCount()
                if (existingCount > 0) {
                    Log.d(TAG, "Servers already exist locally ($existingCount servers), skipping sync")
                    return@withContext Result.success(existingCount)
                }
            }
            
            // Fetch servers from API
            val apiServers = vpnApiClient.fetchFreeServers()
            
            if (apiServers.isEmpty()) {
                Log.w(TAG, "No servers fetched from API")
                return@withContext Result.failure(Exception("No servers available from API"))
            }
            
            Log.d(TAG, "Fetched ${apiServers.size} servers from API, syncing to database...")
            
            // Convert to entities
            val serverEntities = apiServers.map { server ->
                ServerEntity(
                    id = server.id,
                    name = server.name,
                    countryCode = server.countryCode,
                    countryName = server.countryName,
                    host = server.host,
                    port = server.port,
                    protocol = server.protocol.lowercase(),
                    username = server.username,
                    password = server.password,
                    ovpnConfig = server.ovpnConfig,
                    isPremium = server.isPremium,
                    latency = server.latency,
                    speed = server.speed,
                    isFavorite = false // Don't mark as favorite by default
                )
            }
            
            // Clear existing servers if forcing refresh
            if (forceRefresh) {
                serverDao.deleteAllServers()
                Log.d(TAG, "Cleared existing servers for refresh")
            }
            
            // Insert new servers
            serverDao.insertServers(serverEntities)
            
            val syncedCount = serverEntities.size
            Log.d(TAG, "Successfully synced $syncedCount servers to database")
            
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing servers from API", e)
            Result.failure(e)
        }
    }

    /**
     * Merge API servers with existing local servers
     * This keeps local servers (like custom/local servers) and adds new ones from API
     */
    suspend fun mergeServersFromApi(): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Starting server merge from API")
            
            // Fetch servers from API
            val apiServers = vpnApiClient.fetchFreeServers()
            
            if (apiServers.isEmpty()) {
                Log.w(TAG, "No servers fetched from API")
                return@withContext Result.success(0)
            }
            
            // Get existing server IDs
            val existingServerIds = serverDao.getAllServerIds().toSet()
            
            // Filter out servers that already exist
            val newServers = apiServers.filter { it.id !in existingServerIds }
            
            if (newServers.isEmpty()) {
                Log.d(TAG, "No new servers to add")
                return@withContext Result.success(0)
            }
            
            // Convert to entities
            val serverEntities = newServers.map { server ->
                ServerEntity(
                    id = server.id,
                    name = server.name,
                    countryCode = server.countryCode,
                    countryName = server.countryName,
                    host = server.host,
                    port = server.port,
                    protocol = server.protocol.lowercase(),
                    username = server.username,
                    password = server.password,
                    ovpnConfig = server.ovpnConfig,
                    isPremium = server.isPremium,
                    latency = server.latency,
                    speed = server.speed,
                    isFavorite = false
                )
            }
            
            // Insert new servers
            serverDao.insertServers(serverEntities)
            
            val addedCount = serverEntities.size
            Log.d(TAG, "Successfully merged $addedCount new servers from API")
            
            Result.success(addedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error merging servers from API", e)
            Result.failure(e)
        }
    }
}


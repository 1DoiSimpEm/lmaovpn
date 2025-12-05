package com.amobear.freevpn.data.repository

import com.amobear.freevpn.data.local.dao.ServerDao
import com.amobear.freevpn.data.local.entity.toDomain
import com.amobear.freevpn.data.network.NetworkMonitor
import com.amobear.freevpn.domain.model.Country
import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.domain.model.SpeedTestResult
import com.amobear.freevpn.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao,
    private val networkMonitor: NetworkMonitor,
    private val vpnServerSyncService: VpnServerSyncService
) : ServerRepository {

    override suspend fun syncServersFromApi(forceRefresh: Boolean): Result<Int> {
        return vpnServerSyncService.syncServersFromApi(forceRefresh)
    }

    override suspend fun getAllServers(): Flow<List<Server>> {
        return serverDao.getAllServers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getServersByCountry(countryCode: String): Flow<List<Server>> {
        return serverDao.getServersByCountry(countryCode).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCountries(): Flow<List<Country>> {
        return serverDao.getAllServers().map { entities ->
            entities.groupBy { it.countryCode }
                .map { (code, servers) ->
                    Country(
                        code = code,
                        name = servers.first().countryName,
                        flagEmoji = getFlagEmoji(code),
                        serverCount = servers.size
                    )
                }
                .sortedBy { it.name }
        }
    }

    override suspend fun getServerById(serverId: String): Server? {
        return serverDao.getServerById(serverId)?.toDomain()
    }

    override suspend fun pingServer(serverId: String): Long {
        val server = serverDao.getServerById(serverId) ?: return -1L
        val latency = networkMonitor.pingServer(server.host)
        if (latency > 0) {
            serverDao.updateLatency(serverId, latency)
        }
        return latency
    }

    override suspend fun testSpeed(serverId: String): SpeedTestResult {
        val server = serverDao.getServerById(serverId)?.toDomain()
            ?: throw IllegalArgumentException("Server not found")
        return networkMonitor.testSpeed(server)
    }

    override suspend fun toggleFavorite(serverId: String): Result<Unit> {
        return try {
            val server = serverDao.getServerById(serverId)
                ?: return Result.failure(Exception("Server not found"))
            serverDao.updateFavorite(serverId, !server.isFavorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavoriteServers(): Flow<List<Server>> {
        return serverDao.getFavoriteServers().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateServerLatency(serverId: String, latency: Long) {
        serverDao.updateLatency(serverId, latency)
    }

    private fun getFlagEmoji(countryCode: String): String {
        // Convert country code to flag emoji
        return countryCode.uppercase()
            .map { char -> Character.codePointAt("$char", 0) - 0x41 + 0x1F1E6 }
            .map { Character.toChars(it) }
            .joinToString("") { String(it) }
    }
}


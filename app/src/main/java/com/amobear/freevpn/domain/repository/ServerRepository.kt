package com.amobear.freevpn.domain.repository

import com.amobear.freevpn.domain.model.Country
import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.domain.model.SpeedTestResult
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    suspend fun getAllServers(): Flow<List<Server>>
    suspend fun getServersByCountry(countryCode: String): Flow<List<Server>>
    suspend fun getCountries(): Flow<List<Country>>
    suspend fun getServerById(serverId: String): Server?
    suspend fun pingServer(serverId: String): Long
    suspend fun testSpeed(serverId: String): SpeedTestResult
    suspend fun toggleFavorite(serverId: String): Result<Unit>
    suspend fun getFavoriteServers(): Flow<List<Server>>
    suspend fun updateServerLatency(serverId: String, latency: Long)
    
    /**
     * Sync servers from API to local database
     * @param forceRefresh Force refresh even if servers exist locally
     * @return Result with number of servers synced
     */
    suspend fun syncServersFromApi(forceRefresh: Boolean = false): Result<Int>
}


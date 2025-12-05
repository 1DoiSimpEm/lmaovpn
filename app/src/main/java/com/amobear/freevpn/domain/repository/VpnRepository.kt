package com.amobear.freevpn.domain.repository

import com.amobear.freevpn.domain.model.TrafficStats
import com.amobear.freevpn.domain.model.VpnConnection
import kotlinx.coroutines.flow.Flow

interface VpnRepository {
    suspend fun connect(serverId: String): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun resume(): Result<Unit>
    fun observeConnectionState(): Flow<VpnConnection>
    fun observeTrafficStats(): Flow<TrafficStats>
    suspend fun getCurrentConnection(): VpnConnection?
    suspend fun isConnected(): Boolean
}


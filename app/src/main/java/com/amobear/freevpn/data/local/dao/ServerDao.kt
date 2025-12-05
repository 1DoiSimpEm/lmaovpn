package com.amobear.freevpn.data.local.dao

import androidx.room.*
import com.amobear.freevpn.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("SELECT * FROM servers ORDER BY latency ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE countryCode = :countryCode ORDER BY latency ASC")
    fun getServersByCountry(countryCode: String): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :serverId")
    suspend fun getServerById(serverId: String): ServerEntity?

    @Query("SELECT * FROM servers WHERE isFavorite = 1 ORDER BY latency ASC")
    fun getFavoriteServers(): Flow<List<ServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerEntity>)

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Query("UPDATE servers SET latency = :latency WHERE id = :serverId")
    suspend fun updateLatency(serverId: String, latency: Long)

    @Query("UPDATE servers SET isFavorite = :isFavorite WHERE id = :serverId")
    suspend fun updateFavorite(serverId: String, isFavorite: Boolean)

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("DELETE FROM servers")
    suspend fun deleteAllServers()

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun getServerCount(): Int

    @Query("SELECT id FROM servers")
    suspend fun getAllServerIds(): List<String>
}


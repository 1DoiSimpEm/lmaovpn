package com.amobear.freevpn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.amobear.freevpn.domain.model.Server

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val username: String? = null,
    val password: String? = null,
    val ovpnConfig: String? = null,
    val isPremium: Boolean = false,
    val latency: Long = 0L,
    val speed: Double = 0.0,
    val isFavorite: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

fun ServerEntity.toDomain(): Server {
    return Server(
        id = id,
        name = name,
        countryCode = countryCode,
        countryName = countryName,
        host = host,
        port = port,
        protocol = protocol,
        username = username,
        password = password,
        ovpnConfig = ovpnConfig,
        isPremium = isPremium,
        latency = latency,
        speed = speed,
        isFavorite = isFavorite
    )
}

fun Server.toEntity(): ServerEntity {
    return ServerEntity(
        id = id,
        name = name,
        countryCode = countryCode,
        countryName = countryName,
        host = host,
        port = port,
        protocol = protocol,
        username = username,
        password = password,
        ovpnConfig = ovpnConfig,
        isPremium = isPremium,
        latency = latency,
        speed = speed,
        isFavorite = isFavorite
    )
}


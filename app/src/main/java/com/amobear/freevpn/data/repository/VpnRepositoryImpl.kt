package com.amobear.freevpn.data.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.amobear.freevpn.data.local.dao.ServerDao
import com.amobear.freevpn.data.vpn.ConnectionParamsUuidServiceHelper
import com.amobear.freevpn.data.vpn.VpnManager
import com.amobear.freevpn.data.vpn.openvpn.OpenVpnBackend
import com.amobear.freevpn.data.vpn.models.ConnectionParams
import com.amobear.freevpn.data.vpn.models.ConnectionParamsOpenVpn
import com.amobear.freevpn.data.vpn.openvpn.OpenVPNWrapperService
import com.amobear.freevpn.domain.model.TrafficStats
import com.amobear.freevpn.domain.model.VpnConnection
import com.amobear.freevpn.domain.repository.VpnRepository
import com.amobear.freevpn.models.config.TransmissionProtocol
import com.amobear.freevpn.utils.Storage
import com.amobear.freevpn.utils.OvpnFileReader
import dagger.hilt.android.qualifiers.ApplicationContext
import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject

class VpnRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vpnManager: VpnManager,
    private val serverDao: ServerDao,
    private val openVpnBackend: OpenVpnBackend
) : VpnRepository {

    override suspend fun connect(serverId: String): Result<Unit> {
        return try {
            val serverEntity = serverDao.getServerById(serverId)
                ?: return Result.failure(Exception("Server not found"))

            val server = serverEntity.let {
                com.amobear.freevpn.domain.model.Server(
                    id = it.id,
                    name = it.name,
                    countryCode = it.countryCode,
                    countryName = it.countryName,
                    host = it.host,
                    port = it.port,
                    protocol = it.protocol,
                    username = it.username,
                    password = it.password,
                    ovpnConfig = it.ovpnConfig,
                    isPremium = it.isPremium,
                    latency = it.latency,
                    speed = it.speed,
                    isFavorite = it.isFavorite,
                    isIPv6Supported = false
                )
            }

            vpnManager.setCurrentServer(server)

            Log.d("VpnRepository", "Connecting to server: ${server.name}")
            
            val transmissionProtocol = when (server.protocol.lowercase()) {
                "udp" -> TransmissionProtocol.UDP
                "tcp" -> TransmissionProtocol.TCP
                "tls" -> TransmissionProtocol.TLS
                else -> TransmissionProtocol.UDP
            }

            val connectionParams = ConnectionParamsOpenVpn(
                server = server,
                connectingDomain = server.host,
                entryIp = server.host,
                transmission = transmissionProtocol,
                port = server.port,
                ipv6SettingEnabled = false
            )
            
            openVpnBackend.connect(connectionParams)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VpnRepository", "Error connecting", e)
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            Log.d("VpnRepository", "Disconnecting VPN")
            
            val intent = Intent(context, OpenVPNWrapperService::class.java)
            intent.action = OpenVPNService.DISCONNECT_VPN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            openVpnBackend.disconnect()

            ConnectionParams.deleteFromStore("user disconnect")
            
            openVpnBackend.setConnectionParamsForTracking(null)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VpnRepository", "Error disconnecting", e)
            Result.failure(e)
        }
    }

    override suspend fun pause(): Result<Unit> {
        return try {
            Log.d("VpnRepository", "Pausing VPN")
            val intent = Intent(context, OpenVPNWrapperService::class.java).apply {
                action = OpenVPNService.PAUSE_VPN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VpnRepository", "Error pausing VPN", e)
            Result.failure(e)
        }
    }

    override suspend fun resume(): Result<Unit> {
        return try {
            Log.d("VpnRepository", "Resuming VPN")
            val intent = Intent(context, OpenVPNWrapperService::class.java).apply {
                action = OpenVPNService.RESUME_VPN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VpnRepository", "Error resuming VPN", e)
            Result.failure(e)
        }
    }

    override fun observeConnectionState(): Flow<VpnConnection> = callbackFlow {
        var connectionStartTime = 0L

        val stateListener = object : VpnStatus.StateListener {
            override fun updateState(
                state: String?,
                logmessage: String?,
                localizedResId: Int,
                level: ConnectionStatus?,
                intent: Intent?
            ) {
                val connectionState = when (level) {
                    ConnectionStatus.LEVEL_CONNECTED -> com.amobear.freevpn.domain.model.ConnectionState.CONNECTED
                    ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
                    ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
                    ConnectionStatus.LEVEL_START,
                    ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> com.amobear.freevpn.domain.model.ConnectionState.CONNECTING
                    ConnectionStatus.LEVEL_NOTCONNECTED,
                    ConnectionStatus.LEVEL_VPNPAUSED -> com.amobear.freevpn.domain.model.ConnectionState.DISCONNECTED
                    ConnectionStatus.LEVEL_NONETWORK -> com.amobear.freevpn.domain.model.ConnectionState.NO_NETWORK
                    ConnectionStatus.LEVEL_AUTH_FAILED -> com.amobear.freevpn.domain.model.ConnectionState.AUTH_FAILED
                    else -> com.amobear.freevpn.domain.model.ConnectionState.DISCONNECTED
                }

                if (connectionState == com.amobear.freevpn.domain.model.ConnectionState.CONNECTED && connectionStartTime == 0L) {
                    connectionStartTime = System.currentTimeMillis()
                }
                if (connectionState == com.amobear.freevpn.domain.model.ConnectionState.DISCONNECTED) {
                    connectionStartTime = 0L
                }

                val server = vpnManager.getCurrentServer()

                val connection = VpnConnection(
                    serverId = server?.id ?: "",
                    serverName = server?.name ?: "",
                    state = connectionState,
                    startTime = connectionStartTime,
                    duration = if (connectionStartTime > 0) System.currentTimeMillis() - connectionStartTime else 0L
                )

                trySend(connection)
            }

            override fun setConnectedVPN(uuid: String?) {
                // no-op
            }
        }

        VpnStatus.addStateListener(stateListener)

        awaitClose {
            VpnStatus.removeStateListener(stateListener)
        }
    }

    override fun observeTrafficStats(): Flow<TrafficStats> {
        return callbackFlow {
            val byteCountListener = VpnStatus.ByteCountListener { inBytes, outBytes, diffIn, diffOut ->
                val stats = TrafficStats(
                    uploadBytes = outBytes,
                    downloadBytes = inBytes,
                    uploadSpeed = diffOut.toDouble(),
                    downloadSpeed = diffIn.toDouble()
                )
                trySend(stats)
            }

            VpnStatus.addByteCountListener(byteCountListener)

            awaitClose {
                VpnStatus.removeByteCountListener(byteCountListener)
            }
        }
    }

    override suspend fun getCurrentConnection(): VpnConnection {
        val state = openVpnBackend.selfStateFlow.value
        val connectionState = when (state) {
            is com.amobear.freevpn.data.vpn.VpnState.Connected -> com.amobear.freevpn.domain.model.ConnectionState.CONNECTED
            else -> com.amobear.freevpn.domain.model.ConnectionState.DISCONNECTED
        }
        
        return VpnConnection(
            serverId = "",
            serverName = "",
            state = connectionState,
            startTime = 0L,
            duration = 0L
        )
    }

    override suspend fun isConnected(): Boolean {
        return openVpnBackend.selfStateFlow.value is com.amobear.freevpn.data.vpn.VpnState.Connected
    }
}


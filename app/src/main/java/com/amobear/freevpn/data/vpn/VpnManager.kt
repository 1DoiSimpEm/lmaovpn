package com.amobear.freevpn.data.vpn

import com.amobear.freevpn.domain.model.ConnectionState
import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.domain.model.TrafficStats
import com.amobear.freevpn.domain.model.VpnConnection
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnManager @Inject constructor(
) {
    private var currentServer: Server? = null
    private var connectionStartTime: Long = 0
    private var lastUploadBytes: Long = 0
    private var lastDownloadBytes: Long = 0
    private var lastUpdateTime: Long = 0

    fun observeConnectionState(): Flow<VpnConnection> = callbackFlow {
        val stateListener = object : VpnStatus.StateListener {
            override fun updateState(
                state: String?,
                logmessage: String?,
                localizedResId: Int,
                level: ConnectionStatus?,
                intent: android.content.Intent?
            ) {
                val connectionState = mapConnectionStatus(level)

                if (connectionState == ConnectionState.CONNECTED && connectionStartTime == 0L) {
                    connectionStartTime = System.currentTimeMillis()
                }

                if (connectionState == ConnectionState.DISCONNECTED) {
                    connectionStartTime = 0
                    lastUploadBytes = 0
                    lastDownloadBytes = 0
                }

                val connection = VpnConnection(
                    serverId = currentServer?.id ?: "",
                    serverName = currentServer?.name ?: "",
                    state = connectionState,
                    startTime = connectionStartTime,
                    duration = if (connectionStartTime > 0) {
                        System.currentTimeMillis() - connectionStartTime
                    } else 0L,
                    uploadBytes = lastUploadBytes,
                    downloadBytes = lastDownloadBytes
                )

                trySend(connection)
            }

            override fun setConnectedVPN(uuid: String?) {
                // Handle connected VPN
            }
        }

        VpnStatus.addStateListener(stateListener)

        // Send initial state
        val initialConnection = VpnConnection(
            serverId = currentServer?.id ?: "",
            serverName = currentServer?.name ?: "",
            state = ConnectionState.DISCONNECTED,
            startTime = 0L,
            duration = 0L
        )
        trySend(initialConnection)

        awaitClose {
            VpnStatus.removeStateListener(stateListener)
        }
    }

    fun observeTrafficStats(): Flow<TrafficStats> = callbackFlow {
        val byteCountListener = object : VpnStatus.ByteCountListener {
            override fun updateByteCount(inBytes: Long, outBytes: Long, diffIn: Long, diffOut: Long) {
                val currentTime = System.currentTimeMillis()
                val timeDiff = if (lastUpdateTime > 0) {
                    (currentTime - lastUpdateTime) / 1000.0 // seconds
                } else 1.0

                lastUploadBytes = outBytes
                lastDownloadBytes = inBytes
                lastUpdateTime = currentTime

                val uploadSpeed = if (timeDiff > 0) diffOut / timeDiff else 0.0
                val downloadSpeed = if (timeDiff > 0) diffIn / timeDiff else 0.0

                val stats = TrafficStats(
                    uploadBytes = outBytes,
                    downloadBytes = inBytes,
                    uploadSpeed = uploadSpeed,
                    downloadSpeed = downloadSpeed
                )

                trySend(stats)
            }
        }

        VpnStatus.addByteCountListener(byteCountListener)

        // Send initial stats
        val initialStats = TrafficStats(0, 0, 0.0, 0.0)
        trySend(initialStats)

        awaitClose {
            VpnStatus.removeByteCountListener(byteCountListener)
        }
    }

    fun createVpnProfile(server: Server): VpnProfile {
        val profile = VpnProfile(server.name)
        // Configure primary connection parameters
        profile.mServerName = server.host
        profile.mServerPort = server.port.toString()
        profile.mUseUdp = server.protocol.equals("udp", ignoreCase = true)

        // Ensure we have at least one connection and it's enabled
        // checkProfile() requires at least one enabled connection
        if (profile.mConnections.isEmpty()) {
            profile.mConnections = arrayOf(de.blinkt.openvpn.core.Connection())
        }
        
        // Update the first connection with server details and enable it
        val connection = profile.mConnections[0]
        connection.mServerName = server.host
        connection.mServerPort = server.port.toString()
        connection.mUseUdp = server.protocol.equals("udp", ignoreCase = true)
        connection.mEnabled = true  // Critical: connection must be enabled for checkProfile() to pass

        // Use simple username/password auth to avoid requiring a user certificate.
        profile.mAuthenticationType = VpnProfile.TYPE_USERPASS
        profile.mUsername = server.username ?: ""
        profile.mPassword = server.password ?: ""
        
        // Ensure mUsePull is true (default) so we don't need to set IPv4Address
        profile.mUsePull = true
        profile.mUseDefaultRoute = true

        if (!server.ovpnConfig.isNullOrEmpty()) {
            // Parse and configure from .ovpn config
            // This would require ConfigParser from OpenVPN module
        }

        return profile
    }

    fun setCurrentServer(server: Server) {
        currentServer = server
    }

    fun getCurrentServer(): Server? = currentServer

    private fun mapConnectionStatus(status: ConnectionStatus?): ConnectionState {
        return when (status) {
            ConnectionStatus.LEVEL_CONNECTED -> ConnectionState.CONNECTED
            ConnectionStatus.LEVEL_VPNPAUSED -> ConnectionState.DISCONNECTED
            ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
            ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET -> ConnectionState.CONNECTING
            ConnectionStatus.LEVEL_NONETWORK -> ConnectionState.NO_NETWORK
            ConnectionStatus.LEVEL_NOTCONNECTED -> ConnectionState.DISCONNECTED
            ConnectionStatus.LEVEL_AUTH_FAILED -> ConnectionState.AUTH_FAILED
            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> ConnectionState.CONNECTING
            else -> ConnectionState.DISCONNECTED
        }
    }
}


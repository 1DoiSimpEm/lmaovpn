/*
 * Copyright (c) 2020 Proton AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.amobear.freevpn.data.vpn.models

import android.util.Log
import com.amobear.freevpn.data.vpn.ProtocolSelection
import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.models.config.TransmissionProtocol
import com.amobear.freevpn.models.config.VpnProtocol
import com.amobear.freevpn.utils.Storage
import java.io.Serializable
import java.util.UUID

open class ConnectionParams(
    val server: Server,
    val connectingDomain: String?,
    private val protocol: VpnProtocol?,
    val entryIp: String? = null,
    val port: Int? = null,
    protected val transmissionProtocol: TransmissionProtocol? = null,
    val uuid: UUID = UUID.randomUUID(),
    val enableIPv6: Boolean = false
) : java.io.Serializable {
    
    // Don't serialize connectIntent, create it on demand from server.id
    val connectIntent: SimpleConnectIntent get() = SimpleConnectIntent.Server(server.id)
    
    open val info get() = "IP: $connectingDomain/$entryIp Protocol: $protocol Server: ${server.name}"
    
    val protocolSelection get() = protocol?.let { ProtocolSelection(it, transmissionProtocol) }
    
    val bouncing: String? get() = connectingDomain?.takeIf(String::isNotBlank)
    
    override fun toString() = info
    
    fun hasSameProtocolParams(other: ConnectionParams) =
        this.javaClass == other.javaClass &&
            other.protocol == protocol &&
            other.transmissionProtocol == transmissionProtocol &&
            other.port == port
    
    companion object {
        fun store(params: ConnectionParams?) {
            Log.d("ConnectionParams", "storing connection params (${params?.connectingDomain})")
            Storage.save(params, ConnectionParams::class.java)
        }
        
        fun deleteFromStore(msg: String) {
            Log.d("ConnectionParams", "removing connection params ($msg)")
            Storage.delete(ConnectionParams::class.java)
        }
        
        fun readIntentFromStore(expectedUuid: UUID? = null): SimpleConnectIntent? {
            val value = Storage.load(ConnectionParams::class.java, ConnectionParams::class.java)
                ?.takeIf { expectedUuid == null || it.uuid == expectedUuid }
                ?: return null
            return value.connectIntent
        }
    }
}

sealed class SimpleConnectIntent : Serializable {
    data class Server(val serverId: String) : SimpleConnectIntent()
    data class GuestHole(val serverId: String) : SimpleConnectIntent()
}


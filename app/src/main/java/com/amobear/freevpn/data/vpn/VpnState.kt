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
package com.amobear.freevpn.data.vpn

import android.content.Context
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

interface VpnStateSource {
    val selfStateFlow: MutableStateFlow<VpnState>
    val selfState get() = selfStateFlow.value

    fun setSelfState(value: VpnState) {
        selfStateFlow.value = value
    }
}

sealed class VpnState(val isEstablishingConnection: Boolean) {
    object Disabled : VpnState(false)

    object ScanningPorts : VpnState(true)
    object CheckingAvailability : VpnState(true)
    object WaitingForNetwork : VpnState(true)
    object Connecting : VpnState(true)
    object Reconnecting : VpnState(true)
    data class Error(
        val type: ErrorType,
        val description: String? = null,
        val isFinal: Boolean
    ) : VpnState(isEstablishingConnection = !isFinal) {
        override fun toString() = "$name($type) + $description"
    }

    object Connected : VpnState(false)
    object Disconnecting : VpnState(false)

    val name = javaClass.simpleName.uppercase(Locale.ROOT)
    override fun toString() = name
}

fun VpnState.isConnectedOrConnecting() = this is VpnState.Connected || isEstablishingConnection

enum class ErrorType {
    AUTH_FAILED_INTERNAL,
    AUTH_FAILED,
    PEER_AUTH_FAILED,
    NO_PROFILE_FALLBACK_AVAILABLE,
    UNREACHABLE,
    UNREACHABLE_INTERNAL,
    MAX_SESSIONS,
    GENERIC_ERROR,
    MULTI_USER_PERMISSION,
    LOCAL_AGENT_ERROR,
    SERVER_ERROR,
    // Delinquent types should no longer exist, so should be safe to remove in future
    POLICY_VIOLATION_DELINQUENT,
    POLICY_VIOLATION_LOW_PLAN,
    POLICY_VIOLATION_BAD_BEHAVIOUR,
    TORRENT_NOT_ALLOWED,
    KEY_USED_MULTIPLE_TIMES;

    @StringRes
    fun mapToErrorRes(additionalDetails: String? = null): Int {
        // Return 0 for now since we don't have string resources
        return 0
    }

    fun mapToErrorMessage(context: Context, additionalDetails: String? = null): String {
        return when (this) {
            PEER_AUTH_FAILED -> "Peer authentication failed"
            AUTH_FAILED -> "Authentication failed"
            UNREACHABLE -> "Server unreachable"
            POLICY_VIOLATION_DELINQUENT -> "Policy violation"
            MULTI_USER_PERMISSION -> "Multi-user permission error"
            TORRENT_NOT_ALLOWED -> "Torrent not allowed"
            NO_PROFILE_FALLBACK_AVAILABLE -> "No profile fallback available"
            else ->
                if (additionalDetails.isNullOrEmpty())
                    "Generic error"
                else
                    "Error: $additionalDetails"
        }
    }
}

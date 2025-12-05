/*
 * Copyright (c) 2023. Proton AG
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

package com.amobear.freevpn.settings.data

import android.os.Parcelable
import kotlinx.serialization.Serializable

enum class SplitTunnelingMode {
    INCLUDE_ONLY,
    EXCLUDE_ONLY
}

@Serializable
data class SplitTunnelingSettings(
    val isEnabled: Boolean = false,
    val mode: SplitTunnelingMode = SplitTunnelingMode.INCLUDE_ONLY,
    val excludedIps: List<String> = emptyList(),
    val excludedApps: List<String> = emptyList(),
    val includedIps: List<String> = emptyList(),
    val includedApps: List<String> = emptyList(),
) {
    fun isEffectivelySameAs(other: SplitTunnelingSettings): Boolean {
        return !isEnabled && !other.isEnabled
            || mode == other.mode && effectiveApps() == other.effectiveApps() && effectiveIps() == other.effectiveIps()
            // Both INCLUDE_ONLY and EXCLUDE_ONLY with empty values behave the same as split tunneling disabled:
            || isEmpty() && other.isEmpty()
    }

    fun currentModeApps() = when (mode) {
        SplitTunnelingMode.INCLUDE_ONLY -> includedApps
        SplitTunnelingMode.EXCLUDE_ONLY -> excludedApps
    }

    fun currentModeIps() = when (mode) {
        SplitTunnelingMode.INCLUDE_ONLY -> includedIps
        SplitTunnelingMode.EXCLUDE_ONLY -> excludedIps
    }

    fun isEmpty() = currentModeApps().isEmpty() && currentModeIps().isEmpty()

    private fun effectiveApps() = if (isEnabled) currentModeApps() else emptyList()
    private fun effectiveIps() = if (isEnabled) currentModeIps() else emptyList()
}


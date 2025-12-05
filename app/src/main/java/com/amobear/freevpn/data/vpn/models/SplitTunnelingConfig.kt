/*
 * Copyright (c) 2024. Proton AG
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

import androidx.annotation.VisibleForTesting
import com.amobear.freevpn.data.vpn.models.SimpleConnectIntent

@VisibleForTesting
val DIRECT_CONNECTIONS_EXCLUDED_APPS = listOf(
    "com.google.android.projection.gearhead", // Android Auto
)

interface SplitTunnelAppsConfigurator {
    fun includeApplications(packageNames: List<String>)
    fun excludeApplications(packageNames: List<String>)
}

fun applyAppsSplitTunneling(
    configurator: SplitTunnelAppsConfigurator,
    connectIntent: SimpleConnectIntent,
    myPackageName: String,
    splitTunneling: com.amobear.freevpn.settings.data.SplitTunnelingSettings,
    allowDirectLanConnections: Boolean,
) {
    with(splitTunneling) {
        val directConnectionsExcludedApps =
            if (allowDirectLanConnections) DIRECT_CONNECTIONS_EXCLUDED_APPS
            else emptyList()
        when {
            // For GuestHole, include only our app
            connectIntent is SimpleConnectIntent.GuestHole ->
                configurator.includeApplications(listOf(myPackageName))

            isEnabled && mode == com.amobear.freevpn.settings.data.SplitTunnelingMode.INCLUDE_ONLY && includedApps.isNotEmpty() -> {
                configurator.includeApplications(listOf(myPackageName))
                configurator.includeApplications(includedApps)
            }

            isEnabled && mode == com.amobear.freevpn.settings.data.SplitTunnelingMode.EXCLUDE_ONLY && excludedApps.isNotEmpty() -> {
                val appsToExclude = (excludedApps + directConnectionsExcludedApps).filter { it != myPackageName }
                configurator.excludeApplications(appsToExclude)
            }

            // When split tunneling disabled or include enabled without included apps
            allowDirectLanConnections ->
                configurator.excludeApplications(directConnectionsExcludedApps)
        }
    }
}


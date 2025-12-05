/*
 * Copyright (c) 2021 Proton AG
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
package com.amobear.freevpn.data.vpn.openvpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import com.amobear.freevpn.data.vpn.ConnectionParamsUuidServiceHelper
import com.amobear.freevpn.data.vpn.CurrentVpnServiceProvider
import com.amobear.freevpn.data.vpn.VpnConnectionManager
import com.amobear.freevpn.data.vpn.models.ConnectionParams
import com.amobear.freevpn.data.vpn.models.ConnectionParamsOpenVpn
import com.amobear.freevpn.data.vpn.usecases.ComputeAllowedIPs
import com.amobear.freevpn.data.vpn.usecases.SettingsForConnectionSync
import com.amobear.freevpn.notifications.NotificationHelper
import com.amobear.freevpn.utils.Constants
import com.amobear.freevpn.utils.Storage
import dagger.hilt.android.AndroidEntryPoint
import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus.StateListener
import javax.inject.Inject

@AndroidEntryPoint
class OpenVPNWrapperService : OpenVPNService(), StateListener {

    @Inject lateinit var settingsForConnection: SettingsForConnectionSync
    @Inject lateinit var vpnConnectionManager: VpnConnectionManager
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var currentVpnServiceProvider: CurrentVpnServiceProvider
    @Inject lateinit var computeAllowedIPs: ComputeAllowedIPs
    @Inject lateinit var openVpnBackend: OpenVpnBackend

    private val connectionParamsUuid = ConnectionParamsUuidServiceHelper()
    private var currentIntent: Intent? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.initNotificationChannel(applicationContext)
        createOpenVPNNotificationChannels()
        currentVpnServiceProvider.onVpnServiceCreated(OpenVpnBackend::class, this)
        
        // Register OpenVpnBackend as StateListener in the service process
        // This ensures state updates are received even when service runs in separate process
        android.util.Log.i("OpenVPNWrapperService", "Registering OpenVpnBackend as VpnStatus.StateListener in service process")
        de.blinkt.openvpn.core.VpnStatus.addStateListener(openVpnBackend)
    }

    private fun createOpenVPNNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Background channel
            val bgChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_BG_ID,
                "VPN Background",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background VPN notifications"
                enableLights(false)
                lightColor = Color.DKGRAY
            }
            notificationManager.createNotificationChannel(bgChannel)

            // Status channel
            val statusChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                "VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN connection status notifications"
                enableLights(true)
                lightColor = Color.BLUE
            }
            notificationManager.createNotificationChannel(statusChannel)

            val userReqChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_USERREQ_ID,
                "VPN User Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent VPN user requests"
                enableVibration(true)
                lightColor = Color.CYAN
            }
            notificationManager.createNotificationChannel(userReqChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentIntent = intent
        android.util.Log.d("OpenVPNWrapperService", "onStartCommand: intent=$intent")
        if (intent != null) {
            val profileUuid = intent.getStringExtra(de.blinkt.openvpn.VpnProfile.EXTRA_PROFILEUUID)
            android.util.Log.d("OpenVPNWrapperService", "Intent has profile UUID: $profileUuid")
            if (profileUuid != null) {
                val profile = de.blinkt.openvpn.core.ProfileManager.get(applicationContext, profileUuid)
                android.util.Log.d("OpenVPNWrapperService", "Profile from ProfileManager: ${if (profile != null) "found (${profile.mName})" else "NOT FOUND"}")
            }
        }
        startForeground(Constants.NOTIFICATION_ID, notificationHelper.buildNotification())
        connectionParamsUuid.onStartCommand(intent)
        return super.onStartCommand(intent, flags, startId)
    }


     fun onProcessRestore(): Boolean {
        val connectIntent = ConnectionParams.readIntentFromStore() ?: return false
        return vpnConnectionManager.onRestoreProcess(connectIntent, "service restart")
    }

    override fun onDestroy() {
        // Unregister listener when service is destroyed
        android.util.Log.i("OpenVPNWrapperService", "Unregistering OpenVpnBackend as VpnStatus.StateListener")
        de.blinkt.openvpn.core.VpnStatus.removeStateListener(openVpnBackend)
        
        vpnConnectionManager.onVpnServiceDestroyed(connectionParamsUuid.last)
        currentVpnServiceProvider.onVpnServiceDestroyed(OpenVpnBackend::class)
        super.onDestroy()
    }
}

/*
 * Copyright (c) 2022. Proton AG
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
package com.amobear.freevpn.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import androidx.core.app.NotificationCompat
import com.amobear.freevpn.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    fun buildNotification(): Notification {
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("VPN Connected")
            .setContentText("FreeVPN is protecting your connection")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "com.amobear.freevpn"

        fun initNotificationChannel(context: Context) {
            val channelOneName = "VPN Channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    CHANNEL_ID, channelOneName,
                    NotificationManager.IMPORTANCE_LOW)
                notificationChannel.enableLights(true)
                notificationChannel.setShowBadge(true)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                val manager =
                    context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(notificationChannel)
            }
        }
    }
}


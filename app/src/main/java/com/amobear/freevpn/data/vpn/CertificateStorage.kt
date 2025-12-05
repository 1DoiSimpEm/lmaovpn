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

package com.amobear.freevpn.data.vpn

import android.util.Log
import com.amobear.freevpn.utils.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CertInfo(
    val privateKeyPem: String,
    val publicKeyPem: String,
    val x25519Base64: String,
    val expiresAt: Long = 0,
    val refreshAt: Long = 0,
    val certificatePem: String? = null,
    val refreshCount: Int = 0,
)

@Singleton
class CertificateStorage @Inject constructor(
    mainScope: CoroutineScope,
) {
    private val inMemoryCache = mutableMapOf<String, CertInfo>()

    suspend fun get(sessionId: String): CertInfo? {
        return inMemoryCache[sessionId] ?: getFromStore(sessionId)?.also {
            inMemoryCache[sessionId] = it
        }
    }

    suspend fun put(sessionId: String, info: CertInfo) {
        inMemoryCache[sessionId] = info
        putInStore(sessionId, info)
    }

    suspend fun remove(sessionId: String) {
        inMemoryCache.remove(sessionId)
        Storage.delete("cert_$sessionId")
    }

    private suspend fun getFromStore(sessionId: String): CertInfo? {
        return try {
            Storage.load(CertInfo::class.java, "cert_$sessionId")
        } catch (e: Exception) {
            Log.e("CertificateStorage", "Failed to load certificate for $sessionId", e)
            null
        }
    }

    private suspend fun putInStore(sessionId: String, info: CertInfo) {
        try {
            Storage.save(info, "cert_$sessionId")
        } catch (e: Exception) {
            Log.e("CertificateStorage", "Failed to save certificate for $sessionId", e)
        }
    }
}


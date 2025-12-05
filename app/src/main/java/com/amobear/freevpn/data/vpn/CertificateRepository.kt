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

package com.amobear.freevpn.data.vpn

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CertificateRepository @Inject constructor(
    private val mainScope: CoroutineScope,
    private val certificateStorage: CertificateStorage,
) {
    sealed class CertificateResult {
        data class Error(val error: String? = null) : CertificateResult()
        data class Success(val certificate: String, val privateKeyPem: String) : CertificateResult()
    }

    private val _currentCertUpdateFlow = MutableSharedFlow<CertificateResult.Success>()
    val currentCertUpdateFlow: Flow<CertificateResult.Success> = _currentCertUpdateFlow.asSharedFlow()

    private val guestX25519Key = generateX25519Base64()

    suspend fun generateNewKey(sessionId: String): CertInfo = withContext(mainScope.coroutineContext) {
        val info = generateCertInfo()
        certificateStorage.put(sessionId, info)
        Log.d("CertificateRepository", "Generated new key for session: $sessionId")
        info
    }

    suspend fun getCertificate(sessionId: String, cancelOngoing: Boolean = false): CertificateResult =
        withContext(mainScope.coroutineContext) {
            val certInfo = certificateStorage.get(sessionId)
            if (certInfo?.certificatePem != null && certInfo.expiresAt > System.currentTimeMillis()) {
                CertificateResult.Success(certInfo.certificatePem, certInfo.privateKeyPem)
            } else {
                // For now, return error if no certificate available
                // In full implementation, this would fetch from backend
                CertificateResult.Error("No certificate available")
            }
        }

    /**
     * Returns the locally stored certificate.
     * Does not try to fetch it if there isn't one nor refresh it if its expired.
     * In most cases getCertificate should be used.
     */
    suspend fun getCertificateWithoutRefresh(sessionId: String): CertificateResult =
        withContext(mainScope.coroutineContext) {
            val certInfo = certificateStorage.get(sessionId)
            if (certInfo?.certificatePem != null) {
                CertificateResult.Success(certInfo.certificatePem, certInfo.privateKeyPem)
            } else {
                CertificateResult.Error(null)
            }
        }

    suspend fun updateCertificate(sessionId: String, cancelOngoing: Boolean): CertificateResult {
        // For now, just return error as we don't have backend API
        // In full implementation, this would call API to get certificate
        return CertificateResult.Error("Certificate update not implemented")
    }

    suspend fun getX25519Key(sessionId: String?): String {
        return sessionId?.let {
            val certInfo = certificateStorage.get(it)
            certInfo?.x25519Base64 ?: generateNewKey(it).x25519Base64
        } ?: guestX25519Key
    }

    suspend fun clear(sessionId: String) = withContext(mainScope.coroutineContext) {
        certificateStorage.remove(sessionId)
    }

    private fun generateCertInfo(): CertInfo {
        // Generate dummy certificate info for now
        // In full implementation, this would use crypto library to generate real keys
        val uuid = UUID.randomUUID().toString()
        return CertInfo(
            privateKeyPem = "-----BEGIN PRIVATE KEY-----\n$uuid\n-----END PRIVATE KEY-----",
            publicKeyPem = "-----BEGIN PUBLIC KEY-----\n$uuid\n-----END PUBLIC KEY-----",
            x25519Base64 = uuid,
            expiresAt = System.currentTimeMillis() + 86400000L, // 24 hours
            refreshAt = System.currentTimeMillis() + 43200000L, // 12 hours
            certificatePem = null,
            refreshCount = 0
        )
    }

    private fun generateX25519Base64(): String {
        return UUID.randomUUID().toString()
    }
}


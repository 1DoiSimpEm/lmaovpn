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

import com.amobear.freevpn.data.vpn.models.applyAppsSplitTunneling
import com.amobear.freevpn.data.vpn.models.SplitTunnelAppsConfigurator
import com.amobear.freevpn.data.vpn.usecases.ComputeAllowedIPs
import com.amobear.freevpn.data.vpn.usecases.SettingsForConnection
import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.models.config.TransmissionProtocol
import com.amobear.freevpn.models.config.VpnProtocol
import com.amobear.freevpn.utils.Constants
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.Connection

// This should correspond to N_DHCP_ADDR in OpenVPN code. 10 user servers + 1 Proton DNS at the end.
private const val OPENVPN_MAX_DNS_COUNT = 11

class ConnectionParamsOpenVpn(
    server: Server,
    connectingDomain: String?,
    entryIp: String?,
    transmission: TransmissionProtocol,
    port: Int,
    ipv6SettingEnabled: Boolean,
) : ConnectionParams(
    server,
    connectingDomain,
    VpnProtocol.OpenVPN,
    entryIp,
    port,
    transmission,
    java.util.UUID.randomUUID(),
    ipv6SettingEnabled && server.isIPv6Supported
), java.io.Serializable {

    override val info get() = "${super.info} $transmissionProtocol port: $port"

    fun openVpnProfile(
        myPackageName: String,
        connectionSettings: SettingsForConnection.ConnectionSettings,
        clientCertificate: CertificateData?,
        computeAllowedIPs: ComputeAllowedIPs,
    ) = VpnProfile(server.name).apply {
        if (clientCertificate != null) {
            mAuthenticationType = VpnProfile.TYPE_CERTIFICATES
            mClientKeyFilename = inlineFile(clientCertificate.key)
            mClientCertFilename = inlineFile(clientCertificate.certificate)
        } else {
            mAuthenticationType = VpnProfile.TYPE_USERPASS
            mUsername = server.username ?: "guest"
            mPassword = server.password ?: "guest"
        }
        // Check if connecting to local server (192.168.x.x or localhost)
        val isLocalServer = server.host.startsWith("192.168.") || 
                           server.host.startsWith("10.") || 
                           server.host == "localhost" || 
                           server.host == "127.0.0.1"
        
        if (isLocalServer) {
            // Use local server certificates
            mCaFilename = Constants.LOCAL_VPN_ROOT_CERTS
            mTLSAuthFilename = LOCAL_TLS_AUTH_KEY
            mCipher = "AES-256-CBC"  // Match server config
            mAuth = "SHA256"  // Match server config
        } else {
            // Use Proton VPN certificates
            mCaFilename = Constants.VPN_ROOT_CERTS
            mTLSAuthFilename = TLS_AUTH_KEY
            mCipher = "AES-256-GCM"
        }
        mTLSAuthDirection = "tls-crypt"
        mUseTLSAuth = true
        mTunMtu = 1500
        mMssFix = connectionSettings.mtuSize - 40
        mExpectTLSCert = true
        mX509AuthType = VpnProfile.X509_VERIFY_TLSREMOTE_SAN
        mCheckRemoteCN = true
        mRemoteCN = connectingDomain ?: server.host
        mPersistTun = true
        val splitsTunnel = connectionSettings.splitTunneling.isEnabled || connectionSettings.lanConnections
        if (connectIntent !is SimpleConnectIntent.GuestHole && splitsTunnel) {
            // Split tunneling logic - simplified for now since we don't have full ComputeAllowedIPs implementation
            // TODO: Implement full split tunneling logic when ComputeAllowedIPs is complete
            mUseDefaultRoute = false
            mUseDefaultRoutev6 = false
        } else {
            mUseDefaultRoute = true
            mUseDefaultRoutev6 = false
        }

        mOverrideDNS = connectionSettings.customDns.effectiveEnabled
        // Leave one spot for the Proton VPN DNS at the end of the list. It will be set via control message from the
        // server.
        mCustomDNS = connectionSettings.customDns.effectiveDnsList.take(OPENVPN_MAX_DNS_COUNT - 1)

        val appsSplitTunnelingConfigurator = SplitTunnelAppsOpenVpnConfigurator(this)
        applyAppsSplitTunneling(
            appsSplitTunnelingConfigurator,
            connectIntent,
            myPackageName,
            connectionSettings.splitTunneling,
            allowDirectLanConnections = connectionSettings.lanConnectionsAllowDirect,
        )
        mConnections[0] = Connection().apply {
            mServerName = entryIp ?: server.host
            mUseUdp = transmissionProtocol == TransmissionProtocol.UDP
            mServerPort = port.toString()
            mCustomConfiguration = ""
        }
        if (enableIPv6 == true && server.isIPv6Supported) {
            // Will push custom config enabling v6 (UV_IPV6 1) to server
            mPushPeerInfo = true
            mUseCustomConfig = true
            mCustomConfigOptions += "setenv UV_IPV6 1\n"
        }
    }

    private fun inlineFile(data: String) = "[[INLINE]]$data"

    private class SplitTunnelAppsOpenVpnConfigurator(private val profile: VpnProfile) : SplitTunnelAppsConfigurator {
        override fun includeApplications(packageNames: List<String>) {
            profile.mAllowedAppsVpn += packageNames
            profile.mAllowedAppsVpnAreDisallowed = false
        }

        override fun excludeApplications(packageNames: List<String>) {
            profile.mAllowedAppsVpnAreDisallowed = true
            profile.mAllowedAppsVpn += packageNames
        }
    }

    companion object {

        const val TLS_AUTH_KEY =
            "[[INLINE]]# 2048 bit OpenVPN static key\n" +
            "-----BEGIN OpenVPN Static key V1-----\n" +
            Constants.TLS_AUTH_KEY_HEX +
            "-----END OpenVPN Static key V1-----"
        
        const val LOCAL_TLS_AUTH_KEY =
            "[[INLINE]]# 2048 bit OpenVPN static key\n" +
            "-----BEGIN OpenVPN Static key V1-----\n" +
            Constants.LOCAL_TLS_AUTH_KEY_HEX +
            "-----END OpenVPN Static key V1-----"
    }
}

data class CertificateData(
    val key: String,
    val certificate: String
) : java.io.Serializable

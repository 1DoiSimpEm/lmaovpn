package com.amobear.freevpn.utils

import android.content.Context
import android.util.Log
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Utility class to read and parse .ovpn configuration files from assets
 */
object OvpnFileReader {
    private const val TAG = "OvpnFileReader"

    /**
     * Read .ovpn file from assets and return its content as String
     */
    fun readOvpnFromAssets(context: Context, filename: String): String? {
        return try {
            val inputStream = context.assets.open(filename)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val config = StringBuilder()
            
            reader.use { r ->
                var line: String?
                while (r.readLine().also { line = it } != null) {
                    config.append(line).append("\n")
                }
            }
            
            Log.d(TAG, "Successfully read .ovpn file: $filename")
            config.toString()
        } catch (e: IOException) {
            Log.e(TAG, "Error reading .ovpn file from assets: $filename", e)
            null
        }
    }

    /**
     * Parse .ovpn file content and create VpnProfile
     */
    fun parseOvpnConfig(configContent: String, profileName: String = "VPN Profile"): VpnProfile? {
        return try {
            val configParser = ConfigParser()
            configParser.parseConfig(java.io.StringReader(configContent))
            val profile = configParser.convertProfile()
            profile.mName = profileName
            
            // Fix: Set server name, port, and protocol from first connection
            // ConfigParser sets these in mConnections but not in VpnProfile directly
            if (profile.mConnections.isNotEmpty() && profile.mConnections[0] != null) {
                val firstConn = profile.mConnections[0]
                if (firstConn.mServerName.isNotEmpty() && firstConn.mServerName != "openvpn.example.com") {
                    profile.mServerName = firstConn.mServerName
                }
                if (firstConn.mServerPort.isNotEmpty()) {
                    profile.mServerPort = firstConn.mServerPort
                }
                profile.mUseUdp = firstConn.mUseUdp
                
                // CRITICAL: Ensure connection is enabled (required for checkProfile to pass)
                firstConn.mEnabled = true
                
                Log.d(TAG, "Set profile server from connection: ${profile.mServerName}:${profile.mServerPort}, UDP=${profile.mUseUdp}")
            }
            
            // Ensure at least one connection exists and is enabled
            if (profile.mConnections.isEmpty()) {
                Log.w(TAG, "No connections found, creating default connection")
                profile.mConnections = arrayOf(de.blinkt.openvpn.core.Connection().apply {
                    mEnabled = true
                    mServerName = profile.mServerName
                    mServerPort = profile.mServerPort
                    mUseUdp = profile.mUseUdp
                })
            } else {
                // Ensure at least one connection is enabled
                val hasEnabledConnection = profile.mConnections.any { it.mEnabled }
                if (!hasEnabledConnection) {
                    Log.w(TAG, "No enabled connections found, enabling first connection")
                    profile.mConnections[0].mEnabled = true
                }
            }
            
            // Ensure mUsePull is true (default) for server-pushed config
            if (!profile.mUsePull) {
                Log.d(TAG, "Setting mUsePull to true for server-pushed config")
                profile.mUsePull = true
            }
            
            // Ensure mUseDefaultRoute is true (default) unless explicitly set
            if (!profile.mUseDefaultRoute && profile.mAllowedAppsVpn.isEmpty()) {
                Log.d(TAG, "Setting mUseDefaultRoute to true")
                profile.mUseDefaultRoute = true
            }
            
            // Log certificate information for debugging
            Log.d(TAG, "Successfully parsed .ovpn config, profile: ${profile.mName}")
            Log.d(TAG, "Profile certificates: " +
                    "CA=${if (profile.mCaFilename != null) "present" else "missing"}, " +
                    "ClientCert=${if (profile.mClientCertFilename != null) "present" else "missing"}, " +
                    "ClientKey=${if (profile.mClientKeyFilename != null) "present" else "missing"}, " +
                    "AuthType=${profile.mAuthenticationType}, " +
                    "UseTLSAuth=${profile.mUseTLSAuth}, " +
                    "TLSAuthFilename=${if (profile.mTLSAuthFilename != null) "present" else "missing"}")
            Log.d(TAG, "Profile connection: " +
                    "server=${profile.mServerName}, " +
                    "port=${profile.mServerPort}, " +
                    "useUdp=${profile.mUseUdp}, " +
                    "connections=${profile.mConnections.size}, " +
                    "usePull=${profile.mUsePull}, " +
                    "useDefaultRoute=${profile.mUseDefaultRoute}")
            
            // Verify certificates are present
            if (profile.mCaFilename == null) {
                Log.w(TAG, "Warning: CA certificate is missing in parsed profile")
            }
            if (profile.mClientCertFilename == null && profile.mAuthenticationType == VpnProfile.TYPE_CERTIFICATES) {
                Log.w(TAG, "Warning: Client certificate is missing but auth type is CERTIFICATES")
            }
            if (profile.mClientKeyFilename == null && profile.mAuthenticationType == VpnProfile.TYPE_CERTIFICATES) {
                Log.w(TAG, "Warning: Client key is missing but auth type is CERTIFICATES")
            }
            
            // If TLS auth is enabled but no TLS auth key is provided, disable it
            // (Some .ovpn files might have tls-auth in config but not the key)
            if (profile.mUseTLSAuth && profile.mTLSAuthFilename.isNullOrEmpty()) {
                Log.w(TAG, "TLS auth is enabled but no key provided, disabling TLS auth")
                profile.mUseTLSAuth = false
            }
            
            // CRITICAL FIX: If cipher is set but data-ciphers is empty, and cipher is not a default GCM/CHACHA20 cipher,
            // add it to data-ciphers to allow cipher negotiation with servers that require legacy ciphers like AES-128-CBC
            if (!profile.mCipher.isNullOrEmpty() && profile.mDataCiphers.isNullOrEmpty()) {
                val cipherUpper = profile.mCipher.uppercase()
                // Check if cipher is not one of the default modern ciphers
                val isDefaultCipher = cipherUpper == "AES-256-GCM" || 
                                     cipherUpper == "AES-128-GCM" || 
                                     cipherUpper == "CHACHA20-POLY1305"
                
                if (!isDefaultCipher) {
                    // Add cipher to data-ciphers with DEFAULT prefix to include default ciphers + this cipher
                    profile.mDataCiphers = "DEFAULT:${profile.mCipher}"
                    Log.d(TAG, "Added cipher '${profile.mCipher}' to data-ciphers: ${profile.mDataCiphers}")
                }
            }
            
            // Log cipher configuration
            Log.d(TAG, "Profile cipher config: cipher=${profile.mCipher}, dataCiphers=${profile.mDataCiphers}")
            
            profile
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing .ovpn config", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Read and parse .ovpn file from assets in one step
     */
    fun readAndParseOvpn(context: Context, filename: String, profileName: String = "VPN Profile"): VpnProfile? {
        val configContent = readOvpnFromAssets(context, filename) ?: return null
        return parseOvpnConfig(configContent, profileName)
    }
}


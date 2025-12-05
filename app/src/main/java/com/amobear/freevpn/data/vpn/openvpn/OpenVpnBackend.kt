package com.amobear.freevpn.data.vpn.openvpn

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.amobear.freevpn.data.vpn.ConnectionParamsUuidServiceHelper
import com.amobear.freevpn.data.vpn.ErrorType
import com.amobear.freevpn.data.vpn.VpnBackend
import com.amobear.freevpn.data.vpn.VpnState
import com.amobear.freevpn.data.vpn.models.ConnectionParams
import com.amobear.freevpn.data.vpn.models.ConnectionParamsOpenVpn
import com.amobear.freevpn.data.vpn.usecases.SettingsForConnection
import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.models.config.TransmissionProtocol
import com.amobear.freevpn.models.config.VpnProtocol
import com.amobear.freevpn.utils.OvpnFileReader
import com.amobear.freevpn.utils.Storage
import dagger.hilt.android.qualifiers.ApplicationContext
import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.LogItem
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenVpnBackend @Inject constructor(
    @ApplicationContext private val appContext: Context,
    settingsForConnection: SettingsForConnection,
    mainScope: CoroutineScope,
) : VpnBackend(
    settingsForConnection,
    VpnProtocol.OpenVPN,
    mainScope,
), VpnStatus.StateListener {

    init {
        VpnStatus.addStateListener(this)
        VpnStatus.addLogListener(this::vpnLog)
        Log.d(TAG, "OpenVpnBackend initialized and registered as VpnStatus.StateListener")
        
        // Log current state listeners count for debugging
        android.util.Log.d(TAG, "Current VpnStatus state listeners registered")
    }

    override suspend fun connect(connectionParams: ConnectionParams) {
        lastConnectionParams = connectionParams
        Log.d(TAG, "Connecting with params: ${connectionParams.info}")
        
        // Set initial connecting state
        vpnProtocolState = VpnState.Connecting
        Log.d(TAG, "Set initial state to Connecting, current selfStateFlow value: ${selfStateFlow.value}")
        
        val server = connectionParams.server
        
        // Check if server has ovpnConfig (from VPN Gate API)
        // If yes, use it directly instead of creating profile from ConnectionParams
        val profile = if (!server.ovpnConfig.isNullOrEmpty()) {
            Log.d(TAG, "Server has ovpnConfig, parsing it directly")
            // Parse ovpnConfig string directly into VpnProfile
            val parsedProfile = OvpnFileReader.parseOvpnConfig(server.ovpnConfig, server.name)
                ?: throw Exception("Failed to parse ovpnConfig from server")
            
            Log.d(TAG, "Parsed profile from ovpnConfig: ${parsedProfile.mName}, UUID: ${parsedProfile.getUUIDString()}")
            parsedProfile
        } else {
            // No ovpnConfig, create profile from ConnectionParams (for manual servers)
            Log.d(TAG, "No ovpnConfig, creating profile from ConnectionParams")
            
            if (connectionParams !is ConnectionParamsOpenVpn) {
                throw Exception("ConnectionParams must be ConnectionParamsOpenVpn for OpenVPN")
            }
            
            // Create VpnProfile from connection params
            val settings = SettingsForConnection.getForSync(connectionParams.connectIntent)
            val createdProfile = connectionParams.openVpnProfile(appContext.packageName, settings, null, com.amobear.freevpn.data.vpn.usecases.ComputeAllowedIPs())
            
            // Store connection params for our use (only when not using ovpnConfig)
            ConnectionParams.store(connectionParams)
            Log.d(TAG, "Stored connection params: ${connectionParams.info}")
            
            Log.d(TAG, "Created profile from ConnectionParams: ${createdProfile.mName}, UUID: ${createdProfile.getUUIDString()}")
            createdProfile
        }
        
        // Validate profile (like LaunchVPN.launchVPN())
        val profileError = profile.checkProfile(appContext)
        if (profileError != de.blinkt.openvpn.R.string.no_error_found) {
            val errorMessage = appContext.getString(profileError)
            Log.e(TAG, "Profile validation failed: $errorMessage")
            VpnStatus.logError(profileError)
            throw Exception("Profile validation failed: $errorMessage")
        }
        
        // Save profile to ProfileManager (required for OpenVPNService)
        val profileManager = ProfileManager.getInstance(appContext)
        profileManager.addProfile(profile)
        profileManager.saveProfileList(appContext)
        ProfileManager.saveProfile(appContext, profile)
        
        // Verify profile was saved correctly
        val profileUuid = profile.getUUIDString()
        val savedProfile = ProfileManager.get(appContext, profileUuid)
        if (savedProfile == null) {
            Log.e(TAG, "Failed to retrieve saved profile with UUID: $profileUuid")
            throw Exception("Failed to save profile to ProfileManager")
        }
        Log.d(TAG, "Profile verified in ProfileManager: ${savedProfile.mName}")
        
        // Update LRU (like LaunchVPN does)
        ProfileManager.updateLRU(appContext, savedProfile)
        
        // Check if password is needed (like LaunchVPN checks needUserPWInput)
        val needPassword = savedProfile.needUserPWInput(null, null)
        if (needPassword != 0) {
            val passwordType = appContext.getString(needPassword)
            Log.w(TAG, "Profile requires password input: $passwordType")
            // For backend layer, we'll let the service handle password prompts
            VpnStatus.updateStateString(
                "USER_VPN_PASSWORD",
                "",
                de.blinkt.openvpn.R.string.state_user_vpn_password,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT
            )
        }
        
        // Log profile details for debugging
        Log.d(TAG, "Profile details: name=${savedProfile.mName}, " +
                "server=${savedProfile.mServerName}, port=${savedProfile.mServerPort}, " +
                "authType=${savedProfile.mAuthenticationType}, " +
                "connections=${savedProfile.mConnections.size}, " +
                "useUdp=${savedProfile.mUseUdp}, " +
                "CA=${if (savedProfile.mCaFilename != null) "present" else "missing"}, " +
                "ClientCert=${if (savedProfile.mClientCertFilename != null) "present" else "missing"}, " +
                "ClientKey=${if (savedProfile.mClientKeyFilename != null) "present" else "missing"}")
        
        // Check VPN permission first (like LaunchVPN does)
        val vpnPermissionIntent = android.net.VpnService.prepare(appContext)
        if (vpnPermissionIntent != null) {
            // Need VPN permission - launch LaunchVPN activity to request it
            VpnStatus.updateStateString(
                "USER_VPN_PERMISSION",
                "",
                de.blinkt.openvpn.R.string.state_user_vpn_permission,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT
            )
            
            val launchIntent = Intent(appContext, LaunchVPN::class.java).apply {
                action = Intent.ACTION_MAIN
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(LaunchVPN.EXTRA_KEY, profileUuid)
                putExtra(OpenVPNService.EXTRA_START_REASON, "Connect from OpenVpnBackend")
                putExtra(LaunchVPN.EXTRA_HIDELOG, true)
            }
            // Only add connection params UUID if we have connection params (not using ovpnConfig)
            if (server.ovpnConfig.isNullOrEmpty()) {
                ConnectionParamsUuidServiceHelper.addConnectionParamsUuid(launchIntent, connectionParams.uuid)
            }
            
            try {
                appContext.startActivity(launchIntent)
                Log.d(TAG, "Launched LaunchVPN activity to request VPN permission")
                // Return - permission dialog will handle the rest
                return
            } catch (e: android.content.ActivityNotFoundException) {
                // Handle case where LaunchVPN activity is not found (like LaunchVPN does)
                Log.e(TAG, "LaunchVPN activity not found", e)
                VpnStatus.logError(de.blinkt.openvpn.R.string.no_vpn_support_image)
                throw Exception("VPN permission activity not available")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch LaunchVPN activity", e)
                throw e
            }
        }
        
        // VPN permission already granted - use VPNLaunchHelper.startOpenVpn() pattern (like LaunchVPN does)
        // But override service class to use our wrapper service
        val startReason = "Connect from OpenVpnBackend"
        val serviceIntent = savedProfile.getStartServiceIntent(appContext, startReason, true)
        if (serviceIntent != null) {
            // Override service class to use our wrapper service
            serviceIntent.setClass(appContext, com.amobear.freevpn.data.vpn.openvpn.OpenVPNWrapperService::class.java)
            // Only add connection params UUID if we have connection params (not using ovpnConfig)
            if (server.ovpnConfig.isNullOrEmpty()) {
                ConnectionParamsUuidServiceHelper.addConnectionParamsUuid(serviceIntent, connectionParams.uuid)
            }
            
            try {
                // Verify profile UUID is in intent
                val intentProfileUuid = serviceIntent.getStringExtra(de.blinkt.openvpn.VpnProfile.EXTRA_PROFILEUUID)
                Log.i(TAG, "=== VERIFYING INTENT ===")
                Log.i(TAG, "Expected profile UUID: $profileUuid")
                Log.i(TAG, "Intent profile UUID: $intentProfileUuid")
                Log.i(TAG, "Intent extras keys: ${serviceIntent.extras?.keySet()}")
                
                if (intentProfileUuid != profileUuid) {
                    Log.e(TAG, "PROFILE UUID MISMATCH! Expected: $profileUuid, Got: $intentProfileUuid")
                    // Manually add profile UUID to intent
                    serviceIntent.putExtra(de.blinkt.openvpn.VpnProfile.EXTRA_PROFILEUUID, profileUuid)
                    Log.i(TAG, "Manually added profile UUID to intent")
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(serviceIntent)
                } else {
                    appContext.startService(serviceIntent)
                }
                Log.i(TAG, "=== VPN SERVICE STARTED === profile UUID: $profileUuid")
                Log.i(TAG, "Service intent: action=${serviceIntent.action}, extras=${serviceIntent.extras?.keySet()}")
                Log.i(TAG, "Waiting for VpnStatus updates... (check for '=== updateState CALLED ===' logs)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VPN service", e)
                throw e
            }
        } else {
            Log.e(TAG, "getStartServiceIntent returned null")
            throw Exception("Failed to get service intent from profile")
        }
    }

    override suspend fun closeVpnTunnel(withStateChange: Boolean) {
        Log.d(TAG, "Closing VPN tunnel")
        val intent = Intent(appContext, com.amobear.freevpn.data.vpn.openvpn.OpenVPNWrapperService::class.java)
        intent.action = OpenVPNService.PAUSE_VPN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
        waitForDisconnect()
    }


    override fun updateState(
        openVpnState: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus?,
        intent: Intent?
    ) {
        // ALWAYS log state updates to debug
        Log.i(TAG, "=== updateState CALLED === state='$openVpnState', level=$level, message='$logmessage', localizedResId=$localizedResId")
        
        // Log all state updates, especially errors and NOPROCESS
        when (level) {
            ConnectionStatus.LEVEL_NOTCONNECTED,
            ConnectionStatus.UNKNOWN_LEVEL -> {
                Log.w(TAG, "updateState: state=$openVpnState, level=$level, message=$logmessage")
            }
            else -> {
                Log.d(TAG, "updateState: state=$openVpnState, level=$level, message=$logmessage")
            }
        }
        
        if (level == ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET &&
            (vpnProtocolState as? VpnState.Error)?.type == ErrorType.PEER_AUTH_FAILED) {
            return
        }

        val translatedState = when {
            openVpnState == "CONNECTRETRY" && level == ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET ->
                VpnState.WaitingForNetwork
            openVpnState == "RECONNECTING" ->
                if (logmessage?.startsWith("tls-error") == true)
                    VpnState.Error(ErrorType.PEER_AUTH_FAILED, isFinal = false)
                else
                    VpnState.Reconnecting
            openVpnState == "NOPROCESS" -> {
                Log.e(TAG, "OpenVPN process stopped unexpectedly. Message: $logmessage")
                // Get recent logs to see what went wrong
                val recentLogs = VpnStatus.getlogbuffer().takeLast(10)
                Log.e(TAG, "Recent VPN logs before exit:")
                recentLogs.forEach { logItem ->
                    Log.e(TAG, "  - ${logItem.getString(appContext)}")
                }
                VpnState.Disabled
            }
            else -> when (level) {
                ConnectionStatus.LEVEL_CONNECTED -> VpnState.Connected
                ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
                ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
                ConnectionStatus.LEVEL_START,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> VpnState.Connecting
                ConnectionStatus.LEVEL_NONETWORK -> VpnState.WaitingForNetwork
                ConnectionStatus.LEVEL_NOTCONNECTED,
                ConnectionStatus.LEVEL_VPNPAUSED -> VpnState.Disabled
                ConnectionStatus.LEVEL_AUTH_FAILED -> VpnState.Error(ErrorType.AUTH_FAILED_INTERNAL, isFinal = false)
                ConnectionStatus.UNKNOWN_LEVEL -> VpnState.Error(ErrorType.GENERIC_ERROR, isFinal = true)
                ConnectionStatus.LEVEL_MULTI_USER_PERMISSION -> VpnState.Error(ErrorType.MULTI_USER_PERMISSION, isFinal = true)
                null -> VpnState.Disabled
            }
        }
        
        val oldState = vpnProtocolState
        vpnProtocolState = translatedState
        Log.d(TAG, "State updated: $oldState -> $translatedState (selfStateFlow.value=${selfStateFlow.value})")
    }

    override fun setConnectedVPN(uuid: String) {
        Log.d(TAG, "setConnectedVPN: $uuid")
    }

    private fun vpnLog(item: LogItem) {
        val logMessage = item.getString(appContext)
        val logLevel = item.mLevel
        
        // Log all messages, especially errors and warnings
        when (logLevel) {
            VpnStatus.LogLevel.ERROR -> Log.e(TAG, "VPN Error: $logMessage")
            VpnStatus.LogLevel.WARNING -> Log.w(TAG, "VPN Warning: $logMessage")
            VpnStatus.LogLevel.VERBOSE -> Log.v(TAG, "VPN Verbose: $logMessage")
            else -> Log.d(TAG, "VPN Log: $logMessage")
        }
    }

    companion object {
        private const val TAG = "OpenVpnBackend"
        private const val PRIMARY_PORT = 443
    }
}


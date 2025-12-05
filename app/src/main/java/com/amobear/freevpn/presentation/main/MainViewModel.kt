package com.amobear.freevpn.presentation.main

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amobear.freevpn.domain.model.ConnectionState
import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.domain.model.SignalServerResponse
import com.amobear.freevpn.domain.model.TrafficStats
import com.amobear.freevpn.domain.model.VpnConnection
import com.amobear.freevpn.domain.model.SignalServer
import com.amobear.freevpn.domain.usecase.ConnectSignalVpnUseCase
import com.amobear.freevpn.domain.usecase.ConnectVpnUseCase
import com.amobear.freevpn.domain.usecase.DisconnectVpnUseCase
import com.amobear.freevpn.domain.usecase.FetchSignalServersUseCase
import com.amobear.freevpn.domain.usecase.GetServersUseCase
import com.amobear.freevpn.domain.usecase.InitializeDataUseCase
import com.amobear.freevpn.domain.usecase.MonitorTrafficUseCase
import com.amobear.freevpn.domain.usecase.ObserveConnectionUseCase
import com.amobear.freevpn.domain.usecase.PauseVpnUseCase
import com.amobear.freevpn.domain.usecase.PingServerUseCase
import com.amobear.freevpn.domain.usecase.ResumeVpnUseCase
import com.amobear.freevpn.domain.usecase.SyncServersUseCase
import com.amobear.freevpn.util.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * MainViewModel following Clean Architecture principles
 *
 * ViewModel only depends on Use Cases (Domain Layer)
 * No direct dependencies on Repository or Data Layer
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val connectVpnUseCase: ConnectVpnUseCase,
    private val disconnectVpnUseCase: DisconnectVpnUseCase,
    private val pauseVpnUseCase: PauseVpnUseCase,
    private val resumeVpnUseCase: ResumeVpnUseCase,
    private val observeConnectionUseCase: ObserveConnectionUseCase,
    private val monitorTrafficUseCase: MonitorTrafficUseCase,
    private val getServersUseCase: GetServersUseCase,
    private val pingServerUseCase: PingServerUseCase,
    private val syncServersUseCase: SyncServersUseCase,
    private val initializeDataUseCase: InitializeDataUseCase,
    private val fetchSignalServersUseCase: FetchSignalServersUseCase,
    private val connectSignalVpnUseCase: ConnectSignalVpnUseCase,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var lastConnectedAt: Long = 0L

    init {
        viewModelScope.launch {
            initializeDataUseCase()
        }

        observeConnection()

        observeTraffic()
        
        // Observe Signal VPN connection state
        observeSignalVpnConnection()

        // Load servers
        loadServers()
    }
    
    /**
     * Observe Signal VPN connection state
     */
    private fun observeSignalVpnConnection() {
        viewModelScope.launch {
            connectSignalVpnUseCase.observeConnectionState().collect { state ->
                _uiState.update {
                    it.copy(
                        signalVpnConnectionState = state,
                        isSignalConnecting = state is ConnectSignalVpnUseCase.ConnectionState.Connecting
                    )
                }
            }
        }
    }

    /**
     * Connect to VPN server
     */
    fun connect(serverId: String) {
        val context = applicationContext

        if (!PermissionManager.hasAllPermissions(context)) {
            _uiState.update {
                it.copy(
                    error = "Required permissions not granted",
                    showPermissionRequest = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConnecting = true,
                    error = null,
                    showPermissionRequest = false
                )
            }

            connectVpnUseCase(serverId)
                .onSuccess {
                    _uiState.update { it.copy(isConnecting = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun pause() {
        viewModelScope.launch {
            pauseVpnUseCase()
        }
    }

    fun resume() {
        viewModelScope.launch {
            resumeVpnUseCase()
        }
    }

    /**
     * Handle permissions granted
     */
    fun onPermissionsGranted() {
        _uiState.update { it.copy(showPermissionRequest = false) }
    }

    /**
     * Handle permissions denied
     */
    fun onPermissionsDenied() {
        _uiState.update {
            it.copy(
                showPermissionRequest = false,
                error = "Permissions required to connect to VPN"
            )
        }
    }

    /**
     * Disconnect from VPN
     */
    fun disconnect() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConnecting = true,
                    error = null
                )
            }

            disconnectVpnUseCase()
                .onSuccess {
                    _uiState.update { it.copy(isConnecting = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    /**
     * Ping server to test latency
     */
    fun pingServer(serverId: String) {
        viewModelScope.launch {
            try {
                val latency = pingServerUseCase(serverId)
                // Update server latency in UI
                _uiState.update { state ->
                    val updatedServers = state.servers.map { server ->
                        if (server.id == serverId) {
                            server.copy(latency = latency)
                        } else {
                            server
                        }
                    }
                    state.copy(servers = updatedServers)
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    /**
     * Select a server
     */
    fun selectServer(server: Server) {
        _uiState.update { it.copy(selectedServer = server) }
    }

    /**
     * Sync servers from VPN API
     * @param forceRefresh Force refresh even if servers exist locally
     */
    fun syncServersFromApi(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    error = null
                )
            }

            val result = syncServersUseCase(forceRefresh)

            result.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncMessage = "Synced $count servers from API"
                    )
                }
                // Reload servers after sync
                loadServers()
                // Clear sync message after 3 seconds
                delay(3000)
                _uiState.update { it.copy(syncMessage = null) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = "Failed to sync servers: ${error.message}",
                        syncMessage = null
                    )
                }
            }
        }
    }

    /**
     * Refresh servers list from API (force refresh)
     */
    fun refreshServers() {
        syncServersFromApi(forceRefresh = true)
    }

    /**
     * Fetch ad-hoc Signal servers (no DB persistence, no params required)
     */
    fun fetchSignalServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSignalLoading = true, signalError = null) }
            fetchSignalServersUseCase.fetchWithCredentials()
                .onSuccess { fetchResult ->
                    // Initialize Signal VPN with server list and credentials
                    connectSignalVpnUseCase.initialize(
                        fetchResult.serverResponse,
                        fetchResult.authId,
                        fetchResult.authToken
                    )
                    
                    _uiState.update {
                        it.copy(
                            signalResponse = fetchResult.serverResponse,
                            isSignalLoading = false,
                            signalError = null,
                            showSignalList = true
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSignalLoading = false,
                            signalError = error.message ?: "Fetch Signal servers failed"
                        )
                    }
                }
        }
    }
    
    /**
     * Connect to Signal VPN server
     */
    fun connectSignalVpn(server: SignalServer) {
        viewModelScope.launch {
            // Check VPN permission first
            val permissionIntent = connectSignalVpnUseCase.checkVpnPermission()
            if (permissionIntent != null) {
                _uiState.update {
                    it.copy(
                        signalVpnPermissionIntent = permissionIntent,
                        showSignalVpnPermissionRequest = true
                    )
                }
                return@launch
            }
            
            _uiState.update {
                it.copy(
                    isSignalConnecting = true,
                    signalError = null,
                    selectedSignalServer = server
                )
            }
            
            // Connect to Signal VPN
            connectSignalVpnUseCase.connect(server)
        }
    }
    
    /**
     * Disconnect from Signal VPN
     */
    fun disconnectSignalVpn() {
        viewModelScope.launch {
            connectSignalVpnUseCase.disconnect()
            _uiState.update {
                it.copy(
                    isSignalConnecting = false,
                    selectedSignalServer = null
                )
            }
        }
    }
    
    /**
     * Handle Signal VPN permission granted
     */
    fun onSignalVpnPermissionGranted() {
        _uiState.update { it.copy(showSignalVpnPermissionRequest = false) }
        // Retry connection after permission granted
        _uiState.value.selectedSignalServer?.let { server ->
            connectSignalVpn(server)
        }
    }
    
    /**
     * Handle Signal VPN permission denied
     */
    fun onSignalVpnPermissionDenied() {
        _uiState.update {
            it.copy(
                showSignalVpnPermissionRequest = false,
                signalError = "VPN permission required to connect"
            )
        }
    }

    fun showDefaultServers() {
        _uiState.update { it.copy(showSignalList = false) }
    }

    /**
     * Observe connection state
     */
    private fun observeConnection() {
        viewModelScope.launch {
            observeConnectionUseCase().collect { connection ->
                if (connection.state == ConnectionState.CONNECTED && lastConnectedAt == 0L) {
                    lastConnectedAt = System.currentTimeMillis()
                }
                if (connection.state == ConnectionState.DISCONNECTED && lastConnectedAt > 0L) {
                    val duration = System.currentTimeMillis() - lastConnectedAt
                    lastConnectedAt = 0L
                    val stats = _uiState.value.trafficStats
                    _uiState.update {
                        it.copy(
                            sessionSummary = SessionSummary(
                                duration = duration,
                                uploadBytes = stats.uploadBytes,
                                downloadBytes = stats.downloadBytes
                            ),
                            showDisconnectDialog = true
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        connection = connection,
                        isConnecting = connection.state == ConnectionState.CONNECTING
                    )
                }
            }
        }
    }

    /**
     * Observe traffic stats
     */
    private fun observeTraffic() {
        viewModelScope.launch {
            monitorTrafficUseCase().collect { stats ->
                _uiState.update { it.copy(trafficStats = stats) }
            }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDisconnectDialog = false, sessionSummary = null) }
    }

    /**
     * Load servers from repository
     */
    private fun loadServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                getServersUseCase().collect { servers ->
                    _uiState.update {
                        it.copy(
                            servers = servers,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
}

/**
 * UI State for MainScreen
 */
data class MainUiState(
    val connection: VpnConnection? = null,
    val trafficStats: TrafficStats = TrafficStats(0, 0, 0.0, 0.0),
    val servers: List<Server> = emptyList(),
    val selectedServer: Server? = null,
    val isLoading: Boolean = false,
    val isConnecting: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val error: String? = null,
    val showPermissionRequest: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val sessionSummary: SessionSummary? = null,
    val signalResponse: SignalServerResponse? = null,
    val isSignalLoading: Boolean = false,
    val signalError: String? = null,
    val showSignalList: Boolean = false,
    val selectedSignalServer: SignalServer? = null,
    val isSignalConnecting: Boolean = false,
    val signalVpnConnectionState: ConnectSignalVpnUseCase.ConnectionState = ConnectSignalVpnUseCase.ConnectionState.Idle,
    val showSignalVpnPermissionRequest: Boolean = false,
    val signalVpnPermissionIntent: android.content.Intent? = null
)

data class SessionSummary(
    val duration: Long,
    val uploadBytes: Long,
    val downloadBytes: Long
)

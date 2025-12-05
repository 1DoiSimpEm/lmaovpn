package com.amobear.freevpn.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amobear.freevpn.domain.model.ConnectionState
import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.presentation.util.PermissionHandler
import com.amobear.freevpn.util.PermissionManager
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.ui.platform.LocalContext
import com.amobear.freevpn.domain.model.SignalServer
import com.amobear.freevpn.domain.usecase.ConnectSignalVpnUseCase
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onRequestPermissions: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.showPermissionRequest) {
        onRequestPermissions?.invoke()
        PermissionHandler(
            permissions = PermissionManager.getRuntimePermissions(),
            onPermissionsGranted = { viewModel.onPermissionsGranted() },
            onPermissionsDenied = { viewModel.onPermissionsDenied() }
        )
    }
    
    // Handle Signal VPN permission request
    if (uiState.showSignalVpnPermissionRequest && uiState.signalVpnPermissionIntent != null) {
        LaunchedEffect(uiState.signalVpnPermissionIntent) {
            try {
                context.startActivity(uiState.signalVpnPermissionIntent)
                viewModel.onSignalVpnPermissionGranted()
            } catch (e: Exception) {
                viewModel.onSignalVpnPermissionDenied()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Free VPN") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshServers() },
                        enabled = !uiState.isSyncing && !uiState.isLoading
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh servers from API",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ConnectionStatusCard(
                connection = uiState.connection,
                trafficStats = uiState.trafficStats,
                onDisconnect = { viewModel.disconnect() },
                onPause = { viewModel.pause() },
                onResume = { viewModel.resume() },
                onCancel = { viewModel.disconnect() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            uiState.syncMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (uiState.connection?.state == ConnectionState.CONNECTED ||
                uiState.connection?.state == ConnectionState.CONNECTING) {
                TrafficStatsView(uiState.trafficStats, uiState.connection)
            } else {
                if (uiState.showSignalList) {
                    SignalServerListSection(
                        signalResponse = uiState.signalResponse,
                        isLoading = uiState.isSignalLoading,
                        error = uiState.signalError,
                        isConnecting = uiState.isSignalConnecting,
                        connectionState = uiState.signalVpnConnectionState,
                        selectedServer = uiState.selectedSignalServer,
                        onBackToDefault = { viewModel.showDefaultServers() },
                        onConnect = { viewModel.connectSignalVpn(it) },
                        onDisconnect = { viewModel.disconnectSignalVpn() }
                    )
                } else {
                    ServerListSection(
                        servers = uiState.servers,
                        selectedServer = uiState.selectedServer,
                        isLoading = uiState.isLoading || uiState.isSyncing,
                        onServerSelect = { viewModel.selectServer(it) },
                        onConnect = { viewModel.connect(it.id) },
                        onPing = { viewModel.pingServer(it.id) },
                        onConnectFromOvpn = {  },
                        onFetchSignal = { viewModel.fetchSignalServers() },
                        isSignalLoading = uiState.isSignalLoading,
                        signalError = uiState.signalError
                    )
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (uiState.showDisconnectDialog && uiState.sessionSummary != null) {
            val summary = uiState.sessionSummary
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text("Close")
                    }
                },
                title = { Text("VPN Session Summary") },
                text = {
                    Column {
                        Text("Duration: ${formatDuration(summary?.duration ?: 0L)}")
                        Text(String.format(Locale.US, "Upload: %.2f MB", summary?.uploadBytes?.div(
                            1024.0
                        )?.div(1024.0)
                        ))
                        Text(String.format(Locale.US, "Download: %.2f MB", summary?.downloadBytes?.div(
                            1024.0
                        )?.div(1024.0)
                        ))
                    }
                }
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connection: com.amobear.freevpn.domain.model.VpnConnection?,
    trafficStats: com.amobear.freevpn.domain.model.TrafficStats,
    onDisconnect: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val state = connection?.state ?: ConnectionState.DISCONNECTED
            val color = when (state) {
                ConnectionState.CONNECTED -> Color.Green
                ConnectionState.CONNECTING -> Color.Yellow
                ConnectionState.DISCONNECTED -> Color.Gray
                ConnectionState.AUTH_FAILED -> Color.Red
                else -> Color.Gray
            }

            Icon(
                imageVector = when (state) {
                    ConnectionState.CONNECTED -> Icons.Filled.CheckCircle
                    ConnectionState.CONNECTING -> Icons.Filled.Refresh
                    else -> Icons.Filled.Clear
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (connection != null && state == ConnectionState.CONNECTED) {
                Text(
                    text = connection.serverName,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Duration: ${formatDuration(connection.duration)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume")
                    }
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disconnect")
                    }
                }
            } else if (state == ConnectionState.CONNECTING) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hủy kết nối")
                }
            }
        }
    }
}

@Composable
fun TrafficStatsView(
    trafficStats: com.amobear.freevpn.domain.model.TrafficStats,
    connection: com.amobear.freevpn.domain.model.VpnConnection?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Traffic Statistics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TrafficStatItem(
                icon = Icons.Filled.KeyboardArrowUp,
                label = "Upload",
                value = String.format(Locale.US, "%.2f MB", trafficStats.uploadMB()),
                speed = String.format(Locale.US, "%.2f Mbps", trafficStats.uploadSpeedMbps())
            )

            TrafficStatItem(
                icon = Icons.Filled.KeyboardArrowDown,
                label = "Download",
                value = String.format(Locale.US, "%.2f MB", trafficStats.downloadMB()),
                speed = String.format(Locale.US, "%.2f Mbps", trafficStats.downloadSpeedMbps())
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Total Data: ${String.format(Locale.US, "%.2f MB", trafficStats.totalMB())}",
                    style = MaterialTheme.typography.bodyLarge
                )
                connection?.ipAddress?.let {
                    Text(
                        text = "IP Address: $it",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun TrafficStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    speed: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = speed,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ServerListSection(
    servers: List<Server>,
    selectedServer: Server?,
    isLoading: Boolean,
    onServerSelect: (Server) -> Unit,
    onConnect: (Server) -> Unit,
    onPing: (Server) -> Unit,
    onConnectFromOvpn: () -> Unit,
    onFetchSignal: () -> Unit,
    isSignalLoading: Boolean,
    signalError: String?
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Select Server",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

            // Connect from .ovpn file button
            Button(
                onClick = onConnectFromOvpn,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect from vpngate.ovpn")
            }

            // Quick Connect Button
            Button(
                onClick = {
                    // Connect to local Mac VPN server
                    // Server IP: 192.168.100.241, Port: 1194, Protocol: UDP
                    val testServer = Server(
                        id = "local-mac-server",
                        name = "Local Mac Server",
                        countryCode = "VN",
                        countryName = "Local",
                        host = "192.168.100.241",  // Your Mac's IP
                        port = 1194,  // UDP port
                        protocol = "UDP",  // UDP protocol
                        username = null,  // Certificate-based auth (no username/password needed)
                        password = null,
                        ovpnConfig = null,
                        isPremium = false,
                        latency = 0,
                        speed = 0.0,
                        isFavorite = false
                    )
                    onConnect(testServer)
                },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quick Connect - Local Mac Server")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Button(
            onClick = onFetchSignal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            enabled = !isSignalLoading
        ) {
            if (isSignalLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đang tải server Signal...")
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lấy server Signal")
            }
        }

        signalError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(servers) { server ->
                    ServerItem(
                        server = server,
                        isSelected = server.id == selectedServer?.id,
                        onSelect = { onServerSelect(server) },
                        onPing = { onPing(server) }
                    )
                }
            }

            // Connect button
            selectedServer?.let { server ->
                Button(
                    onClick = { onConnect(server) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect to ${server.name}")
                }
            }
        }
    }
}

@Composable
fun SignalServerListSection(
    signalResponse: com.amobear.freevpn.domain.model.SignalServerResponse?,
    isLoading: Boolean,
    error: String?,
    isConnecting: Boolean,
    connectionState: ConnectSignalVpnUseCase.ConnectionState,
    selectedServer: SignalServer?,
    onBackToDefault: () -> Unit,
    onConnect: (SignalServer) -> Unit,
    onDisconnect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Signal servers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onBackToDefault) {
                Text("Quay lại danh sách chính")
            }
        }

        signalResponse?.config?.let { cfg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("DNS: ${cfg.dnsServers.joinToString().ifBlank { "N/A" }}")
                    Text("UDP ports: ${cfg.udpPorts.joinToString().ifBlank { "N/A" }}")
                    Text("TCP ports: ${cfg.tcpPorts.joinToString().ifBlank { "N/A" }}")
                    cfg.tunMtu?.let { Text("tun_mtu: $it") }
                }
            }
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return
        }

        val servers = signalResponse?.servers.orEmpty()
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Không có server Signal")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(servers) { server ->
                    SignalServerItem(
                        server = server,
                        isSelected = server.ip == selectedServer?.ip,
                        isConnecting = isConnecting && server.ip == selectedServer?.ip,
                        connectionState = if (server.ip == selectedServer?.ip) connectionState else ConnectSignalVpnUseCase.ConnectionState.Idle,
                        onConnect = { onConnect(server) },
                        onDisconnect = onDisconnect
                    )
                }
            }
            
            // Connection status card
            if (connectionState is ConnectSignalVpnUseCase.ConnectionState.Connected ||
                connectionState is ConnectSignalVpnUseCase.ConnectionState.Connecting) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (connectionState) {
                            is ConnectSignalVpnUseCase.ConnectionState.Connected -> 
                                MaterialTheme.colorScheme.primaryContainer
                            is ConnectSignalVpnUseCase.ConnectionState.Connecting -> 
                                MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = when (connectionState) {
                                is ConnectSignalVpnUseCase.ConnectionState.Connected -> 
                                    "Đã kết nối"
                                is ConnectSignalVpnUseCase.ConnectionState.Connecting -> 
                                    "Đang kết nối..."
                                else -> "Trạng thái"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        selectedServer?.let { server ->
                            Text("Server: ${server.country}${if (server.area.isNotBlank()) " - ${server.area}" else ""}")
                            Text("IP: ${server.ip}")
                        }
                        if (connectionState is ConnectSignalVpnUseCase.ConnectionState.Connected) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onDisconnect,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ngắt kết nối")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServerItem(
    server: Server,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPing: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country flag
            Text(
                text = server.countryCode,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = server.countryName,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (server.latency > 0) {
                    Text(
                        text = "Ping: ${server.latency}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            server.latency < 100 -> Color.Green
                            server.latency < 200 -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                }
            }

            IconButton(onClick = onPing) {
                Icon(Icons.Filled.Refresh, contentDescription = "Ping")
            }

            if (server.isFavorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = Color.Yellow
                )
            }
        }
    }
}

@Composable
fun SignalServerItem(
    server: SignalServer,
    isSelected: Boolean,
    isConnecting: Boolean,
    connectionState: ConnectSignalVpnUseCase.ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                connectionState is ConnectSignalVpnUseCase.ConnectionState.Connected -> 
                    MaterialTheme.colorScheme.primaryContainer
                isSelected -> 
                    MaterialTheme.colorScheme.secondaryContainer
                else -> 
                    MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = listOf(server.country, server.area)
                            .filter { it.isNotBlank() }
                            .joinToString(" - ")
                            .ifBlank { server.ip },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("IP: ${server.ip}")
                    if (server.area.isNotBlank()) {
                        Text("Khu vực: ${server.area}")
                    }
                    Text("Load: ${server.load}%")
                    if (server.isVip) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "VIP",
                                tint = Color.Yellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VIP Server", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (server.isRunning) {
                        Text(
                            "✓ Running",
                            color = Color.Green,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            "✗ Not Running",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                when {
                    connectionState is ConnectSignalVpnUseCase.ConnectionState.Connected -> {
                        Button(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ngắt")
                        }
                    }
                    isConnecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    else -> {
                        Button(
                            onClick = onConnect,
                            enabled = server.isRunning
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kết nối")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}


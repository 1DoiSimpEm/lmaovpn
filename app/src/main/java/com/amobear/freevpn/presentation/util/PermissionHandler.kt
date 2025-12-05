package com.amobear.freevpn.presentation.util

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    permissions: List<String>,
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit = {}
) {
    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    if (!permissionState.allPermissionsGranted) {
        PermissionRequestDialog(
            permissions = permissionState.permissions,
            onConfirm = {
                permissionState.launchMultiplePermissionRequest()
            },
            onDismiss = onPermissionsDenied
        )
    }
}

@Composable
fun PermissionRequestDialog(
    permissions: List<Any>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Lock, contentDescription = null)
        },
        title = {
            Text("Permissions Required")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    "This VPN app needs the following permissions to connect:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                permissions.forEach { permission ->
                    val permissionName = when (permission) {
                        is String -> permission.split(".").last()
                        else -> permission.toString()
                    }

                    Text(
                        "• $permissionName",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}


package com.example.quickqr.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.example.quickqr.R

@Composable
fun PermissionDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(R.string.ok),
    dismissText: String = stringResource(R.string.cancel),
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    )
}

@Composable
fun RationaleDialog(
    permissionText: String,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Permission required")
        },
        text = {
            if (isPermanentlyDeclined) {
                Text("The $permissionText permission was permanently denied. You can enable it in the app settings.")
            } else {
                Text("This app needs access to $permissionText to function properly.")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPermanentlyDeclined) {
                        onGoToAppSettingsClick()
                    } else {
                        onOkClick()
                    }
                }
            ) {
                Text(
                    if (isPermanentlyDeclined) "Open Settings" else "OK"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

package com.alexvt.publisher.viewutils

import androidx.compose.material.*
import androidx.compose.runtime.Composable

@Composable
actual fun PopupDialog(
    text: String,
    okButtonText: String,
    cancelButtonText: String,
    onCancelButton: () -> Unit,
    onOkButton: () -> Unit,
) {
    AlertDialog(
        backgroundColor = MaterialTheme.colors.background,
        onDismissRequest = {
            onCancelButton()
        },
        text = {
            Text(text)
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.secondary
                ),
                onClick = {
                    onOkButton()
                }) {
                Text(okButtonText)
            }
        },
        dismissButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                ),
                onClick = {
                    onCancelButton()
                }) {
                Text(cancelButtonText)
            }
        }
    )
}
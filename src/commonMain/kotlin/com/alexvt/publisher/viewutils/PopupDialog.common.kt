package com.alexvt.publisher.viewutils

import androidx.compose.runtime.Composable

@Composable
expect fun PopupDialog(
    text: String,
    okButtonText: String,
    cancelButtonText: String,
    onCancelButton: () -> Unit,
    onOkButton: () -> Unit,
)
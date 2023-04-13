package com.alexvt.publisher.viewutils

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

actual fun Modifier.pointerMoveFilter(
    onEnter: () -> Boolean,
    onExit: () -> Boolean,
    onMove: (Offset) -> Boolean
): Modifier = this

actual fun Modifier.rightClickListener(
    onClick: (Offset) -> Unit
): Modifier = this

actual fun Modifier.cursorForHorizontalResize() = this
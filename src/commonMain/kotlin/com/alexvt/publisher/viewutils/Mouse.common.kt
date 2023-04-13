package com.alexvt.publisher.viewutils

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

expect fun Modifier.pointerMoveFilter(
    onEnter: () -> Boolean = { true },
    onExit: () -> Boolean = { true },
    onMove: (Offset) -> Boolean = { true }
): Modifier

expect fun Modifier.rightClickListener(
    onClick: (Offset) -> Unit
): Modifier

expect fun Modifier.cursorForHorizontalResize(): Modifier

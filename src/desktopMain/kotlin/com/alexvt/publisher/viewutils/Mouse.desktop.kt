package com.alexvt.publisher.viewutils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.pointerMoveFilter
import java.awt.Cursor

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.pointerMoveFilter(
    onEnter: () -> Boolean,
    onExit: () -> Boolean,
    onMove: (Offset) -> Boolean
): Modifier = this.pointerMoveFilter(onEnter = onEnter, onExit = onExit, onMove = onMove)

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.rightClickListener(
    onClick: (Offset) -> Unit,
): Modifier = this.onPointerEvent(PointerEventType.Press) {
    if (it.buttons.isSecondaryPressed) {
        it.changes.first().position.let { onClick(it) }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.cursorForHorizontalResize(): Modifier =
    this.pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

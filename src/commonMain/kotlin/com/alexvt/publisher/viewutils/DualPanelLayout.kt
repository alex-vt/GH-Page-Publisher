package com.alexvt.publisher.viewutils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun DualPanelLayout(
    leftPanelContent: @Composable () -> Unit,
    rightPanelContent: @Composable () -> Unit
) {
    val panelState = remember { PanelState() }
    val animatedSize = if (panelState.splitter.isResizing) {
        if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize
    } else {
        animateDpAsState(
            if (panelState.isExpanded) panelState.expandedSize else panelState.collapsedSize,
            SpringSpec(stiffness = Spring.StiffnessLow)
        ).value
    }
    VerticalSplittable(
        Modifier.fillMaxSize().background(MaterialTheme.colors.surface),
        panelState.splitter,
        onResize = {
            panelState.expandedSize =
                (panelState.expandedSize + it)
                    .coerceAtLeast(panelState.expandedSizeMin)
                    .coerceAtMost(panelState.expandedSizeMax)
        },
    ) {
        ResizablePanel(
            Modifier.width(animatedSize).fillMaxHeight(),
            panelState
        ) {
            leftPanelContent()
        }
        rightPanelContent()
    }
}

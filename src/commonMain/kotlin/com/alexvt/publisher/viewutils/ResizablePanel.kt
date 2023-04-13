package com.alexvt.publisher.viewutils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

class PanelState {
    val collapsedSize = 16.dp
    var expandedSize by mutableStateOf(250.dp)
    val expandedSizeMin = 150.dp
    val expandedSizeMax = 500.dp
    var isExpanded by mutableStateOf(true)
    val splitter = SplitterState()
}

@Composable
fun ResizablePanel(
    modifier: Modifier,
    state: PanelState,
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        if (state.isExpanded) 1f else 0f,
        SpringSpec(stiffness = Spring.StiffnessLow)
    )

    Box(modifier) {
        Box(Modifier.fillMaxSize().padding(end = 16.dp).graphicsLayer(alpha = alpha)) {
            content()
        }

        Box(
            Modifier.width(16.dp).wrapContentHeight().align(Alignment.TopEnd)
        ) {
            Icon(
                if (state.isExpanded) Icons.Default.ArrowBack else Icons.Default.List,
                contentDescription = if (state.isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .requiredWidth(32.dp)
                    .requiredHeight(32.dp)
                    .clickable {
                        state.isExpanded = !state.isExpanded
                    }
                    .padding(top = 8.dp, bottom = 8.dp, end = 4.dp) // icon is 16 by 16
            )
        }
    }
}

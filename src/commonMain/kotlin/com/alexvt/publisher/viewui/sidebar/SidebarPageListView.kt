package com.alexvt.publisher.viewui.sidebar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.alexvt.publisher.viewmodels.MainViewModel
import com.alexvt.publisher.viewui.LinePatternOverlayView
import com.alexvt.publisher.viewutils.VerticalScrollbar

@Composable
fun SidebarPageListView(
    pageList: List<MainViewModel.SidebarPage>,
    isMarkedOutOfSync: Boolean,
    pageClickListener: (MainViewModel.SidebarPage) -> Unit,
    pageLongPressListener: (MainViewModel.SidebarPage) -> Unit
) = Box(Modifier.fillMaxSize()) {
    val scrollState = rememberLazyListState()

    key(pageList.size) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp),
            state = scrollState
        ) {
            items(pageList.size) {
                SidebarPageView(
                    pageList[it], pageClickListener,
                    pageLongPressListener
                )
            }
        }
    }

    VerticalScrollbar(
        Modifier.align(Alignment.CenterEnd),
        scrollState
    )
    if (isMarkedOutOfSync) {
        LinePatternOverlayView(
            lineColor = MaterialTheme.colors.onBackground.toArgb(),
            linePitchDp = 24.dp,
            lineThicknessDp = 1.dp,
            alpha = 0.1f
        )
    }
}

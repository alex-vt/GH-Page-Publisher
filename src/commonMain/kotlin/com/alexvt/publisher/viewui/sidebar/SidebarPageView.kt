package com.alexvt.publisher.viewui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alexvt.publisher.viewmodels.MainViewModel
import com.alexvt.publisher.viewutils.pointerMoveFilter
import com.alexvt.publisher.viewutils.rightClickListener

@Composable
fun SidebarPageView(
    sidebarPage: MainViewModel.SidebarPage,
    pageClickListener: (MainViewModel.SidebarPage) -> Unit,
    pageLongPressListener: (MainViewModel.SidebarPage) -> Unit
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(24.dp)
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    pageLongPressListener(sidebarPage)
                },
                onTap = {
                    pageClickListener(sidebarPage)
                }
            )
        }
        .rightClickListener {
            pageLongPressListener(sidebarPage)
        }
        .background(
            color = if (sidebarPage.isSelected) {
                MaterialTheme.colors.primary
            } else {
                Color.Transparent
            }
        )
        .padding(start = 12.dp)
        .fillMaxWidth()
) {
    val active = remember { mutableStateOf(false) }

    if (sidebarPage.isNew) {
        Text(
            "New",
            fontSize = MaterialTheme.typography.subtitle1.fontSize,
            modifier = Modifier
                .clip(shape = RoundedCornerShape(8.dp))
                .background(MaterialTheme.colors.secondaryVariant)
                .padding(horizontal = 4.dp)
                .align(Alignment.CenterVertically)
        )
        Spacer(Modifier.width(4.dp))
    }

    Text(
        text = sidebarPage.title,
        fontSize = MaterialTheme.typography.subtitle1.fontSize,
        color = when {
            active.value -> {
                MaterialTheme.colors.onSecondary
            }
            sidebarPage.isSelected -> {
                MaterialTheme.colors.onBackground
            }
            else -> {
                MaterialTheme.colors.onSurface
            }
        },
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .padding(horizontal = 2.dp)
            .pointerMoveFilter(
                onEnter = {
                    active.value = true
                    true
                },
                onExit = {
                    active.value = false
                    true
                }
            ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

}

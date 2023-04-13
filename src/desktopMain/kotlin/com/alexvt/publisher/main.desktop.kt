package com.alexvt.publisher

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.singleWindowApplication
import com.alexvt.publisher.viewui.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import moe.tlaster.precompose.PreComposeWindow
import java.awt.Dimension

@ExperimentalFoundationApi
@FlowPreview
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@InternalCoroutinesApi
fun main() = application {
    val dependencies: AppDependencies = remember { AppDependencies::class.create() }
    PreComposeWindow(
        onCloseRequest = ::exitApplication,
        title = "GH Page Publisher",
        state = WindowState(width = 960.dp, height = 720.dp),
        icon = BitmapPainter(useResource("ic_launcher.png", ::loadImageBitmap)),
    ) {
        window.minimumSize = Dimension(400, 700)
        MainView(dependencies, Dispatchers.Default)
    }
}
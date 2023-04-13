package com.alexvt.publisher.viewui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.alexvt.publisher.usecases.ContentHighlight
import com.alexvt.publisher.viewmodels.MainViewModel
import com.alexvt.publisher.viewui.editor.EditorEmptyView
import com.alexvt.publisher.viewui.editor.EditorTitleView
import com.alexvt.publisher.viewui.editor.EditorView
import kotlinx.coroutines.CoroutineDispatcher

@ExperimentalFoundationApi
@Composable
fun PageView(
    page: MainViewModel.ViewedPage,
    isMarkedOutOfSync: Boolean,
    onEditListener: (String) -> Unit,
    markdownHighlighter: (String) -> List<ContentHighlight>,
    backgroundDispatcher: CoroutineDispatcher,
    hasLink: Boolean,
    onLinkClicked: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // title
            EditorTitleView(
                title = page.title,
                isNewLabelVisible = page.isNew,
                hasLink,
                onLinkClicked,
            )
            if (page.isShowingText) {
                // text
                Box(Modifier.fillMaxSize()) {
                    key(page.title, isMarkedOutOfSync) {
                        EditorView(
                            page = page,
                            onEditListener = { newText ->
                                if (newText != page.contentText) {
                                    onEditListener(newText)
                                }
                            },
                            markdownHighlighter = markdownHighlighter,
                            backgroundDispatcher,
                        )
                    }
                }
            } else {
                // placeholder when no text
                EditorEmptyView()
            }
        }
        if (isMarkedOutOfSync) {
            LinePatternOverlayView(
                lineColor = MaterialTheme.colors.onSurface.toArgb(),
                linePitchDp = 24.dp,
                lineThicknessDp = 1.dp,
                alpha = 0.1f
            )
        }
    }
}

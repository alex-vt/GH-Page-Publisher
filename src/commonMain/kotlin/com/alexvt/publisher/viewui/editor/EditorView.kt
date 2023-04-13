package com.alexvt.publisher.viewui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alexvt.publisher.usecases.ContentHighlight
import com.alexvt.publisher.viewmodels.MainViewModel
import com.alexvt.publisher.viewutils.Fonts
import com.alexvt.publisher.viewutils.VerticalScrollbar
import kotlinx.coroutines.*

@ExperimentalFoundationApi
@Composable
fun EditorView(
    page: MainViewModel.ViewedPage,
    onEditListener: (String) -> Unit,
    markdownHighlighter: (String) -> List<ContentHighlight>,
    backgroundDispatcher: CoroutineDispatcher,
    scrollToCursorDelayMillis: Long = 500,
    scrollToCursorOffsetFromBottom: Dp = 60.dp,
) = Box(Modifier.fillMaxSize().background(MaterialTheme.colors.primary)) {

    val scrollState by remember { mutableStateOf(ScrollState(initial = 0)) }
    val coroutineScope = rememberCoroutineScope()
    val editorTextFormatter = remember {
        EditorTextFormatter(coroutineScope + backgroundDispatcher)
    }
    val mutableFieldValue = remember {
        mutableStateOf(
            TextFieldValue(
                annotatedString = editorTextFormatter.applyHighlights(
                    page.contentText, markdownHighlighter(page.contentText)
                ),
            )
        )
    }
    var cursorRect by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
    var editorRect by remember { mutableStateOf(Rect(0f, 0f, 0f, 0f)) }
    var textLayoutResultOrNull by remember { mutableStateOf<TextLayoutResult?>(null) }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var scrollToCursorJob by remember { mutableStateOf<Job?>(null) }
    val localDensity = LocalDensity.current.density

    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = MaterialTheme.colors.secondary,
            backgroundColor = MaterialTheme.colors.secondaryVariant,
        )
    ) {
        BasicTextField(
            value = mutableFieldValue.value,
            onValueChange = { newFieldValue ->
                // if typing got cursor too low to keyboard then lift text up
                scrollToCursorJob?.cancel()
                scrollToCursorJob = coroutineScope.launch {
                    textLayoutResultOrNull?.let { result ->
                        try {
                            cursorRect = result.getCursorRect(mutableFieldValue.value.selection.end)
                        } catch (exception: IllegalArgumentException) {
                            return@launch // todo look into interference with editing
                        }
                    }
                    delay(scrollToCursorDelayMillis)
                    val targetCursorBottomOffset =
                        scrollToCursorOffsetFromBottom.value * localDensity
                    bringIntoViewRequester.bringIntoView(
                        cursorRect.copy(bottom = cursorRect.bottom + targetCursorBottomOffset)
                    )
                }
                // old highlighting is quickly adjusted to new text
                editorTextFormatter.requestUpdateTextAndHighlights(
                    sourceField = newFieldValue,
                    destinationMutableField = mutableFieldValue,
                    highlighter = markdownHighlighter,
                )
                // while new highlighting is calculated, new text is returned right away
                onEditListener(mutableFieldValue.value.text)
            },
            cursorBrush = SolidColor(MaterialTheme.colors.onSecondary),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f), // dimmed to match highlights
                fontSize = MaterialTheme.typography.subtitle1.fontSize,
                fontFamily = Fonts.robotoMono(),
            ),
            onTextLayout = { textLayoutResult ->
                textLayoutResultOrNull = textLayoutResult
            },
            modifier = Modifier
                .verticalScroll(scrollState)
                .matchParentSize()
                .padding(12.dp)
                .padding(bottom = 35.dp)
                .focusRequester(focusRequester)
                .onGloballyPositioned { // scroll posit
                    editorRect = it.boundsInWindow()
                }
                .bringIntoViewRequester(bringIntoViewRequester)
        )
    }
    // hint
    if (mutableFieldValue.value.text.isEmpty()) {
        Text(
            page.hint,
            style = LocalTextStyle.current.copy(
                color = MaterialTheme.colors.onPrimary.copy(alpha = 0.4f),
                fontSize = MaterialTheme.typography.subtitle1.fontSize,
                fontFamily = Fonts.robotoMono(),
            ),
            modifier = Modifier.padding(12.dp)
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    VerticalScrollbar(Modifier.align(Alignment.CenterEnd), scrollState)
}


private class EditorTextFormatter(
    private val coroutineScope: CoroutineScope,
) {
    private var formattingJob: Job? = null

    fun requestUpdateTextAndHighlights(
        sourceField: TextFieldValue,
        destinationMutableField: MutableState<TextFieldValue>,
        formattingDelayMillis: Long = 500,
        highlighter: (String) -> List<ContentHighlight>
    ) {
        transferTextAndHighlights(sourceField, destinationMutableField)
        formattingJob?.cancel()
        formattingJob = coroutineScope.launch {
            delay(formattingDelayMillis)
            destinationMutableField.value = destinationMutableField.value.copy(
                annotatedString = applyHighlights(
                    destinationMutableField.value.annotatedString.text,
                    highlighter(destinationMutableField.value.annotatedString.text)
                ),
                selection = destinationMutableField.value.selection
            )
        }
    }

    private fun transferTextAndHighlights(
        sourceField: TextFieldValue,
        destinationMutableField: MutableState<TextFieldValue>
    ) {
        val spanShift = sourceField.text.length - destinationMutableField.value.text.length
        val cursorPosition = sourceField.selection.end
        destinationMutableField.value = sourceField.copy(
            annotatedString = AnnotatedString(
                text = sourceField.text,
                spanStyles = destinationMutableField.value.annotatedString.spanStyles.map {
                    val start = it.start
                        .increaseIfOver(cursorPosition, spanShift)
                        .coerceAtLeast(0)
                        .coerceAtMost(sourceField.text.length)
                    val end = it.end
                        .increaseIfOver(cursorPosition, spanShift)
                        .coerceAtLeast(start)
                        .coerceAtMost(sourceField.text.length)
                    AnnotatedString.Range(it.item, start, end)
                }
            )
        )
    }

    fun applyHighlights(
        text: String, highlights: List<ContentHighlight>
    ): AnnotatedString {
        val builder = AnnotatedString.Builder()
        builder.append(text)
        highlights.sortedBy { highlight ->
            highlight.position.first
        }.forEach { highlight ->
            builder.addStyle(
                SpanStyle(
                    color = Color(highlight.colorArgbInt),
                    fontStyle = if (highlight.isItalic) {
                        FontStyle.Italic
                    } else {
                        FontStyle.Normal
                    },
                    fontWeight = if (highlight.isBold) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    textDecoration = if (highlight.isUnderlined) {
                        TextDecoration.Underline
                    } else {
                        TextDecoration.None
                    }
                ),
                start = highlight.position.first,
                end = highlight.position.last
            )
        }
        return builder.toAnnotatedString()
    }

    private fun Int.increaseIfOver(threshold: Int, increase: Int): Int =
        if (this >= threshold) this + increase else this
}

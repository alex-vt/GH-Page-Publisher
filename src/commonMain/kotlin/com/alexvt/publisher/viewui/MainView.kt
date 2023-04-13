package com.alexvt.publisher.viewui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexvt.publisher.AppDependencies
import com.alexvt.publisher.viewmodels.MainViewModel
import com.alexvt.publisher.viewui.sidebar.SidebarPageListView
import com.alexvt.publisher.viewutils.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.ui.viewModel

@OptIn(ExperimentalMaterialApi::class)
@ExperimentalFoundationApi
@Composable
fun MainView(
    dependencies: AppDependencies,
    backgroundDispatcher: CoroutineDispatcher,
) {
    val viewModel = viewModel {
        MainViewModel(dependencies.mainViewModelUseCases, backgroundDispatcher)
    }
    val uiState by viewModel.uiStateFlow.collectAsState()

    MaterialTheme(
        colors = with(uiState.settingsProfile.theme.color) {
            Colors(
                primary = Color(background.bright),
                primaryVariant = Color(background.dim),
                secondary = Color(background.accentBright),
                secondaryVariant = Color(background.accent),
                background = Color(background.normal),
                surface = Color(background.dim),
                error = Color(background.normal),
                onPrimary = Color(text.normal),
                onSecondary = Color(text.bright),
                onBackground = Color(text.normal),
                onSurface = Color(text.dim),
                onError = Color(text.error),
                isLight = uiState.isThemeLight,
            )
        },
        typography = with(uiState.settingsProfile.theme.font.size) {
            Typography(
                body1 = TextStyle(fontSize = big.sp),
                body2 = TextStyle(fontSize = normal.sp),
                button = TextStyle(fontSize = button.sp),
                subtitle1 = TextStyle(fontSize = small.sp),
                subtitle2 = TextStyle(fontSize = small.sp),
            )
        }
    ) {
        val drawerCloseCommandFlow = remember {
            MutableSharedFlow<Unit>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }
        val projectListView: @Composable () -> Unit = {
            SidebarPageListView(
                pageList = uiState.sidebarPages,
                isMarkedOutOfSync = uiState.isContentMarkedOutOfSync,
                pageClickListener = { page ->
                    viewModel.clickSidebarPage(page.title)
                    drawerCloseCommandFlow.tryEmit(Unit)
                },
                pageLongPressListener = { page ->
                    viewModel.rightClickSidebarPage(page.title)
                },
            )
        }
        val uriHandler = LocalUriHandler.current
        val projectContentView: @Composable () -> Unit = {
            PageView(
                page = uiState.viewedPage,
                isMarkedOutOfSync = uiState.isContentMarkedOutOfSync,
                onEditListener = { newText ->
                    viewModel.onViewedPageTextUpdated(newText)
                },
                markdownHighlighter = { text ->
                    viewModel.highlightMarkdown(text)
                },
                backgroundDispatcher,
                hasLink = uiState.viewedPage.hasPageLink,
                onLinkClicked = {
                    browseLink(uriHandler, uiState.viewedPage.pageLink)
                },
            )
        }

        val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
        val coroutineScope = rememberCoroutineScope()
        var contentWidthDp by remember { mutableStateOf(0.dp) }
        val localDensity = LocalDensity.current

        BottomSheetScaffold(
            sheetBackgroundColor = MaterialTheme.colors.background,
            scaffoldState = bottomSheetScaffoldState,
            sheetPeekHeight = 60.dp,
            sheetContent = {
                BottomSheetView(
                    isAlternativeProfile = uiState.isAlternativeProfile,
                    isLoading = uiState.busyDialog.isShown,
                    loadingMessage = uiState.busyDialog.message,
                    hasPageLink = uiState.viewedPage.hasPageLink,
                    onPageLinkClick = {
                        copyToClipboard(uiState.viewedPage.pageLink)
                    },
                    settingsProfile = uiState.settingsProfile,
                    errorMessage = uiState.bottomSheetErrorMessage,
                    onPublishClick = {
                        viewModel.clickPublish()
                    },
                    onSettingsProfileUpdate = { settingsProfile ->
                        viewModel.onSettingsProfileUpdated(settingsProfile)
                    },
                    onAlternativeProfileSelection = { isAlternativeProfile ->
                        viewModel.selectSettingsProfile(isAlternativeProfile)
                    },
                    onCloseSettingsClick = {
                        coroutineScope.launch {
                            bottomSheetScaffoldState.bottomSheetState.collapse()
                        }
                        viewModel.reloadPages()
                    }
                )
            },
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    contentWidthDp = with(localDensity) { coordinates.size.width.toDp() }
                }
        ) {
            Box(Modifier.padding(bottom = 60.dp)) {
                if (contentWidthDp < 600.dp) {
                    DrawerLayout(
                        sideContent = projectListView,
                        mainContent = projectContentView,
                        closeCommandFlow = drawerCloseCommandFlow,
                    )
                } else {
                    DualPanelLayout(
                        leftPanelContent = projectListView,
                        rightPanelContent = projectContentView
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.deletePageDialog.isShown,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PopupDialog(
                text = "Do you want to delete ${uiState.deletePageDialog.ghPagePathToDelete}?",
                okButtonText = "Delete",
                cancelButtonText = "Cancel",
                onCancelButton = { viewModel.clickCancelDelete() },
                onOkButton = { viewModel.clickConfirmDelete() },
            )
        }
    }
}

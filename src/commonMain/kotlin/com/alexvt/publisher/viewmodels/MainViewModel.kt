package com.alexvt.publisher.viewmodels

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.Settings
import com.alexvt.publisher.repositories.SettingsProfile
import com.alexvt.publisher.usecases.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import me.tatarka.inject.annotations.Inject
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

@AppScope
@Inject
class MainViewModelUseCases(
    val listPagesUseCase: ListPagesUseCase,
    val showPageUseCase: ShowPageUseCase,
    val publishPageUseCase: PublishPageUseCase,
    val deletePageUseCase: DeletePageUseCase,
    val readSettingsUseCase: ReadSettingsUseCase,
    val editNewPageUseCase: EditNewPageUseCase,
    val writeSettingsProfileUseCase: WriteSettingsProfileUseCase,
    val markdownHighlightUseCase: MarkdownHighlightUseCase,
    val getPageLinkUseCase: GetPageLinkUseCase,
)

class MainViewModel(
    private val useCases: MainViewModelUseCases,
    backgroundDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val backgroundCoroutineScope = viewModelScope + backgroundDispatcher

    data class SidebarPage(
        val title: String,
        val textTint: Int,
        val isNew: Boolean,
        val isSelected: Boolean,
    )

    data class DeletePageDialog(
        val isShown: Boolean,
        val ghPagePathToDelete: String,
    )

    data class BusyDialog(
        val isShown: Boolean,
        val message: String,
    )

    data class ViewedPage(
        val title: String,
        val contentText: String,
        val isShowingText: Boolean,
        val isNew: Boolean,
        val hint: String = "Enter Markdown text",
        val hasPageLink: Boolean,
        val pageLink: String,
    )

    data class UiState(
        val sidebarPages: List<SidebarPage>,
        val deletePageDialog: DeletePageDialog,
        val viewedPage: ViewedPage,
        val isBottomSheetExpanded: Boolean,
        val isAlternativeProfile: Boolean,
        val settingsProfile: SettingsProfile,
        val bottomSheetErrorMessage: String,
        val isContentMarkedOutOfSync: Boolean,
        val busyDialog: BusyDialog,
        val isThemeLight: Boolean,
    )

    private fun getBlankState(settings: Settings): UiState {
        return UiState(
            sidebarPages = listOf(),
            deletePageDialog = DeletePageDialog(isShown = false, ghPagePathToDelete = ""),
            viewedPage = ViewedPage(
                title = "",
                contentText = "",
                isShowingText = true,
                isNew = true,
                hasPageLink = false,
                pageLink = "",
            ),
            isBottomSheetExpanded = false,
            isAlternativeProfile = false,
            settingsProfile = settings.mainProfile,
            bottomSheetErrorMessage = "",
            isContentMarkedOutOfSync = true,
            busyDialog = BusyDialog(isShown = true, message = "Loading from GitHub..."),
            isThemeLight = settings.mainProfile.isThemeLight(),
        )
    }

    private fun SettingsProfile.isThemeLight(): Boolean =
        theme.color.run { background.normal > text.normal }

    val uiStateFlow: StateFlow<UiState> =
        MutableStateFlow(
            value = getBlankState(
                settings = useCases.readSettingsUseCase.execute()
            )
        ).also {
            backgroundCoroutineScope.launch {
                loadPageList()
                loadSelectedPageToViewArea()
            }
        }

    private var uiState: UiState = (uiStateFlow as MutableStateFlow).value
        set(newUiState) {
            field = newUiState.apply((uiStateFlow as MutableStateFlow)::tryEmit)
        }

    fun onViewedPageTextUpdated(newText: String) {
        useCases.editNewPageUseCase.execute(newText)
        uiState = uiState.copy(
            viewedPage = uiState.viewedPage.copy(contentText = newText)
        )
    }

    fun onSettingsProfileUpdated(settingsProfile: SettingsProfile) {
        if (settingsProfile == uiState.settingsProfile) return // no changes
        useCases.writeSettingsProfileUseCase.execute(settingsProfile, uiState.isAlternativeProfile)
        uiState = uiState.copy(
            settingsProfile = settingsProfile,
            isThemeLight = settingsProfile.isThemeLight(),
        )
    }

    fun clickSidebarPage(pageTitle: String) {
        uiState = uiState.copy(
            sidebarPages = uiState.sidebarPages.map { page ->
                page.copy(isSelected = pageTitle == page.title)
            },
        )
        backgroundCoroutineScope.launch {
            if (!uiState.busyDialog.isShown) {
                loadPageList() // already loading when busy
            }
            loadSelectedPageToViewArea()
        }
    }

    fun rightClickSidebarPage(pageTitle: String) {
        if (uiState.busyDialog.isShown) return // operations only in fully loaded state
        uiState = uiState.copy(
            deletePageDialog = DeletePageDialog(isShown = true, ghPagePathToDelete = pageTitle)
        )
    }

    fun clickConfirmDelete() {
        val ghPagePathToDelete = uiState.deletePageDialog.ghPagePathToDelete
        val isDeletingViewedPage = ghPagePathToDelete == getViewedPagePath()
        uiState = uiState.copy(
            deletePageDialog = DeletePageDialog(isShown = false, ghPagePathToDelete = ""),
            bottomSheetErrorMessage = "",
        )
        backgroundCoroutineScope.launch {
            useCases.deletePageUseCase.execute(
                ghPagePath = ghPagePathToDelete,
                isAlternativeProfile = uiState.isAlternativeProfile,
            ).onSuccess {
                loadPageList()
                if (isDeletingViewedPage) {
                    loadSelectedPageToViewArea()
                }
            }.onFailure { error ->
                uiState = uiState.copy(
                    bottomSheetErrorMessage = "Failed to delete $ghPagePathToDelete.\n$error",
                )
            }
        }
    }

    fun clickCancelDelete() {
        uiState = uiState.copy(
            deletePageDialog = DeletePageDialog(isShown = false, ghPagePathToDelete = "")
        )
    }

    fun clickPublish() {
        if (uiState.busyDialog.isShown) return
        backgroundCoroutineScope.launch {
            val pageTitle = getViewedPagePath()
            val isNewPage = uiState.sidebarPages.first().isSelected
            uiState = uiState.copy(
                busyDialog = BusyDialog(isShown = true, message = "Publishing $pageTitle..."),
                isContentMarkedOutOfSync = true,
                bottomSheetErrorMessage = "",
            )
            useCases.publishPageUseCase.execute(
                ghPagesPath = pageTitle,
                pageText = uiState.viewedPage.contentText,
                isNew = isNewPage,
                isAlternativeProfile = uiState.isAlternativeProfile,
            ).onSuccess {
                loadPageList()
                clickSidebarPage(pageTitle) // view the published page
            }.onFailure { error ->
                uiState =
                    uiState.copy(bottomSheetErrorMessage = "Failed to publish $pageTitle\n$error")
            }
            uiState = uiState.copy(
                busyDialog = BusyDialog(isShown = false, message = "Done."),
                isContentMarkedOutOfSync = uiState.bottomSheetErrorMessage.isNotBlank(),
            )
        }
    }

    fun selectSettingsProfile(isAlternativeProfile: Boolean) {
        val isProfileChanged =
            isAlternativeProfile != uiState.isAlternativeProfile
        if (!isProfileChanged) return
        with(useCases.readSettingsUseCase.execute()) {
            uiState = uiState.copy(
                isAlternativeProfile = isAlternativeProfile,
                settingsProfile = if (isAlternativeProfile) alternativeProfile else mainProfile,
            )
        }
        backgroundCoroutineScope.launch {
            loadPageList()
            loadSelectedPageToViewArea()
        }
    }

    fun reloadPages() {
        backgroundCoroutineScope.launch {
            loadPageList()
            loadSelectedPageToViewArea()
        }
    }

    fun highlightMarkdown(text: String): List<ContentHighlight> =
        useCases.markdownHighlightUseCase.execute(text)

    private fun loadPageList() {
        val selectedPageTitleOrNull = uiState.sidebarPages.firstOrNull { it.isSelected }?.title
        uiState = uiState.copy(
            busyDialog = BusyDialog(isShown = true, message = "Loading pages..."),
            isContentMarkedOutOfSync = true,
            bottomSheetErrorMessage = "",
        )
        useCases.listPagesUseCase.execute(
            isAlternativeProfile = uiState.isAlternativeProfile,
        ).onSuccess { loadedPages ->
            val isKeepSelection =
                loadedPages.any { it.title == selectedPageTitleOrNull }
            uiState = uiState.copy(
                sidebarPages = loadedPages.mapIndexed { pageIndex, loadedPage ->
                    SidebarPage(
                        title = loadedPage.title,
                        textTint = 0xDDDDBB,
                        isSelected = if (isKeepSelection) {
                            loadedPage.title == selectedPageTitleOrNull
                        } else {
                            pageIndex == 0
                        },
                        isNew = loadedPage.pageType == PageType.NEW,
                    )
                },
            )
        }.onFailure { error ->
            uiState = uiState.copy(bottomSheetErrorMessage = "Failed to load pages.\n$error")
        }
        uiState = uiState.copy(
            busyDialog = BusyDialog(isShown = false, message = "Done."),
            isContentMarkedOutOfSync = uiState.bottomSheetErrorMessage.isNotBlank(),
        )
    }

    private fun getViewedPagePath(): String =
        uiState.viewedPage.title

    private fun loadSelectedPageToViewArea() {
        val selectedPageTitle = uiState.sidebarPages.firstOrNull { it.isSelected }?.title ?: run {
            uiState = uiState.copy(bottomSheetErrorMessage = "Failed to load page")
            return
        }
        uiState = uiState.copy(
            busyDialog = BusyDialog(isShown = true, message = "Loading $selectedPageTitle..."),
            isContentMarkedOutOfSync = true,
            bottomSheetErrorMessage = "",
        )
        val isSelectedPageNew = uiState.sidebarPages.firstOrNull { it.isSelected }?.isNew ?: false
        useCases.showPageUseCase.execute(
            title = selectedPageTitle,
            isNew = isSelectedPageNew,
            isAlternativeProfile = uiState.isAlternativeProfile,
        ).onSuccess { loadedPage ->
            uiState = uiState.copy(
                viewedPage = ViewedPage(
                    title = loadedPage.fileName,
                    contentText = loadedPage.text,
                    isShowingText = loadedPage.isShowingText,
                    isNew = loadedPage.isNew,
                    hasPageLink = !loadedPage.isNew,
                    pageLink = useCases.getPageLinkUseCase.execute(
                        pagePath = loadedPage.fileName,
                        isAlternativeProfile = uiState.isAlternativeProfile,
                    ),
                ),
            )
        }.onFailure { error ->
            uiState =
                uiState.copy(bottomSheetErrorMessage = "Failed to load $selectedPageTitle\n$error")
        }
        uiState = uiState.copy(
            busyDialog = BusyDialog(isShown = false, message = "Done."),
            isContentMarkedOutOfSync = uiState.bottomSheetErrorMessage.isNotBlank(),
        )
    }

}

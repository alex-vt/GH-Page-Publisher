package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.GhPagesFile
import com.alexvt.publisher.repositories.GhPagesRepository
import com.alexvt.publisher.repositories.LogRepository
import com.alexvt.publisher.repositories.SettingsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class ListPagesUseCase(
    private val getPageTypeUseCase: GetPageTypeUseCase,
    private val ghPagesRepository: GhPagesRepository,
    private val settingsRepository: SettingsRepository,
    private val logRepository: LogRepository,
) {

    fun execute(isAlternativeProfile: Boolean): Result<List<PageMetadata>> {
        val result = ghPagesRepository.listAllRemote(
            settingsProfile = with(settingsRepository.readSettings()) {
                if (isAlternativeProfile) alternativeProfile else mainProfile
            }
        ).mapCatching { ghPageFiles ->
            listOf(getNewPageMetadata()) + ghPageFiles
                .filterNot { it.ghPagesPath.startsWith("/.git/") }
                .map { it.toPageMetadata() }
                .sortedBy { (title, pageType) ->
                    // priorities: file first, by page type, by name
                    val fileOrFolderPriority = if (title.contains("/")) "1" else "0"
                    val pageTypePriority = pageType.ordinal.toString()
                    fileOrFolderPriority + pageTypePriority + title
                }
        }
        result.exceptionOrNull()?.let {
            logRepository.log("Failed to list pages", it)
        }
        return result
    }

    private fun getNewPageMetadata(): PageMetadata =
        PageMetadata(
            title = "page${getRandomizedTimeBasedId()}.md",
            pageType = PageType.NEW,
        )

    private fun getRandomizedTimeBasedId(): Long =
        System.currentTimeMillis() / 371

    private fun GhPagesFile.toPageMetadata(): PageMetadata =
        PageMetadata( // folders keep / prefix and files do not
            title = if (ghPagesPath.removePrefix("/").contains("/")) {
                ghPagesPath
            } else {
                ghPagesPath.removePrefix("/")
            },
            pageType = getPageTypeUseCase.execute(ghPagesPath, isNew = false)
        )

}

data class PageMetadata(
    val title: String,
    val pageType: PageType,
)

enum class PageType {
    NEW, MARKDOWN, INDEX, OTHER_SUPPORTED, OTHER_UNSUPPORTED
}

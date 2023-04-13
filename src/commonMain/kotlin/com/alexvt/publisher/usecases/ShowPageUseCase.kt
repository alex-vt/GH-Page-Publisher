package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.GhPagesRepository
import com.alexvt.publisher.repositories.LogRepository
import com.alexvt.publisher.repositories.NewPageRepository
import com.alexvt.publisher.repositories.SettingsRepository
import me.tatarka.inject.annotations.Inject
import java.nio.charset.Charset

@AppScope
@Inject
class ShowPageUseCase(
    private val getPageTypeUseCase: GetPageTypeUseCase,
    private val ghPagesRepository: GhPagesRepository,
    private val newPageRepository: NewPageRepository,
    private val settingsRepository: SettingsRepository,
    private val logRepository: LogRepository,
) {

    fun execute(title: String, isNew: Boolean, isAlternativeProfile: Boolean): Result<Page> {
        val result = when (getPageTypeUseCase.execute(title, isNew)) {
            PageType.NEW -> Result.success(
                Page(
                    fileName = title,
                    text = newPageRepository.get(),
                    isShowingText = true,
                    isNew = true,
                )
            )
            PageType.OTHER_UNSUPPORTED -> Result.success(
                Page(
                    fileName = title,
                    text = "",
                    isShowingText = false,
                    isNew = false,
                )
            )
            else -> ghPagesRepository.getPage(
                ghPageFilePath = title,
                settingsProfile = with(settingsRepository.readSettings()) {
                    if (isAlternativeProfile) alternativeProfile else mainProfile
                }
            ).mapCatching { ghPage ->
                Page(
                    fileName = ghPage.ghPagesPath,
                    text = ghPage.content?.toString(Charset.defaultCharset()) ?: "",
                    isShowingText = true,
                    isNew = false,
                )
            }
        }
        result.exceptionOrNull()?.let {
            logRepository.log("Failed to show page $title", it)
        }
        return result
    }

}

data class Page(
    val fileName: String,
    val text: String,
    val isNew: Boolean,
    val isShowingText: Boolean,
)

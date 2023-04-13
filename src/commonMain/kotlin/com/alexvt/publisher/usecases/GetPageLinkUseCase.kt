package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.SettingsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class GetPageLinkUseCase(
    private val settingsRepository: SettingsRepository,
    private val getPageTypeUseCase: GetPageTypeUseCase,
) {

    fun execute(pagePath: String, isAlternativeProfile: Boolean): String {
        val settingsProfile = with(settingsRepository.readSettings()) {
            if (isAlternativeProfile) alternativeProfile else mainProfile
        }
        val githubPagesDomainName = settingsProfile.githubPagesRepoUrl
            .substringAfterLast('/')
            .substringBeforeLast(".git")
        val cleanPagePath = pagePath.removePrefix("/").run {
            when (getPageTypeUseCase.execute(title = this, isNew = false)) {
                PageType.INDEX -> ""
                PageType.MARKDOWN -> substringBeforeLast(".")
                else -> this
            }
        }
        return "https://$githubPagesDomainName/$cleanPagePath"
    }

}

package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.GhPagesRepository
import com.alexvt.publisher.repositories.LogRepository
import com.alexvt.publisher.repositories.SettingsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class DeletePageUseCase(
    private val ghPagesRepository: GhPagesRepository,
    private val settingsRepository: SettingsRepository,
    private val logRepository: LogRepository,
) {

    fun execute(ghPagePath: String, isAlternativeProfile: Boolean): Result<Unit> {
         val result = ghPagesRepository.deleteRemote(
            ghPageFilePaths = listOf(ghPagePath),
            settingsProfile = with(settingsRepository.readSettings()) {
                if (isAlternativeProfile) alternativeProfile else mainProfile
            }
        )
        result.exceptionOrNull()?.let {
            logRepository.log("Failed to delete page $ghPagePath", it)
        }
        return result
    }

}

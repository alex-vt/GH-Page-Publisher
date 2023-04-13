package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.SettingsProfile
import com.alexvt.publisher.repositories.SettingsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class WriteSettingsProfileUseCase(
    private val settingsRepository: SettingsRepository,
) {

    fun execute(settingsProfile: SettingsProfile, isAlternative: Boolean) {
        val settings = settingsRepository.readSettings()
        if (isAlternative) {
            settings.copy(alternativeProfile = settingsProfile)
        } else {
            settings.copy(mainProfile = settingsProfile)
        }.run(settingsRepository::writeSettings)
    }

}

package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.Settings
import com.alexvt.publisher.repositories.SettingsRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class ReadSettingsUseCase(
    private val settingsRepository: SettingsRepository,
) {

    fun execute(): Settings {
        return settingsRepository.readSettings()
    }

}

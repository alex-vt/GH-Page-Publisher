package com.alexvt.publisher.repositories

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
expect class SettingsStorageRepository() {

    fun read(defaultValue: String): String

    fun write(settingsFileText: String)

}

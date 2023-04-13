package com.alexvt.publisher.repositories

import java.nio.file.Files
import java.nio.file.Paths

actual class SettingsStorageRepository {

    private val homeDirectory = System.getProperty("user.home")
    private val settingsFullPath = Paths.get(homeDirectory, ".GH-Page-Publisher/settings.json")

    actual fun read(defaultValue: String): String =
        if (Files.exists(settingsFullPath)) {
            Files.readString(settingsFullPath)
        } else {
            defaultValue
        }

    actual fun write(settingsFileText: String) {
        Files.createDirectories(settingsFullPath.parent)
        Files.writeString(settingsFullPath, settingsFileText)
    }

}
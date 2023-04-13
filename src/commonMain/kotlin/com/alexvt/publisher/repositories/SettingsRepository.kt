package com.alexvt.publisher.repositories

import com.alexvt.publisher.AppScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@kotlinx.serialization.Serializable
data class TextColors(
    val normal: Long,
    val dim: Long,
    val bright: Long,
    val error: Long,
)

@kotlinx.serialization.Serializable
data class BackgroundColors(
    val normal: Long,
    val bright: Long,
    val dim: Long,
    val accent: Long,
    val accentBright: Long,
)

@kotlinx.serialization.Serializable
data class ThemeColors(
    val background: BackgroundColors,
    val text: TextColors,
)

@kotlinx.serialization.Serializable
data class FontSizes(
    val small: Int,
    val normal: Int,
    val big: Int,
    val button: Int,
)

@kotlinx.serialization.Serializable
data class ThemeFonts(
    val size: FontSizes,
)

@kotlinx.serialization.Serializable
data class Theme(
    val color: ThemeColors,
    val font: ThemeFonts,
)

@kotlinx.serialization.Serializable
data class SettingsProfile(
    val githubPagesRepoUrl: String = "",
    val githubPersonalAccessToken: String = "",
    val pageHeader: String = "",
    val pageFooter: String = "",
    val mainListingHeaderName: String = "",
    val linkedFilesFolderPath: String = "",
    val commitMessage: String = "",
    val commitUser: String = "",
    val commitEmail: String = "",
    val overwriteLastCommit: Boolean = false,
    val repoFolderName: String,
    val theme: Theme,
)

@kotlinx.serialization.Serializable
data class Settings(
    val mainProfile: SettingsProfile,
    val alternativeProfile: SettingsProfile,
)

@AppScope
@Inject
class SettingsRepository(
    private val settingsStorageRepository: SettingsStorageRepository
) {
    private val defaultTheme = Theme(
        color = ThemeColors(
            background = BackgroundColors(
                normal = 0xFF303233,
                bright = 0xFF3C3D40,
                dim = 0xFF202122,
                accent = 0xFF006688,
                accentBright = 0xFF1180A2,
            ),
            text = TextColors(
                normal = 0xFFEEEEEE,
                dim = 0xFFBBBBBB,
                bright = 0xFFFFFFFF,
                error = 0xFFFF7777,
            ),
        ),
        font = ThemeFonts(
            size = FontSizes(
                small = 13,
                normal = 14,
                big = 16,
                button = 18,
            )
        )
    )

    private val blankSettings = Settings(
        mainProfile = SettingsProfile(
            repoFolderName = "mainProfileRepository",
            theme = defaultTheme,
        ),
        alternativeProfile = SettingsProfile(
            repoFolderName = "alternativeProfileRepository",
            theme = defaultTheme,
        )
    )
    private val json = Json { prettyPrint = true }

    fun readSettings(): Settings {
        return settingsStorageRepository
            .read(defaultValue = json.encodeToString(blankSettings))
            .run(json::decodeFromString)
    }

    fun writeSettings(settings: Settings) {
        settingsStorageRepository.write(settingsFileText = json.encodeToString(settings))
    }

}

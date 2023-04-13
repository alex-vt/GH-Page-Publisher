package com.alexvt.publisher.repositories

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.alexvt.publisher.App.Companion.androidAppContext

actual class SettingsStorageRepository {

    private val keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC
    private val mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec)
    private val securePreferences = EncryptedSharedPreferences.create(
        "settings",
        mainKeyAlias,
        androidAppContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val key = "settings"

    actual fun read(defaultValue: String): String =
        securePreferences.getString(key, defaultValue) ?: defaultValue

    actual fun write(settingsFileText: String) {
        with(securePreferences.edit()) {
            putString(key, settingsFileText)
            commit()
        }
    }

}
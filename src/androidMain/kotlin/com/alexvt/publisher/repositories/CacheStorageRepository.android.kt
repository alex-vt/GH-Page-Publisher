package com.alexvt.publisher.repositories

import com.alexvt.publisher.App.Companion.androidAppContext

actual class CacheStorageRepository {

    actual fun getRootFolderPath(): String =
        androidAppContext.dataDir.absolutePath

}

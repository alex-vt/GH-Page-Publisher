package com.alexvt.publisher.repositories

import java.nio.file.Paths

actual class CacheStorageRepository {

    private val homeDirectory = System.getProperty("user.home")
    private val rootFolderPath = Paths.get(homeDirectory, ".GH-Page-Publisher/cache/").toString()

    actual fun getRootFolderPath(): String =
        rootFolderPath

}

package com.alexvt.publisher.repositories

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
expect class CacheStorageRepository() {

    fun getRootFolderPath(): String

}

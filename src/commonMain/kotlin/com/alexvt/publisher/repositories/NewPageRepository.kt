package com.alexvt.publisher.repositories

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class NewPageRepository {

    private var newPageText: String = "" // in memory only

    fun get(): String =
        newPageText

    fun set(value: String) {
        newPageText = value
    }

}
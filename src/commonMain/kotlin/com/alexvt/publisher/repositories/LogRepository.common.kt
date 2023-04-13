package com.alexvt.publisher.repositories

import com.alexvt.publisher.AppScope
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
expect class LogRepository() {

    fun log(message: String)

    fun log(message: String, throwable: Throwable)

}

package com.alexvt.publisher.repositories

import android.util.Log

actual class LogRepository {

    private val tag = "GHPagesPublisher"

    actual fun log(message: String) {
        Log.d(tag, message)
    }

    actual fun log(message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
    }

}

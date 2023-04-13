package com.alexvt.publisher.repositories

actual class LogRepository {

    actual fun log(message: String) {
        println(message)
    }

    actual fun log(message: String, throwable: Throwable) {
        println("$message: $throwable")
        throwable.printStackTrace()
    }

}

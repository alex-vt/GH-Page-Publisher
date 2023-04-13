package com.alexvt.publisher.viewutils

import androidx.compose.ui.platform.UriHandler

actual fun browseLink(uriHandler: UriHandler, link: String) {
    Runtime.getRuntime().exec(arrayOf("bash", "-c", "open $link"))
}

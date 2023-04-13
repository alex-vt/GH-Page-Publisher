package com.alexvt.publisher.viewutils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.alexvt.publisher.App.Companion.androidAppContext

actual fun copyToClipboard(text: String) {
    val clipboardManager = androidAppContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("", text)
    clipboardManager.setPrimaryClip(clipData)
}

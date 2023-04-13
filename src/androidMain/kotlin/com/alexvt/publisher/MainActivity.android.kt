package com.alexvt.publisher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import com.alexvt.publisher.viewui.MainView
import kotlinx.coroutines.Dispatchers
import moe.tlaster.precompose.lifecycle.PreComposeActivity
import moe.tlaster.precompose.lifecycle.setContent


@ExperimentalFoundationApi
class MainActivity : PreComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainView(App.dependencies, Dispatchers.Default)
        }

        if (!Environment.isExternalStorageManager()) { // todo request more gracefully
            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
        }

    }

}
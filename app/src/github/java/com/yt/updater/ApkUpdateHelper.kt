package com.yt.updater

import android.app.Activity
import com.supersuman.apkupdater.ApkUpdater

object ApkUpdateHelper {
    fun requestDownload(
        activity: Activity,
        releaseUrl: String,
    ) {
        val updater = ApkUpdater(activity, releaseUrl)
        updater.requestDownload()
    }
}

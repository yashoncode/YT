package com.yt.ui.screens.channel

import android.os.Build
import com.yt.BuildConfig
import com.yt.innertube.YouTube
import com.yt.innertube.models.YouTubeClient

internal fun buildChannelRequestErrorLog(
    operation: String,
    channelId: String,
    error: Throwable,
    query: String? = null,
): String {
    val locale = YouTube.locale
    return buildString {
        appendLine("YT CHANNEL REQUEST ERROR")
        appendLine("Operation: $operation")
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("WEB client: ${YouTubeClient.WEB.clientVersion}")
        appendLine("Locale: hl=${locale.hl}, gl=${locale.gl}")
        appendLine("Visitor data present: ${!YouTube.visitorData.isNullOrBlank()}")
        appendLine("Channel: $channelId")
        query?.let { appendLine("Query: $it") }
        appendLine()
        append(error.stackTraceToString())
    }
}

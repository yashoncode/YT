package com.yt.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.yt.network.AppProxyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

data class UpdateInfo(
    val version: String,
    val changelog: String,
    val downloadUrl: String,
    val isNewer: Boolean,
)

internal data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
)

object UpdateManager {
    private val client: OkHttpClient
        get() = AppProxyManager.applyTo(OkHttpClient.Builder()).build()

    private const val GITHUB_REPO = "yashoncode/YT"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    const val RELEASES_PAGE = "https://github.com/$GITHUB_REPO/releases/latest"

    // Release asset names are a public contract set by .github/workflows/build.yml.
    private const val UNIVERSAL_APK = "yt.apk"
    private const val FOSS_APK_MARKER = "foss"
    private val ABI_APKS = mapOf("arm64-v8a" to "yt-arm64-v8a.apk", "armeabi-v7a" to "yt-armeabi-v7a.apk")

    /** The newer release, or null when this build is current. Throws when GitHub cannot be reached. */
    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            val request =
                Request
                    .Builder()
                    .url(API_URL)
                    .addHeader("Accept", "application/vnd.github+json")
                    .build()

            val body =
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("GitHub releases returned HTTP ${response.code}")
                    response.body.string()
                }
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            if (!isNewer(tag, currentVersionName)) return@withContext null

            val assets = json.optJSONArray("assets")
            val releaseAssets =
                (0 until (assets?.length() ?: 0))
                    .map { assets!!.getJSONObject(it) }
                    .filter { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    .map { ReleaseAsset(it.optString("name"), it.optString("browser_download_url")) }

            UpdateInfo(
                version = tag,
                changelog = json.optString("body"),
                downloadUrl =
                    selectApkDownloadUrl(releaseAssets, Build.SUPPORTED_ABIS.asList())
                        ?: json.optString("html_url").ifBlank { RELEASES_PAGE },
                isNewer = true,
            )
        }

    /** Numeric part-by-part comparison; a leading `v` and any `-suffix` are ignored. */
    internal fun isNewer(
        remote: String,
        current: String,
    ): Boolean {
        fun parts(version: String) =
            version
                .trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }

        val remoteParts = parts(remote)
        val currentParts = parts(current)
        for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    /**
     * This device's ABI split when the release has one, else the universal github-flavor APK,
     * else the single APK a hand-published release carries (`YT-<version>.apk`).
     */
    internal fun selectApkDownloadUrl(
        assets: List<ReleaseAsset>,
        supportedAbis: List<String>,
    ): String? {
        val githubAssets = assets.filterNot { it.name.contains(FOSS_APK_MARKER, ignoreCase = true) }

        fun named(name: String) = githubAssets.firstOrNull { it.name.equals(name, ignoreCase = true) }?.downloadUrl

        return supportedAbis.firstNotNullOfOrNull { abi -> ABI_APKS[abi]?.let(::named) }
            ?: named(UNIVERSAL_APK)
            ?: githubAssets.singleOrNull()?.downloadUrl
    }

    /** Hands the APK link to the browser, which downloads it and passes it to the system installer. */
    fun triggerDownload(
        context: Context,
        url: String,
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Could not open browser", e)
        }
    }
}

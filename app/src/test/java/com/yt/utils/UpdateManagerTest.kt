package com.yt.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdateManagerTest {
    // The asset set .github/workflows/build.yml publishes on every release.
    private val releaseAssets =
        listOf(
            ReleaseAsset("yt.apk", "https://example.test/universal"),
            ReleaseAsset("yt-foss.apk", "https://example.test/foss"),
            ReleaseAsset("yt-arm64-v8a.apk", "https://example.test/arm64"),
            ReleaseAsset("yt-armeabi-v7a.apk", "https://example.test/armv7"),
            ReleaseAsset("yt-foss-arm64-v8a.apk", "https://example.test/foss-arm64"),
            ReleaseAsset("yt-foss-armeabi-v7a.apk", "https://example.test/foss-armv7"),
        )

    @Test
    fun selectApkDownloadUrl_arm64Device_selectsArm64GithubApk() {
        val url = UpdateManager.selectApkDownloadUrl(releaseAssets, listOf("arm64-v8a", "armeabi-v7a"))
        assertThat(url).isEqualTo("https://example.test/arm64")
    }

    @Test
    fun selectApkDownloadUrl_arm32Device_selectsArmv7GithubApk() {
        val url = UpdateManager.selectApkDownloadUrl(releaseAssets, listOf("armeabi-v7a", "armeabi"))
        assertThat(url).isEqualTo("https://example.test/armv7")
    }

    @Test
    fun selectApkDownloadUrl_unsplitAbi_fallsBackToUniversalApk() {
        val url = UpdateManager.selectApkDownloadUrl(releaseAssets, listOf("x86_64", "x86"))
        assertThat(url).isEqualTo("https://example.test/universal")
    }

    @Test
    fun selectApkDownloadUrl_neverOffersAFossBuild() {
        val url =
            UpdateManager.selectApkDownloadUrl(
                assets = releaseAssets.filter { it.name.startsWith("yt-foss") },
                supportedAbis = listOf("arm64-v8a"),
            )
        assertThat(url).isNull()
    }

    @Test
    fun selectApkDownloadUrl_handPublishedRelease_selectsItsOnlyApk() {
        val url =
            UpdateManager.selectApkDownloadUrl(
                assets = listOf(ReleaseAsset("YT-5.0.0.apk", "https://example.test/single")),
                supportedAbis = listOf("arm64-v8a"),
            )
        assertThat(url).isEqualTo("https://example.test/single")
    }

    @Test
    fun isNewer_comparesNumericallyAndIgnoresPrefixAndSuffix() {
        assertThat(UpdateManager.isNewer("v5.0.0", "4.15.24")).isTrue()
        assertThat(UpdateManager.isNewer("v4.15.24", "4.15.9")).isTrue()
        assertThat(UpdateManager.isNewer("v5.0", "5.0.0-debug")).isFalse()
        assertThat(UpdateManager.isNewer("v4.15.23", "4.15.24")).isFalse()
        assertThat(UpdateManager.isNewer("", "5.0.0")).isFalse()
    }
}

//==================================================================================================
//This implementation was based on metrolist's (https://github.com/MetrolistGroup/Metrolist)
//==================================================================================================

package com.yt.data.lyrics

import com.yt.data.lyrics.kugou.KuGou
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KuGouLyricsProvider : LyricsProvider {
    override val name = "KuGou"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?
    ): Result<List<LyricsEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val lrc = KuGou.getLyrics(title, artist, duration, album).getOrThrow()
            LyricsUtils.parseLyrics(lrc)
        }
    }
}

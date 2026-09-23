package com.yt.data.recommendation.music

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MusicSkipFeedbackTest {
    private val now = 1_700_000_000_000L

    private fun signal(
        artistKey: String = "UCartist",
        pct: Double,
        tier: MusicSkipTier = MusicSkipTier.NONE,
        autoplay: Boolean = false,
    ) = MusicSignal(
        trackId = "t1",
        artistKey = artistKey,
        artistDisplay = "Artist",
        percentPlayed = pct,
        skipTier = tier,
        isAutoplay = autoplay,
    )

    private fun listen(
        brain: MusicBrain,
        sig: MusicSignal,
    ) = MusicBrainLearn.applyMusicSignal(brain, sig, MusicBrainLearn.newlyCrossed(0.0, sig.percentPlayed), now, null)

    private fun brainWithArtist(score: Double): MusicBrain =
        MusicBrain().apply {
            artistAffinity["UCartist"] = MusicAffinity(plays = 5, score = score, lastPlayed = now)
            seenArtists.add("UCartist")
        }

    @Test
    fun `only a user-ended listen short of the count milestone is a skip`() {
        assertThat(MusicBrainLearn.skipTier(endedByUser = false, playedFraction = 0.05, playedMs = 5_000)).isEqualTo(MusicSkipTier.NONE)
        assertThat(MusicBrainLearn.skipTier(endedByUser = true, playedFraction = 0.6, playedMs = 5_000)).isEqualTo(MusicSkipTier.NONE)
        assertThat(MusicBrainLearn.skipTier(endedByUser = true, playedFraction = 0.05, playedMs = 29_999)).isEqualTo(MusicSkipTier.EARLY)
        assertThat(MusicBrainLearn.skipTier(endedByUser = true, playedFraction = 0.3, playedMs = 30_000)).isEqualTo(MusicSkipTier.PARTIAL)
    }

    @Test
    fun `an early skip lowers a known artist and teaches nothing else`() {
        val brain = brainWithArtist(score = 0.5)
        val counted = listen(brain, signal(pct = 0.05, tier = MusicSkipTier.EARLY))
        assertThat(counted).isFalse()
        assertThat(brain.artistAffinity["UCartist"]!!.score).isWithin(1e-9).of(0.5 * MusicBrainParams.EARLY_SKIP_SCORE_FACTOR)
        assertThat(brain.artistAffinity["UCartist"]!!.plays).isEqualTo(5)
    }

    @Test
    fun `an early skip of an unknown artist leaves them novel`() {
        val brain = MusicBrain()
        listen(brain, signal(artistKey = "UCnew", pct = 0.2, tier = MusicSkipTier.EARLY))
        assertThat(brain.artistAffinity).doesNotContainKey("UCnew")
        assertThat(brain.seenArtists).doesNotContain("UCnew")
    }

    @Test
    fun `a partial skip pulls the artist down where a sample used to lift it`() {
        val skipped = brainWithArtist(score = 0.2)
        listen(skipped, signal(pct = 0.3, tier = MusicSkipTier.PARTIAL))
        val sampled = brainWithArtist(score = 0.2)
        listen(sampled, signal(pct = 0.3))
        assertThat(skipped.artistAffinity["UCartist"]!!.score).isLessThan(0.2)
        assertThat(sampled.artistAffinity["UCartist"]!!.score).isGreaterThan(0.2)
    }

    @Test
    fun `autoplay listens teach at half strength`() {
        val chosen = MusicBrain()
        listen(chosen, signal(pct = 1.0))
        val autoplay = MusicBrain()
        listen(autoplay, signal(pct = 1.0, autoplay = true))
        val chosenScore = chosen.artistAffinity["UCartist"]!!.score
        val autoplayScore = autoplay.artistAffinity["UCartist"]!!.score
        assertThat(autoplayScore).isGreaterThan(0.0)
        assertThat(autoplayScore).isLessThan(chosenScore)
    }

    @Test
    fun `artists skipped this session sink to the end of a ranking`() {
        val brain = brainWithArtist(score = 0.9)
        val inputs = listOf(MusicRankInput("a", "UCartist"), MusicRankInput("b", "UCother"))
        val order =
            MusicBrainRanker.rank(
                brain,
                inputs,
                MusicBrainRanker.SURFACE_QUICK_PICKS,
                now,
                sessionSkipped = setOf("UCartist"),
            )
        assertThat(order).containsExactly(1, 0).inOrder()
    }

    @Test
    fun `session skips expire after the session gap and clear on a real listen`() {
        val skips = MusicSessionSkips()
        skips.record("UCa", MusicSkipTier.EARLY, now)
        skips.record("UCb", MusicSkipTier.PARTIAL, now)
        assertThat(skips.active(now + 1_000)).containsExactly("UCa", "UCb")
        skips.record("UCa", MusicSkipTier.NONE, now + 2_000)
        assertThat(skips.snapshot).containsExactly("UCb")
        assertThat(skips.active(now + MusicBrainParams.SESSION_GAP_MS)).isEmpty()
    }

    @Test
    fun `discovery mode moves every surface's novelty target and blend changes nothing`() {
        MusicBrainRanker.run {
            listOf(SURFACE_QUICK_PICKS, SURFACE_RADIO, SURFACE_SIMILAR, SURFACE_DISCOVER).forEach { surface ->
                val blend = surfaceTargetNovelty(surface, 0.3, MusicDiscoveryMode.BLEND)
                assertThat(blend).isEqualTo(surfaceTargetNovelty(surface, 0.3))
                assertThat(surfaceTargetNovelty(surface, 0.3, MusicDiscoveryMode.FAMILIAR)).isLessThan(blend)
                assertThat(surfaceTargetNovelty(surface, 0.3, MusicDiscoveryMode.DISCOVER)).isGreaterThan(blend)
            }
        }
    }

    @Test
    fun `unknown stored mode names fall back to blend`() {
        assertThat(MusicDiscoveryMode.fromName(null)).isEqualTo(MusicDiscoveryMode.BLEND)
        assertThat(MusicDiscoveryMode.fromName("LOUD")).isEqualTo(MusicDiscoveryMode.BLEND)
        assertThat(MusicDiscoveryMode.fromName("DISCOVER")).isEqualTo(MusicDiscoveryMode.DISCOVER)
    }
}

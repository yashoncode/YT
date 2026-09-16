package com.yt.data.engagement

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.yt.data.model.Video
import com.yt.data.recommendation.YTNeuroEngine
import com.yt.data.recommendation.InteractionType
import com.yt.data.repository.YouTubeRepository
import javax.inject.Inject

/**
 * The learning signals an engagement action feeds the recommendation engine.
 *
 * [YTNeuroEngine] is still reached through a context-keyed global, so this is the one place that
 * touches it: every engagement caller injects this instead, which keeps the global access isolated
 * behind a dependency a test can replace.
 */
class VideoEngagementSignals
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val repository: YouTubeRepository,
    ) {
        suspend fun channelSubscriptionChanged(
            channelId: String,
            channelName: String,
            subscribed: Boolean,
        ) = YTNeuroEngine.onChannelSubscriptionChanged(context, channelId, channelName, subscribed)

        suspend fun videoInteraction(
            video: Video,
            interactionType: InteractionType,
        ) = YTNeuroEngine.onVideoInteraction(context, video, interactionType)

        /** Learns a newly subscribed channel's declared keyword tags. */
        suspend fun channelTagsLearned(channelId: String) = repository.learnChannelTags(context, channelId)
    }

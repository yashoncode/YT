package com.yt.di

import android.content.Context
import com.yt.data.local.PlayerPreferences
import com.yt.data.repository.YouTubeRepository
import com.yt.data.shorts.ChannelReelIndex
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideYouTubeRepository(
        playerPreferences: PlayerPreferences,
        channelReelIndex: ChannelReelIndex,
    ): YouTubeRepository = YouTubeRepository.getInstance(playerPreferences, channelReelIndex)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        @ApplicationContext context: Context,
    ): com.yt.data.local.SubscriptionRepository =
        com.yt.data.local.SubscriptionRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideLikedVideosRepository(
        @ApplicationContext context: Context,
    ): com.yt.data.local.LikedVideosRepository =
        com.yt.data.local.LikedVideosRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideViewHistory(
        @ApplicationContext context: Context,
    ): com.yt.data.local.ViewHistory =
        com.yt.data.local.ViewHistory
            .getInstance(context)

    @Provides
    @Singleton
    fun provideHomeFeedCacheRepository(
        @ApplicationContext context: Context,
    ): com.yt.data.local.HomeFeedCacheRepository =
        com.yt.data.local
            .HomeFeedCacheRepository(context)

    @Provides
    @Singleton
    fun provideMusicPlaylistRepository(
        @ApplicationContext context: Context,
    ): com.yt.data.music.PlaylistRepository =
        com.yt.data.music
            .PlaylistRepository(context)

    // VideoDownloadManager is now @Singleton @Inject — Hilt provides it automatically
    @Provides
    @Singleton
    fun providePlayerPreferences(
        @ApplicationContext context: Context,
    ): com.yt.data.local.PlayerPreferences =
        com.yt.data.local
            .PlayerPreferences(context)

    @Provides
    @Singleton
    fun provideShortsRepository(
        @ApplicationContext context: Context,
    ): com.yt.data.shorts.ShortsRepository =
        com.yt.data.shorts.ShortsRepository
            .getInstance(context)
}

package com.yt.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.yt.data.local.dao.CacheDao
import com.yt.data.local.dao.DownloadDao
import com.yt.data.local.dao.HomeFeedCacheDao
import com.yt.data.local.dao.MusicGraphDao
import com.yt.data.local.dao.NoteDao
import com.yt.data.local.dao.NotificationDao
import com.yt.data.local.dao.PlaylistDao
import com.yt.data.local.dao.RecognitionHistoryDao
import com.yt.data.local.dao.SubscriptionGroupDao
import com.yt.data.local.dao.SyncLogDao
import com.yt.data.local.dao.SyncPeerDao
import com.yt.data.local.dao.VideoDao
import com.yt.data.local.dao.WatchHistoryDao
import com.yt.data.local.entity.DownloadEntity
import com.yt.data.local.entity.DownloadItemEntity
import com.yt.data.local.entity.HomeFeedCacheEntity
import com.yt.data.local.entity.MusicGraphAlbumEntity
import com.yt.data.local.entity.MusicGraphArtistEntity
import com.yt.data.local.entity.MusicGraphEdgeEntity
import com.yt.data.local.entity.MusicGraphPlaylistEntity
import com.yt.data.local.entity.MusicGraphTrackEntity
import com.yt.data.local.entity.MusicHomeCacheEntity
import com.yt.data.local.entity.MusicHomeChipEntity
import com.yt.data.local.entity.NoteEntity
import com.yt.data.local.entity.NotificationEntity
import com.yt.data.local.entity.PlaylistEntity
import com.yt.data.local.entity.PlaylistVideoCrossRef
import com.yt.data.local.entity.RecognitionHistoryEntity
import com.yt.data.local.entity.SubscriptionFeedEntity
import com.yt.data.local.entity.SubscriptionGroupEntity
import com.yt.data.local.entity.SyncLogEntity
import com.yt.data.local.entity.SyncPeerEntity
import com.yt.data.local.entity.VideoEntity
import com.yt.data.local.entity.WatchHistoryEntity
import com.yt.data.local.migrations.MIGRATIONS
import com.yt.data.local.migrations.Migration24To25

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        NotificationEntity::class,
        SubscriptionFeedEntity::class,
        MusicHomeCacheEntity::class,
        MusicHomeChipEntity::class,
        DownloadEntity::class,
        DownloadItemEntity::class,
        WatchHistoryEntity::class,
        HomeFeedCacheEntity::class,
        SubscriptionGroupEntity::class,
        RecognitionHistoryEntity::class,
        SyncLogEntity::class,
        SyncPeerEntity::class,
        MusicGraphTrackEntity::class,
        MusicGraphArtistEntity::class,
        MusicGraphAlbumEntity::class,
        MusicGraphPlaylistEntity::class,
        MusicGraphEdgeEntity::class,
        NoteEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 24, to = 25, spec = Migration24To25::class),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
    ],
    version = 27,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun notificationDao(): NotificationDao

    abstract fun noteDao(): NoteDao

    abstract fun cacheDao(): CacheDao

    abstract fun downloadDao(): DownloadDao

    abstract fun watchHistoryDao(): WatchHistoryDao

    abstract fun homeFeedCacheDao(): HomeFeedCacheDao

    abstract fun subscriptionGroupDao(): SubscriptionGroupDao

    abstract fun recognitionHistoryDao(): RecognitionHistoryDao

    abstract fun syncLogDao(): SyncLogDao

    abstract fun syncPeerDao(): SyncPeerDao

    abstract fun musicGraphDao(): MusicGraphDao

    companion object {
        @Volatile
        @Suppress("ktlint:standard:property-naming")
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "yt_database",
                    ).addMigrations(*MIGRATIONS)
                        .fallbackToDestructiveMigration(false)
                        .build()
                INSTANCE = instance
                instance
            }
    }
}

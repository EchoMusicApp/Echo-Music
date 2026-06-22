

package iad1tya.echo.music.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.room.Room
import iad1tya.echo.music.constants.MaxSongCacheSizeKey
import iad1tya.echo.music.db.InternalDatabase
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.listentogether.ListenTogetherClient
import iad1tya.echo.music.listentogether.ListenTogetherManager
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Singleton
    @Provides
    fun provideDao(
        database: InternalDatabase,
    ) = database.dao

    @Singleton
    @Provides
    fun provideEchoBrainDao(
        database: InternalDatabase,
    ) = database.echoBrainDao

    @Singleton
    @Provides
    fun provideDatabase(
        internalDatabase: InternalDatabase,
    ): MusicDatabase = MusicDatabase(internalDatabase)

    @Singleton
    @Provides
    fun provideInternalDatabase(
        @ApplicationContext context: Context,
    ): InternalDatabase = Room
        .databaseBuilder(context, InternalDatabase::class.java, InternalDatabase.DB_NAME)
        .addMigrations(
            iad1tya.echo.music.db.MIGRATION_1_2,
            iad1tya.echo.music.db.MIGRATION_21_24,
            iad1tya.echo.music.db.MIGRATION_22_24,
            iad1tya.echo.music.db.MIGRATION_24_25,
            iad1tya.echo.music.db.MIGRATION_27_28,
            iad1tya.echo.music.db.MIGRATION_36_37,
            iad1tya.echo.music.db.MIGRATION_37_38,
        )

        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .setTransactionExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
        .setQueryExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    db.query("PRAGMA busy_timeout = 60000").close()
                    db.query("PRAGMA cache_size = -16000").close()
                    db.query("PRAGMA wal_autocheckpoint = 1000").close()
                    db.query("PRAGMA synchronous = NORMAL").close()
                } catch (e: Exception) {
                    timber.log.Timber.tag("MusicDatabase").e(e, "Failed to set PRAGMA settings")
                }
            }
        })
        .build()

    @Singleton
    @Provides
    fun provideDatabaseProvider(
        @ApplicationContext context: Context,
    ): DatabaseProvider = StandaloneDatabaseProvider(context)

    @Singleton
    @Provides
    @PlayerCache
    fun providePlayerCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        val cacheSize = context.dataStore[MaxSongCacheSizeKey] ?: 1024
        return SimpleCache(
            context.filesDir.resolve("exoplayer"),
            when (cacheSize) {
                -1 -> NoOpCacheEvictor()
                else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
            },
            databaseProvider,
        )
    }

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        return SimpleCache(
            context.filesDir.resolve("download"),
            NoOpCacheEvictor(),
            databaseProvider
        )
    }

    @Singleton
    @Provides
    fun provideListenTogetherClient(
        @ApplicationContext context: Context,
    ): ListenTogetherClient = ListenTogetherClient(context)

    @Singleton
    @Provides
    fun provideListenTogetherManager(
        @ApplicationContext context: Context,
        client: ListenTogetherClient,
    ): ListenTogetherManager = ListenTogetherManager(client, context)
}

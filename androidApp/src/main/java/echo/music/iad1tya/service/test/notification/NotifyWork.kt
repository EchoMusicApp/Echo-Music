package echo.music.iad1tya.service.test.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import echo.music.iad1tya.domain.data.entities.ArtistEntity
import echo.music.iad1tya.domain.data.entities.FollowedArtistSingleAndAlbum
import echo.music.iad1tya.domain.data.entities.NotificationEntity
import echo.music.iad1tya.domain.data.model.searchResult.albums.AlbumsResult
import echo.music.iad1tya.domain.extension.now
import echo.music.iad1tya.domain.repository.AlbumRepository
import echo.music.iad1tya.domain.repository.ArtistRepository
import echo.music.iad1tya.domain.repository.CommonRepository
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.extension.symmetricDifference
import echo.music.iad1tya.viewModel.MoreAlbumsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotifyWork(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val albumRepository: AlbumRepository by inject()
    private val artistRepository: ArtistRepository by inject()

    private val commonRepository: CommonRepository by inject()

    private val mapOfNotification = arrayListOf<NotificationModel>()

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            Logger.w("NotifyWork", "doWork: ")
            val artistList: List<ArtistEntity> = artistRepository.getFollowedArtists().lastOrNull() ?: listOf()
            val listFollowedArtistSingleAndAlbum =
                albumRepository.getAllFollowedArtistSingleAndAlbums().lastOrNull() ?: listOf()
            Logger.w("NotifyWork", "doWork: $artistList")
            Logger.w("NotifyWork", "doWork: $listFollowedArtistSingleAndAlbum")
            artistList.forEach { art ->
                combine(
                    albumRepository.getAlbumMore("MPAD${art.channelId}", MoreAlbumsViewModel.ALBUM_PARAM),
                    albumRepository.getAlbumMore("MPAD${art.channelId}", MoreAlbumsViewModel.SINGLE_PARAM),
                ) { album, single ->
                    Pair(album, single)
                }.first().let { pair ->
                    val albumItem =
                        pair.first
                            ?.second
                    val singleItem =
                        pair.second
                            ?.second
                    val savedAlbum = listFollowedArtistSingleAndAlbum.find { it.channelId == art.channelId }?.album
                    if (!savedAlbum.isNullOrEmpty() && !albumItem.isNullOrEmpty()) {
                        val differentAlbum =
                            albumItem
                                .filter { ytItem ->
                                    (
                                        albumItem.map { item ->
                                            item.browseId
                                        } symmetricDifference (savedAlbum.map { it["browseId"] })
                                    ).contains(ytItem.browseId)
                                }
                        mapOfNotification.add(
                            NotificationModel(
                                name = art.name,
                                channelId = art.channelId,
                                single = listOf(),
                                album = differentAlbum,
                            ),
                        )
                    }
                    val savedSingle = listFollowedArtistSingleAndAlbum.find { it.channelId == art.channelId }?.single
                    if (!savedSingle.isNullOrEmpty() && !singleItem.isNullOrEmpty()) {
                        val differentSingle =
                            singleItem
                                .filter { ytItem ->
                                    (
                                        singleItem.map { item ->
                                            item.browseId
                                        } symmetricDifference (savedSingle.map { it["browseId"] })
                                    ).contains(ytItem.browseId)
                                }
                        mapOfNotification.add(
                            NotificationModel(
                                name = art.name,
                                channelId = art.channelId,
                                single = differentSingle,
                                album = listOf(),
                            ),
                        )
                    }
                    albumRepository.insertFollowedArtistSingleAndAlbum(
                        FollowedArtistSingleAndAlbum(
                            channelId = art.channelId,
                            name = art.name,
                            single = singleItem.toMap(),
                            album = albumItem.toMap(),
                        ),
                    )
                }
            }
            Logger.w("NotifyWork", "doWork: $mapOfNotification")
            NotificationHandler.createNotificationChannel(applicationContext)
            mapOfNotification.forEach { noti ->
                if (noti.album.isNotEmpty() || noti.single.isNotEmpty()) {
                    NotificationHandler.createReminderNotification(
                        applicationContext,
                        noti,
                    )
                    commonRepository.insertNotification(
                        NotificationEntity(
                            channelId = noti.channelId,
                            thumbnail = artistList.find { it.channelId == noti.channelId }?.thumbnails,
                            name = noti.name,
                            single = noti.single.toMap(),
                            album = noti.album.toMap(),
                            time = now(),
                        ),
                    )
                }
            }
            Result.success()
        }
}

private fun List<AlbumsResult>?.toMap(): List<Map<String, String>> =
    this?.map { single ->
        mapOf(
            "browseId" to single.browseId,
            "title" to single.title,
            "thumbnails" to (single.thumbnails.lastOrNull()?.url ?: ""),
        )
    } ?: emptyList()

data class NotificationModel(
    val name: String,
    val channelId: String,
    val single: List<AlbumsResult>,
    val album: List<AlbumsResult>,
)
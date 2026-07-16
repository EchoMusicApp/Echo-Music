

package iad1tya.echo.music.ui.menu

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import iad1tya.echo.music.LocalListenTogetherManager
import iad1tya.echo.music.R
import iad1tya.echo.music.db.entities.Playlist
import iad1tya.echo.music.db.entities.PlaylistSong
import iad1tya.echo.music.ui.component.Material3MenuGroup
import iad1tya.echo.music.ui.component.Material3MenuItemData


@Composable
fun LocalPlaylistMenu(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    context: Context,
    downloadState: Int,
    onEdit: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onExport: () -> Unit,
    onModifyWithAi: () -> Unit,
    onQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isGuestPlaybackRestricted == true

    val downloadMenuItem = when (downloadState) {
        Download.STATE_COMPLETED -> Material3MenuItemData(
            title = { Text(stringResource(R.string.remove_download)) },
            description = { Text(stringResource(R.string.remove_download_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> Material3MenuItemData(
            title = { Text(stringResource(R.string.downloading)) },
            description = { Text(stringResource(R.string.download_in_progress_desc)) },
            icon = {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        else -> Material3MenuItemData(
            title = { Text(stringResource(R.string.action_download)) },
            description = { Text(stringResource(R.string.download_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
    }

    val isYouTubePlaylist = playlist.playlist.browseId != null

    val menuItems = buildList {
        add(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.edit)) },
                description = { Text(stringResource(R.string.edit_playlist)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null
                    )
                },
                onClick = {
                    onEdit()
                    onDismiss()
                }
            )
        )
        
        if (playlist.playlist.isEditable) {
            add(
                Material3MenuItemData(
                    title = { Text(stringResource(R.string.modify_with_ai)) },
                    description = { Text(stringResource(R.string.modify_with_ai_desc)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.edit), // Or another icon
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onModifyWithAi()
                        onDismiss()
                    }
                )
            )
        }

        add(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.export_as_csv)) },
                description = { Text(stringResource(R.string.export_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.file_export),
                        contentDescription = null
                    )
                },
                onClick = {
                    onExport()
                    onDismiss()
                }
            )
        )

        
        if (isYouTubePlaylist) {
            add(
                Material3MenuItemData(
                    title = { Text(stringResource(R.string.action_sync)) },
                    description = { Text(stringResource(R.string.sync_playlist_desc)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onSync()
                        onDismiss()
                    }
                )
            )
        }

        if (!isGuest) {
            add(
                Material3MenuItemData(
                    title = { Text(stringResource(R.string.add_to_queue)) },
                    description = { Text(stringResource(R.string.add_to_queue_desc)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onQueue()
                        onDismiss()
                    }
                )
            )
        }

        add(downloadMenuItem)

        add(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.share)) },
                description = { Text(stringResource(R.string.share_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null
                    )
                },
                onClick = {
                    val shareText = if (isYouTubePlaylist) {
                        "https://share.echomusic.fun/playlist?list=${playlist.playlist.browseId}"
                    } else {
                        songs.joinToString("\n") { it.song.song.title }
                    }
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                    onDismiss()
                }
            )
        )

        add(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.delete)) },
                description = { Text(stringResource(R.string.delete_playlist_desc)) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null
                    )
                },
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )
        )
    }

    Material3MenuGroup(items = menuItems)
}


@Composable
fun AutoPlaylistMenu(
    downloadState: Int,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isGuestPlaybackRestricted == true

    val downloadMenuItem = when (downloadState) {
        Download.STATE_COMPLETED -> Material3MenuItemData(
            title = { Text(stringResource(R.string.remove_download)) },
            description = { Text(stringResource(R.string.remove_download_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> Material3MenuItemData(
            title = { Text(stringResource(R.string.downloading)) },
            description = { Text(stringResource(R.string.download_in_progress_desc)) },
            icon = {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        else -> Material3MenuItemData(
            title = { Text(stringResource(R.string.action_download)) },
            description = { Text(stringResource(R.string.download_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
    }

    Material3MenuGroup(
        items = listOfNotNull(
            if (!isGuest) {
                Material3MenuItemData(
                    title = { Text(stringResource(R.string.add_to_queue)) },
                    description = { Text(stringResource(R.string.add_to_queue_desc)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onQueue()
                        onDismiss()
                    }
                )
            } else null,
            downloadMenuItem
        )
    )
}


@Composable
fun TopPlaylistMenu(
    downloadState: Int,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isGuestPlaybackRestricted == true

    val downloadMenuItem = when (downloadState) {
        Download.STATE_COMPLETED -> Material3MenuItemData(
            title = { Text(stringResource(R.string.remove_download)) },
            description = { Text(stringResource(R.string.remove_download_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> Material3MenuItemData(
            title = { Text(stringResource(R.string.downloading)) },
            description = { Text(stringResource(R.string.download_in_progress_desc)) },
            icon = {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        else -> Material3MenuItemData(
            title = { Text(stringResource(R.string.action_download)) },
            description = { Text(stringResource(R.string.download_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
    }

    Material3MenuGroup(
        items = listOfNotNull(
            if (!isGuest) {
                Material3MenuItemData(
                    title = { Text(stringResource(R.string.add_to_queue)) },
                    description = { Text(stringResource(R.string.add_to_queue_desc)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onQueue()
                        onDismiss()
                    }
                )
            } else null,
            downloadMenuItem
        )
    )
}


@Composable
fun CachePlaylistMenu(
    downloadState: Int,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isGuestPlaybackRestricted == true

    val downloadMenuItem = when (downloadState) {
        Download.STATE_COMPLETED -> Material3MenuItemData(
            title = { Text(stringResource(R.string.remove_download)) },
            description = { Text(stringResource(R.string.remove_download_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> Material3MenuItemData(
            title = { Text(stringResource(R.string.downloading)) },
            description = { Text(stringResource(R.string.download_in_progress_desc)) },
            icon = {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
        else -> Material3MenuItemData(
            title = { Text(stringResource(R.string.action_download)) },
            description = { Text(stringResource(R.string.download_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null
                )
            },
            onClick = {
                onDownload()
                onDismiss()
            }
        )
    }

    Material3MenuGroup(
        items = listOfNotNull(
            if (!isGuest) {
                Material3MenuItemData(
                    title = { Text(stringResource(R.string.add_to_queue)) },
                    description = { Text(stringResource(R.string.add_to_queue_desc)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        onQueue()
                        onDismiss()
                    }
                )
            } else null,
            downloadMenuItem
        )
    )
}

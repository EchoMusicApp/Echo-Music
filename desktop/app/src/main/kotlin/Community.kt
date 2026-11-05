package echo.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.ArtistPage
import echo.desktop.core.YouTubeSetup
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Anything can call this to open an artist page by YouTube channel id. */
val LocalOpenArtist = compositionLocalOf<(String?) -> Unit> { {} }

class FollowedArtist(val id: String, val name: String, val thumb: String?, val known: Set<String>)

/**
 * "Echo Artist": artists you follow, saved in %APPDATA%\EchoMusic\artists.tsv. On start the app checks
 * each one on YouTube Music for releases it has not seen and tells you (in-app list plus a Windows toast).
 */
class Community {
    private val file = File(AppDir, "artists.tsv")
    private val notesFile = File(AppDir, "notifications.txt")

    var following by mutableStateOf(load())
        private set
    var notifications by mutableStateOf(runCatching { notesFile.readLines().takeLast(50).reversed() }.getOrDefault(emptyList()))
        private set

    private fun load() =
        runCatching {
                file.readLines().mapNotNull {
                    val p = it.split('\t')
                    if (p.size < 4) null else FollowedArtist(p[0], p[1], p[2].ifEmpty { null }, p[3].split(',').filter(String::isNotEmpty).toSet())
                }
            }
            .getOrDefault(emptyList())

    private fun save() =
        runCatching {
            file.parentFile.mkdirs()
            file.writeText(following.joinToString("\n") { "${it.id}\t${it.name.replace('\t', ' ')}\t${it.thumb.orEmpty()}\t${it.known.joinToString(",")}" })
        }

    fun isFollowing(id: String) = following.any { it.id == id }

    private fun releases(page: ArtistPage) = page.sections.flatMap { it.items }.filterIsInstance<AlbumItem>()

    suspend fun follow(page: ArtistPage) {
        // Everything out now counts as already seen, so only future releases notify.
        following = following.filter { it.id != page.artist.id } +
            FollowedArtist(page.artist.id, page.artist.title, page.artist.thumbnail, releases(page).map { it.id }.toSet())
        withContext(Dispatchers.IO) { save() }
    }

    suspend fun unfollow(id: String) {
        following = following.filter { it.id != id }
        withContext(Dispatchers.IO) { save() }
    }

    suspend fun checkReleases() {
        val updated = mutableListOf<FollowedArtist>()
        val found = mutableListOf<String>()
        for (a in following) {
            val page = withContext(Dispatchers.IO) { YouTubeSetup.ensureInit(); YouTube.artist(a.id) }.getOrNull()
            if (page == null) { updated += a; continue }
            val all = releases(page)
            all.filter { it.id !in a.known }.forEach { found += "New from ${a.name}: ${it.title}" }
            updated += FollowedArtist(a.id, a.name, a.thumb, a.known + all.map { it.id })
        }
        following = updated
        if (found.isEmpty()) return
        notifications = found + notifications
        withContext(Dispatchers.IO) {
            save()
            runCatching { notesFile.appendText(found.joinToString("\n", postfix = "\n")) }
            toast(if (found.size == 1) found[0] else "${found.size} new releases from artists you follow")
        }
    }

    private fun toast(message: String) {
        runCatching {
            if (!SystemTray.isSupported()) return
            val tray = SystemTray.getSystemTray()
            val icon = TrayIcon(BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), "Echo Music")
            tray.add(icon)
            icon.displayMessage("Echo Music", message, TrayIcon.MessageType.INFO)
            Thread { Thread.sleep(15_000); tray.remove(icon) }.start()
        }
    }
}

@Composable
private fun AlbumCard(item: YTItem, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        RemoteImage(hires(item.thumbnail), Modifier.size(150.dp), RoundedCornerShape(12.dp))
        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        Text(if (item is AlbumItem) listOfNotNull("Album", item.year).joinToString("  •  ") else "Playlist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ArtistScreen(
    page: ArtistPage?,
    status: String,
    community: Community,
    onFollow: (follow: Boolean) -> Unit,
    onPlay: (List<SongItem>, Int) -> Unit,
    onOpen: (YTItem) -> Unit,
) {
    Panel(Modifier.fillMaxSize()) {
        if (page == null) {
            Text(status, Modifier.padding(20.dp))
            return@Panel
        }
        val following = community.isFollowing(page.artist.id)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                RemoteImage(hires(page.artist.thumbnail.orEmpty()), Modifier.size(150.dp), CircleShape)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(page.artist.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(page.subscriberCountText?.let { "$it on YouTube" }, page.monthlyListenerCount).joinToString("  •  "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${if (following) 1 else 0} following on Echo (this device)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (following) OutlinedButton(onClick = { onFollow(false) }) { Text("Echo Artist ✓  (following)") }
                    else Button(onClick = { onFollow(true) }) { Text("Echo Artist") }
                }
            }
            page.description?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 4, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium) }
            page.sections.forEach { section ->
                val songs = section.items.filterIsInstance<SongItem>()
                if (songs.isNotEmpty()) {
                    Text(if (section.title.contains("song", true)) "Top songs" else section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    songs.take(8).forEachIndexed { i, s -> SongRow(s) { onPlay(songs, i) } }
                } else {
                    val cards = section.items.filter { it is AlbumItem || it is PlaylistItem }
                    if (cards.isNotEmpty()) {
                        Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) { items(cards) { AlbumCard(it) { onOpen(it) } } }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityScreen(community: Community) {
    val open = LocalOpenArtist.current
    Panel(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Community", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (Settings.avatarUrl.isNotBlank()) RemoteImage(Settings.avatarUrl, Modifier.size(84.dp), CircleShape)
                else Box(Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text(Settings.profileName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(Settings.profileName, { Settings.profileName = it; Settings.save() }, singleLine = true, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        Settings.avatarUrl, { Settings.avatarUrl = it; Settings.save() }, singleLine = true,
                        label = { Text("Profile picture link (Discord: right-click your avatar → Copy Link)") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${community.following.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("following", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "Followers from other people need an online account service, which Echo Desktop doesn't have yet.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Echo Artists you follow", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (community.following.isEmpty()) Text("Open an artist (search for one, or click an artist name in the player) and press Echo Artist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(community.following) { a ->
                    Column(Modifier.width(110.dp).clickable { open(a.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        RemoteImage(a.thumb, Modifier.size(100.dp), CircleShape)
                        Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (community.notifications.isEmpty()) Text("No new releases yet. The app checks when it starts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            community.notifications.forEach { Text("•  $it") }
        }
    }
}

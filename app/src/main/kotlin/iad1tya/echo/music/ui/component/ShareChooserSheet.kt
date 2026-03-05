package iad1tya.echo.music.ui.component

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iad1tya.echo.music.R
import iad1tya.echo.music.utils.OdesliRepository
import kotlinx.coroutines.launch

/**
 * A bottom sheet that lets the user choose how to share a song:
 * - YouTube Music direct link (instant)
 * - Odesli/Songlink universal link (loads asynchronously)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareChooserSheet(
    ytmUrl: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var odesliUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingOdesli by remember { mutableStateOf(true) }

    // Fetch Odesli URL immediately on open
    LaunchedEffect(ytmUrl) {
        isLoadingOdesli = true
        odesliUrl = OdesliRepository.getPageUrl(ytmUrl)
        isLoadingOdesli = false
    }

    fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, null))
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            // Header
            Text(
                text = "Share via",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(Modifier.height(8.dp))

            // Option 1 — YouTube Music
            ListItem(
                headlineContent = {
                    Text(
                        text = "YouTube Music",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                },
                supportingContent = {
                    Text(
                        text = "Direct link to YouTube Music",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                },
                modifier = Modifier
                    .clickable { shareUrl(ytmUrl) }
                    .padding(horizontal = 8.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Option 2 — Odesli / Songlink
            ListItem(
                headlineContent = {
                    Text(
                        text = "All Platforms (Songlink)",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    )
                },
                supportingContent = {
                    if (isLoadingOdesli) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Fetching link…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (odesliUrl != null) {
                        Text(
                            text = "Link for Spotify, Apple Music, and more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "Unavailable — check your connection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.link),
                        contentDescription = null,
                        tint = if (odesliUrl != null) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp),
                    )
                },
                modifier = Modifier
                    .then(
                        if (odesliUrl != null) {
                            Modifier.clickable { shareUrl(odesliUrl!!) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 8.dp),
            )

            // Attribution
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Powered by Odesli / song.link",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }
    }
}

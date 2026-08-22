package echo.music.iad1tya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import echo.music.iad1tya.domain.repository.ListenTogetherRepository
import echo.music.iad1tya.ui.icon.PeopleAlt
import echo.music.iad1tya.ui.icon.echoIcons
import org.koin.compose.koinInject

/**
 * The Listen Together entry in a top app bar, carrying a dot while a room is running.
 *
 * Shared by Home and Library rather than duplicated: the badge is the only thing that says a room
 * is still live once you have navigated away from it, so the two bars must not drift apart.
 */
@Composable
fun ListenTogetherIconButton(
    modifier: Modifier = Modifier,
    repository: ListenTogetherRepository = koinInject(),
    onClick: () -> Unit,
) {
    val room by repository.room.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        RippleIconButton(
            imageVector = echoIcons.PeopleAlt,
            tint = MaterialTheme.colorScheme.onBackground,
            onClick = onClick,
        )
        if (room.inRoom) {
            // A ring of page colour around the dot: without it the dot merges into the icon's own
            // dark shapes and stops reading as a badge.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
            }
        }
    }
}

package echo.music.iad1tya.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.extension.NonLazyGrid
import echo.music.iad1tya.ui.icon.Downloading
import echo.music.iad1tya.ui.icon.Favorite
import echo.music.iad1tya.ui.icon.Insights
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.icon.TrendingUp
import echo.music.iad1tya.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import echo.music.iad1tya.ui.screen.library.LibraryDynamicPlaylistType
import echo.music.iad1tya.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.downloaded
import echomusic.composeapp.generated.resources.favorite
import echomusic.composeapp.generated.resources.followed
import echomusic.composeapp.generated.resources.most_played

@Composable
fun LibraryTilingBox(navController: NavController) {
    val listItem =
        listOf(
            LibraryTilingState.Favorite,
            LibraryTilingState.Followed,
            LibraryTilingState.MostPlayed,
            LibraryTilingState.Downloaded,
        )
    NonLazyGrid(
        columns = 2,
        itemCount = 4,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp, end = 10.dp),
    ) { number ->
        Box(
            Modifier.padding(start = 10.dp, top = 10.dp),
        ) {
            LibraryTilingItem(
                listItem[number],
                onClick = {
                    when (listItem[number]) {
                        LibraryTilingState.Favorite -> {
                            navController.navigate(
                                LibraryDynamicPlaylistDestination(
                                    type = LibraryDynamicPlaylistType.Favorite.toStringParams(),
                                ),
                            )
                        }

                        LibraryTilingState.Followed -> {
                            navController.navigate(
                                LibraryDynamicPlaylistDestination(
                                    type = LibraryDynamicPlaylistType.Followed.toStringParams(),
                                ),
                            )
                        }

                        LibraryTilingState.MostPlayed -> {
                            navController.navigate(
                                LibraryDynamicPlaylistDestination(
                                    type = LibraryDynamicPlaylistType.MostPlayed.toStringParams(),
                                ),
                            )
                        }

                        LibraryTilingState.Downloaded -> {
                            navController.navigate(
                                LibraryDynamicPlaylistDestination(
                                    type = LibraryDynamicPlaylistType.Downloaded.toStringParams(),
                                ),
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun LibraryTilingItem(
    state: LibraryTilingState,
    onClick: () -> Unit = {},
) {
    val title = stringResource(state.title)
    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth().clickable {
                onClick.invoke()
            },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(),
        colors =
            CardDefaults.elevatedCardColors().copy(
                containerColor = state.containerColor,
            ),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                state.icon,
                contentDescription = title,
                modifier =
                    Modifier
                        .size(50.dp)
                        .padding(10.dp),
                tint = state.iconColor,
            )
            Text(
                title,
                style = typo().titleSmall,
                color = Color.Black,
            )
        }
    }
}

data class LibraryTilingState(
    val title: StringResource,
    val containerColor: Color,
    val icon: ImageVector,
    val iconColor: Color,
) {
    companion object {
        val Favorite =
            LibraryTilingState(
                title = Res.string.favorite,
                containerColor = Color(0xffff99ae),
                icon = echoIcons.Favorite,
                iconColor = Color(0xffD10000),
            )
        val Followed =
            LibraryTilingState(
                title = Res.string.followed,
                containerColor = Color(0xffFFEB3B),
                icon = echoIcons.Insights,
                iconColor = Color.Black,
            )
        val MostPlayed =
            LibraryTilingState(
                title = Res.string.most_played,
                containerColor = Color(0xff00BCD4),
                icon = echoIcons.TrendingUp,
                iconColor = Color.Black,
            )
        val Downloaded =
            LibraryTilingState(
                title = Res.string.downloaded,
                containerColor = Color(0xff4CAF50),
                icon = echoIcons.Downloading,
                iconColor = Color.Black,
            )
    }
}
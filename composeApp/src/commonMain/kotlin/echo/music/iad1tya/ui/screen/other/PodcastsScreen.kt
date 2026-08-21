package echo.music.iad1tya.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import echo.music.iad1tya.ui.component.rememberHolderPainter
import echo.music.iad1tya.expect.ui.toImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kmpalette.rememberPaletteState
import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.domain.utils.toSongEntity
import echo.music.iad1tya.domain.utils.toTrack
import echo.music.iad1tya.extension.angledGradientBackground
import echo.music.iad1tya.extension.artworkScrimBrush
import echo.music.iad1tya.extension.getColorFromPalette
import echo.music.iad1tya.ui.component.CenterLoadingBox
import echo.music.iad1tya.ui.component.DescriptionView
import echo.music.iad1tya.ui.component.EndOfPage
import echo.music.iad1tya.ui.component.HeartCheckBox
import echo.music.iad1tya.ui.component.NowPlayingBottomSheet
import echo.music.iad1tya.ui.component.PodcastEpisodeFullWidthItem
import echo.music.iad1tya.ui.component.RippleIconButton
import echo.music.iad1tya.ui.icon.ArrowBackIosNew
import echo.music.iad1tya.ui.icon.PlayCircle
import echo.music.iad1tya.ui.icon.Share
import echo.music.iad1tya.ui.icon.Shuffle
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.navigation.destination.list.ArtistDestination
import echo.music.iad1tya.ui.theme.typo
import echo.music.iad1tya.viewModel.PodcastUIEvent
import echo.music.iad1tya.viewModel.PodcastUIState
import echo.music.iad1tya.viewModel.PodcastViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.album_length
import echomusic.composeapp.generated.resources.no_description
import echomusic.composeapp.generated.resources.podcasts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel = koinViewModel(),
    podcastId: String,
    navController: NavController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }

    var currentTrack by rememberSaveable {
        mutableStateOf<Track?>(null)
    }
    var shouldShowMoreBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }

    // Theo dõi gradient cho background
    var gradientColors by remember { mutableStateOf(listOf(Color.Black, Color.Black)) }

    val paletteState = rememberPaletteState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null) {
            paletteState.generate(bm)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                gradientColors = listOf(it.getColorFromPalette(), Color.Black)
            }
    }

    LaunchedEffect(key1 = podcastId) {
        if ((uiState as? PodcastUIState.Success)?.id == podcastId) {
            return@LaunchedEffect
        }
        viewModel.getPodcastBrowse(podcastId)
    }

    Crossfade(targetState = uiState) { state ->
        when (state) {
            is PodcastUIState.Success -> {
                val data = state.data
                val id = state.id
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black),
                    state = lazyState,
                ) {
                    item(contentType = "header") {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .background(Color.Transparent),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .angledGradientBackground(gradientColors, 25f),
                                )
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(artworkScrimBrush(Color.Black)),
                                )
                            }
                            Column(
                                Modifier.background(Color.Transparent),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .wrapContentWidth()
                                            .padding(16.dp)
                                            .windowInsetsPadding(WindowInsets.statusBars),
                                ) {
                                    RippleIconButton(
                                        imageVector = echoIcons.ArrowBackIosNew,
                                    ) {
                                        navController.navigateUp()
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(data.thumbnail.lastOrNull()?.url)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(data.thumbnail.lastOrNull()?.url)
                                                .crossfade(true)
                                                .build(),
                                        placeholder = rememberHolderPainter(),
                                        error = rememberHolderPainter(),
                                        contentDescription = null,
                                        contentScale = ContentScale.FillHeight,
                                        onSuccess = {
                                            bitmap = it.result.image.toImageBitmap()
                                        },
                                        modifier =
                                            Modifier
                                                .height(250.dp)
                                                .wrapContentWidth()
                                                .align(Alignment.CenterHorizontally)
                                                .clip(RoundedCornerShape(8.dp)),
                                    )
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(),
                                    ) {
                                        Column(Modifier.padding(horizontal = 32.dp)) {
                                            Spacer(modifier = Modifier.size(25.dp))
                                            Text(
                                                text = data.title,
                                                style = typo().titleLarge,
                                                color = Color.White,
                                                maxLines = 2,
                                            )
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model =
                                                            ImageRequest
                                                                .Builder(LocalPlatformContext.current)
                                                                .data(data.authorThumbnail)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .diskCacheKey(data.authorThumbnail)
                                                                .crossfade(true)
                                                                .build(),
                                                        placeholder = rememberHolderPainter(),
                                                        error = rememberHolderPainter(),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier
                                                                .size(25.dp)
                                                                .clip(
                                                                    CircleShape,
                                                                ),
                                                    )
                                                    Spacer(modifier = Modifier.size(8.dp))
                                                    CompositionLocalProvider(
                                                        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                                                    ) {
                                                        TextButton(
                                                            modifier =
                                                                Modifier
                                                                    .wrapContentHeight()
                                                                    .defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
                                                            contentPadding = PaddingValues(vertical = 1.dp),
                                                            onClick = {
                                                                val authorId = data.author.id
                                                                if (authorId.isNullOrEmpty().not()) {
                                                                    navController.navigate(
                                                                        ArtistDestination(
                                                                            authorId,
                                                                        ),
                                                                    )
                                                                }
                                                            },
                                                        ) {
                                                            Text(
                                                                text = data.author.name,
                                                                style = typo().labelSmall,
                                                                color = Color.White,
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.size(8.dp))
                                                Text(
                                                    text = stringResource(Res.string.podcasts),
                                                    style = typo().bodyMedium,
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                // Play button
                                                RippleIconButton(
                                                    imageVector = echoIcons.PlayCircle,
                                                    fillMaxSize = true,
                                                    modifier = Modifier.size(36.dp),
                                                ) {
                                                    viewModel.onUIEvent(PodcastUIEvent.PlayAll(id))
                                                }

                                                // Favorite
                                                HeartCheckBox(
                                                    size = 32,
                                                    checked = isFavorite,
                                                    onStateChange = {
                                                        viewModel.onUIEvent(
                                                            PodcastUIEvent.ToggleFavorite(
                                                                id,
                                                                !isFavorite,
                                                            ),
                                                        )
                                                    },
                                                )

                                                Spacer(Modifier.weight(1f))

                                                // Shuffle
                                                RippleIconButton(
                                                    modifier = Modifier.size(36.dp),
                                                    imageVector = echoIcons.Shuffle,
                                                    fillMaxSize = true,
                                                ) {
                                                    viewModel.onUIEvent(PodcastUIEvent.Shuffle(id))
                                                }

                                                Spacer(Modifier.size(5.dp))

                                                // More options
                                                RippleIconButton(
                                                    modifier = Modifier.size(36.dp),
                                                    imageVector = echoIcons.Share,
                                                    fillMaxSize = true,
                                                ) {
                                                    viewModel.onUIEvent(PodcastUIEvent.Share(id))
                                                }
                                            }

                                            // Description
                                            val uriHandler = LocalUriHandler.current
                                            DescriptionView(
                                                modifier = Modifier.padding(top = 8.dp),
                                                text = data.description ?: stringResource(Res.string.no_description),
                                                limitLine = 3,
                                                onTimeClicked = {},
                                                onURLClicked = { url ->
                                                    uriHandler.openUri(url)
                                                },
                                            )

                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.album_length,
                                                        data.listEpisode.size.toString(),
                                                        "",
                                                    ),
                                                color = Color.White,
                                                style = typo().bodyMedium,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Podcast episodes
                    items(count = data.listEpisode.size, key = { index ->
                        val item = data.listEpisode.getOrNull(index)
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val episode = data.listEpisode.getOrNull(index)
                        if (episode != null) {
                            PodcastEpisodeFullWidthItem(
                                episode = episode,
                                onClick = {
                                    viewModel.onUIEvent(
                                        PodcastUIEvent.EpisodeClick(
                                            episode.videoId,
                                            id,
                                        ),
                                    )
                                },
                                onMoreClickListener = {
                                    currentTrack = episode.toTrack()
                                    shouldShowMoreBottomSheet = true
                                },
                            )
                        }
                    }

                    item {
                        EndOfPage()
                    }
                }
                // Animated TopAppBar when scrolling
                AnimatedVisibility(
                    visible = shouldHideTopBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = data.title,
                                style = typo().titleMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        },
                        navigationIcon = {
                            Box(Modifier.padding(horizontal = 5.dp)) {
                                RippleIconButton(
                                    echoIcons.ArrowBackIosNew,
                                    Modifier.size(32.dp),
                                    true,
                                ) {
                                    navController.navigateUp()
                                }
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        modifier = Modifier.angledGradientBackground(gradientColors, 90f),
                    )
                }

                if (shouldShowMoreBottomSheet) {
                    val song = currentTrack?.toSongEntity() ?: return@Crossfade
                    NowPlayingBottomSheet(
                        onDismiss = { shouldShowMoreBottomSheet = false },
                        navController = navController,
                        song = song,
                    )
                }
            }

            is PodcastUIState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CenterLoadingBox(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                    )
                }
            }

            is PodcastUIState.Error -> {
                viewModel.makeToast("Error: ${state.message}")
                navController.navigateUp()
            }
        }
    }
}
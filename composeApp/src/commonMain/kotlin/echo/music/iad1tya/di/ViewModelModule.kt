package echo.music.iad1tya.di

import echo.music.iad1tya.viewModel.AlbumViewModel
import echo.music.iad1tya.viewModel.AnalyticsViewModel
import echo.music.iad1tya.viewModel.ArtistViewModel
import echo.music.iad1tya.viewModel.HomeViewModel
import echo.music.iad1tya.viewModel.ImportViewModel
import echo.music.iad1tya.viewModel.LibraryDynamicPlaylistViewModel
import echo.music.iad1tya.viewModel.LibraryViewModel
import echo.music.iad1tya.viewModel.LocalPlaylistViewModel
import echo.music.iad1tya.viewModel.LogInViewModel
import echo.music.iad1tya.viewModel.MoodViewModel
import echo.music.iad1tya.viewModel.MoreAlbumsViewModel
import echo.music.iad1tya.viewModel.NotificationViewModel
import echo.music.iad1tya.viewModel.NowPlayingBottomSheetViewModel
import echo.music.iad1tya.viewModel.PlaylistViewModel
import echo.music.iad1tya.viewModel.PodcastViewModel
import echo.music.iad1tya.viewModel.RecentlySongsViewModel
import echo.music.iad1tya.viewModel.SearchViewModel
import echo.music.iad1tya.viewModel.SettingsViewModel
import echo.music.iad1tya.viewModel.SharedViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        single {
            SharedViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single {
            SearchViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            NowPlayingBottomSheetViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            ImportViewModel(
                get(),
            )
        }
        viewModel {
            AlbumViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            HomeViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            SettingsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ArtistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            PlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LogInViewModel(
                get(),
            )
        }
        viewModel {
            PodcastViewModel(
                get(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                get(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                get(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            NotificationViewModel(
                get(),
            )
        }
        viewModel {
            MoodViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            AnalyticsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
    }
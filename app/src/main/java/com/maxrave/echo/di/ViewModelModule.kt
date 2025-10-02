package iad1tya.echo.music.di

import androidx.media3.common.util.UnstableApi
import com.maxrave.echo.service.LanguageDownloadManager
import com.maxrave.echo.service.TranslationService
import iad1tya.echo.music.viewModel.AlbumViewModel
import iad1tya.echo.music.viewModel.ArtistViewModel
import iad1tya.echo.music.viewModel.HomeViewModel
import iad1tya.echo.music.viewModel.LibraryDynamicPlaylistViewModel
import iad1tya.echo.music.viewModel.LibraryViewModel
import iad1tya.echo.music.viewModel.LocalPlaylistViewModel
import iad1tya.echo.music.viewModel.LogInViewModel
import iad1tya.echo.music.viewModel.MoodViewModel
import iad1tya.echo.music.viewModel.MoreAlbumsViewModel
import iad1tya.echo.music.viewModel.NotificationViewModel
import iad1tya.echo.music.viewModel.NowPlayingBottomSheetViewModel
import iad1tya.echo.music.viewModel.PlaylistViewModel
import iad1tya.echo.music.viewModel.PodcastViewModel
import iad1tya.echo.music.viewModel.RecentlySongsViewModel
import iad1tya.echo.music.viewModel.SearchViewModel
import iad1tya.echo.music.viewModel.SettingsViewModel
import iad1tya.echo.music.viewModel.SharedViewModel
import iad1tya.echo.music.viewModel.WelcomeViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val viewModelModule =
    module {
        // Services
        single<LanguageDownloadManager> {
            LanguageDownloadManager(get())
        }
        
        single<TranslationService> {
            TranslationService(get())
        }
        
        single {
            SharedViewModel(
                androidApplication(),
            )
        }
        single {
            SearchViewModel(
                application = androidApplication(),
            )
        }
        single {
            NowPlayingBottomSheetViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            LibraryViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            AlbumViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            HomeViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            SettingsViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            ArtistViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            PlaylistViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            LogInViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            PodcastViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                application = androidApplication(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                androidApplication(),
            )
        }
        viewModel {
            NotificationViewModel(
                androidApplication(),
            )
        }
        viewModel {
            MoodViewModel(
                androidApplication(),
            )
        }
        viewModel {
            WelcomeViewModel(
                androidApplication(),
            )
        }
    }
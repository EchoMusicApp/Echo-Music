Echo Music v5.2.6

- Added AI-powered lyrics translation via Puter.com, with a redesigned "Ai" icon now placed under Account in Settings for quicker access.
- Added a "Recommended by AI" playlist that builds and updates automatically from listening history, shown on the Home Screen with a last-updated timestamp.
- Added the ability to create custom AI playlists from the Library using text prompts (e.g. "Upbeat workout pop").
- Added an AI playlist modification tool to edit existing playlists using text prompts.
- Added a global haptics option in Appearance settings for feedback during interactions and scrolling.
- Added "Lossless" as a music provider on the Service Uptime screen, with a "Contribute in Lossless" option in the Account Dialog.
- Added support for importing public Spotify playlists via link, without requiring login.
- Improved the AI Playlist Generator to follow prompts more accurately, correctly matching specified eras, release years, and artists.
- Improved AI Recommendations to stop creating duplicate daily playlists, now refreshing into a single persistent playlist.
- Improved audio quality switches (e.g. Standard to Lossless) to apply immediately instead of using a cached stream format.
- Switched the Lossless audio source from Qobuz to a new GitHub-based index.
- Fixed a compilation error from a missing bracket in PlayerSettings and a missing Intent import in App.kt.
- Fixed an issue where internal volume could get stuck at a lowered level after restoring playback state.
- Fixed the Refetch button in menus to properly clear offline caches before fetching fresh audio.
- Removed JioSaavn and Saavn 320kbps quality options due to server inconsistency.
- Removed Echo Brain and all associated data collection modules.
- Removed the "Charts" section from the Search screen.

Contributors this release: @jester-sys (fixed a database migration crash on the 31 to 32 upgrade), @iamkaleemsajjad-hue (fixed ANRs from main-thread blocking in MusicService, reviewed by @nyxgoober), and translators @champinondev-lab, @Mickael81, @Datvex, @seblopxz, and @k25jura via Hosted Weblate.

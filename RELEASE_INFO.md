Echo Music v5.2.4

New Features
- Added a floating import button on the Library screen to import playlists from Spotify or YouTube Music via URL
- Added Apple Music-style dynamic zoom and fade transition for album art on song change
- Added a dynamic crossfade indicator with a shining animation to show when songs are crossfading
- Added a Refetch button in the player menu to manually reload songs in Opus format
- Added ability for Listen Together hosts to toggle participant music control, now as its own settings card
- Indexed all missing settings across the app into search, so every setting can be found and navigated to

Improvements
- Moved JioSaavn API server configuration to a remote JSON file for dynamic load balancing and updates without needing an app update
- Restored JioSaavn as an audio and download quality option
- Refreshed the Liked playlist screen to match the gradient blur background used on online playlists, with a transparent top nav bar and redundant titles removed
- Removed the volume slider from player menus for a cleaner look
- Removed extra spacing and the horizontal border above player menu action buttons
- Updated settings UI with new custom vector icons for Last.fm, ListenBrainz, and Echo Brain
- Redesigned the ListenBrainz icon into a clean, typographic "LB" style
- Updated the Music Recognizer Quick Settings tile to use the Echo Music logo instead of the default microphone
- Implemented intelligent WebView renderer recovery to prevent hangs and "zombie" states on low-memory devices during streaming

Bug Fixes
- Fixed the missing "Set as Ringtone" option in the player menu
- Fixed Unison lyrics rendering raw TTML XML instead of proper synced text
- Fixed compilation errors in the Listen Together screen and player menu
- Fixed settings search navigating to the wrong highlight, including settings with spaces in their name; added a proper visual highlight and improved scroll behavior to center the setting on screen
- Fixed a touch ripple effect bleeding out of bounds when selecting a song in lists
- Fixed songs liked on YouTube Music failing to sync to Echo Music due to missing playlist video IDs
- Fixed Echo Music repeatedly overwriting the liked date of songs during sync, which now preserves local sorting and prevents accidental re-uploads to YouTube Music
- Fixed an issue affecting liked playlist songs
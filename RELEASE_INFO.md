# Echo Music v5.2.1 Changelog

## New Features
- Added the ability to export playlists as CSV files directly from the playlist menu.

## Bug Fixes
- Fixed Spotify login / import failing with a HTTP 403 API Rate Limit Exceeded error by fetching TOTP secrets directly from raw gist content.
- Fixed a playback crash (NumberFormatException) when attempting to play local music files.

## Enhancements
- Added a toast notification when a song is successfully added to a playlist.
- Added fallback to apply custom playlist cover locally if YouTube upload fails (e.g. 403 error).
- Changed the floating navigation three-dot button to use the dynamic primary color palette instead of a fixed pink color.
- Made the floating navigation toolbar items adapt to the color palette instead of being transparent in AMOLED mode.
- Fixed an issue where the navigation bar would slightly stretch and bounce to the right when switching tabs due to the background pill animation lagging.
- Moved the music recognition button from the overflow menu into the main bottom navbar (between Search and Library).
- Added a Settings shortcut to the overflow menu to replace the music recognition button, which navigates directly to the full Settings screen.
- Disabled page name labels in the bottom navbar for a cleaner look.
- Hid the miniplayer on the Settings screen for a less cluttered view.

## Bug Fixes
- Fixed an issue where the bottom navigation bar would shake or bounce left and right when switching between tabs due to the text expansion animation.

# Echo Music v5.1.6

## New Features

- Implemented a new Export as MP3 feature that downloads, transcodes via FFmpeg, and embeds ID3 metadata (including album art) into local MP3 files directly from the player and song menus.
- Added a new Exported auto-playlist in the Library to easily filter and view all exported MP3 songs, with visibility toggles in Appearance Settings.
- Added full changelog support to the System Update screen. Update notes and descriptions are now fully rendered with Markdown support (bold, italics, code blocks, and clickable links).
- Added a new setting in Appearance Settings to show audio codec information centrally below the player timeline.
- Added an option in Appearance Settings to hide the volume slider, specifically available when the Apple Music Inspired UI is active.
- Added full Persian (Farsi) translation. Thanks to @mcuteangel.

## Changes

- Replaced Qobuz with JioSaavn (320kbps) for streaming and downloading.
- Enabled Crossfade support specifically for Saavn (320kbps) streams.
- Crossfade is now automatically disabled when Qobuz (Lossless) audio quality is selected; the toggle becomes unclickable with an explanatory message.
- Migrated default Listen Together server to Echo Music Server on Hugging Face (`wss://iad1tya-echomusic.hf.space/ws`). To modify or verify the URL, navigate to Listen Together > Settings > Server URL.
- Updated the audio codec display to show codec name, bitrate in kbps, and lossless indicator.
- Adjusted the audio codec display to align horizontally with the music playback timestamps.
- The playback queue is now unlocked for rearrangement by default.
- The Quick Picks carousel is now always pinned to the top of the Home Screen.
- Updated the previous and next player buttons to maintain a consistent translucent white styling across all themes.
- Unlocked the player background style setting, allowing users to customize their background even when the Apple Music Inspired UI is enabled.
- Repositioned the Listen Together connection controls to the top app bar.
- Moved the Listen Together usage guide below the settings card.
- Revamped the "How to use Listen Together" instructions with a clean, numbered list within a Material 3 card.
- Moved "Import from Spotify" back to the Import page inside Backup & Restore settings, placing it above the local file import option.
- Removed FLAC codec information display (sample rate, bit depth) from the Now Playing screen.
- Removed the disc icon from the album title on the Album screen.

## Bug Fixes

- Fixed an issue where the canvas videos were being stretched on the album, artist, and square player screens by properly applying a zoom crop to fit the shapes.
- Fixed an issue where locally liked songs were incorrectly removed from the library instead of being pushed to the YouTube server during cloud sync.
- Fixed random playback pauses by clearing ghost cache entries and removing unnecessary retry delays.
- Fixed an issue where restoring from a manual zip backup could cause database corruption and app crashes by explicitly clearing old WAL cache files during restore.
- Fixed a bug where the player would show active playback with no audio when reopening from active apps, caused by an out-of-bounds queue index during state restoration.
- Prevented text overflow on the Subscribe, Radio, and Shuffle buttons within the Artist screen.
- Prevented potential data wipes on future app updates by safely disabling destructive Room database migrations.
- Improved parsing for localized artist statistics, properly extracting subscriber and monthly listener counts when APIs return text mixed with numbers across different languages.
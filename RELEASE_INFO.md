# Echo Music v5.1.9

- Added a search bar to the Settings screen to allow quick filtering of settings categories and individual sub-settings (e.g., Theme, Cache, Quality).
- Improved update downloading by running it as a background service with auto-retry and fixed notification spam.
- Removed the comments button from the Listen Together screen.
* Switched from ffmpeg-kit-full to ffmpeg-kit-audio to reduce native library overhead.
* Fixed ABI filters for individual architectural builds (arm64, armeabi, x86, x86_64) to prevent them from building universal APKs.
* Removed unused aria2c native dependency from YouTube downloader to further reduce app size.
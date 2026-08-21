# Echo Music v5.2.89

### New Features & Improvements
- **Settings Overhaul**: Redesigned the Account and Settings overflow menu into a modern Material You 3 bottom sheet for a more expressive look.
- **AI Hub Shortcut**: Integrated the AI Hub directly into the bottom navigation bar for quicker access.
- **Better Settings Search**: Added descriptive text to all top-level and nested settings to clarify their purpose, and improved the settings search engine to properly index and search by these descriptions.
- **Ambient Mode Navigation**: Added a back button overlay to the Ambient Mode screen for easier navigation out of landscape mode.
- **Network Resilience**: Changed the default network IP version to Automatic for better compatibility.

### Bug Fixes
- **Lyrics Rendering**: Fixed a bug where the Apple Music V2 (Letter by Letter) lyrics animation would break apart combining characters in non-English languages (like Hindi and Punjabi) into dotted circles. The engine now properly groups grapheme clusters.

### Community Contributions
- `chore(l10n): update translations` ([#999](https://github.com/EchoMusicApp/Echo-Music/pull/999)) by @weblate
- `fix: guard lyrics blur RenderEffect behind API 31 check` ([#992](https://github.com/EchoMusicApp/Echo-Music/pull/992)) by @berruetaa
- `fix: Happens when refetching a song's lyrics` ([#1014](https://github.com/EchoMusicApp/Echo-Music/pull/1014)) by @Chetan786
- `fix(cast): throttle volume slider, add thumbnail, remove dead code` ([#1024](https://github.com/EchoMusicApp/Echo-Music/pull/1024)) by @Hitomatito
- `fix(cast): fix queue lifecycle — looping, race condition, and bidirectional extension` ([#1031](https://github.com/EchoMusicApp/Echo-Music/pull/1031)) by @Hitomatito

---

### Important Notice Regarding Lossless Music
We have completely removed lossless music streaming, downloads, and tracking features. Maintaining the lossless music database is expensive and quite difficult. Soon, the entire lossless database will be archived from GitHub as well. 

We tried our best to maintain it, and while many of you have asked us to use a free cloud service instead, the process requires automation (where a user can upload a track and it gets added automatically). Doing this manually for every track simply isn't viable. Thank you for understanding.

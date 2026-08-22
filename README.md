<div align="center">
  <img src="assets/Echo-new.png" alt="Echo Music Logo" width="120"/>

  <h1>Echo Music</h1>

  <p><b>A modern Android music app with streaming, synced lyrics, offline playback, and an intuitive user experience.</b></p>
  <p>
    <a href="https://buymeacoffee.com/iad1tya">Buy me a Coffee</a> •
    <a href="https://support.iad1tya.cyou">Support</a> •
    <a href="https://instagram.com/iad1tya">Instagram</a> •
    <a href="https://x.com/xad1tya">X</a>
  </p>
</div>

## Features

* High-quality audio streaming (up to 256kbps for supported accounts).
* Browse charts, podcasts, moods, and genres.
* Comprehensive search functionality across the music catalog.
* Playback data analytics and automated custom playlists.
* Video playback support (1080p with subtitles).
* Artificial Intelligence based song suggestions.
* Crossfade and gapless playback capabilities.
* Customizable application themes (Light, Dark, and dynamic colors).
* Sleep timer functionality.
* Android Auto integration for in-car listening.
* Support for Spotify Canvas visualizations.

## Architecture

Echo Music is built utilizing a modern Android and Kotlin Multiplatform (KMP) architecture to ensure scalability, maintainability, and high performance.

* **Kotlin Multiplatform (KMP):** The core business logic, domain models, and data access layers are encapsulated within a dedicated `core` Git submodule. This enables logic sharing across platforms and isolates critical services.
* **UI Layer:** The application interface is built entirely with Jetpack Compose, offering a reactive and declarative UI paradigm.
* **Media Playback:** Playback is handled by AndroidX Media3 (ExoPlayer), providing robust handling of audio streams, local caching, and gapless transitions.
* **Dependency Injection:** Koin is utilized for dependency injection, decoupling module lifecycles and simplifying testing.
* **Local Storage:** Room Database manages structured local data (playlists, favorites, cache metadata) while DataStore manages user preferences.
* **Modularization:** The project is strictly modularized by feature and layer (e.g., `:core:data`, `:core:domain`, `:core:media3`, `:core:service:spotify`, `:core:service:lyricsService`). This structure reduces build times and enforces clear boundary separations.

## Infrastructure and Analytics

* **Firebase Integration:** Echo Music utilizes Firebase Crashlytics for real-time crash reporting and Firebase Analytics to monitor application performance and usage metrics. This telemetry data is critical for maintaining app stability and guiding future improvements.
* **Monetization:** To sustain the infrastructure, development, and maintenance costs associated with this project, minimal advertisements are integrated within the application. 

## Acknowledgements

A special acknowledgment to the [SimpMusic](https://github.com/maxrave-dev/SimpMusic) project. Echo Music leverages the stable and reliable foundation established by SimpMusic. We are deeply grateful to the SimpMusic developers for their exceptional open-source contributions, upon which we continue to build and innovate.

## Installation

Download the latest pre-compiled APK from the [Releases Page](https://github.com/iad1tya/Echo-Music/releases/latest).

## Support

If you find Echo Music valuable, please consider supporting the development infrastructure:

<div align="center">
  <a href="https://buymeacoffee.com/iad1tya"><img src="assets/bmac.png" width="140" style="margin: 10px; border-radius: 8px;"/></a>
  <a href="https://intradeus.github.io/http-protocol-redirector/?r=upi://pay?pa=iad1tya@upi&pn=Aditya%20Yadav"><img src="assets/upi.svg" width="100" style="margin: 10px; border-radius: 8px;"/></a>
  <a href="https://www.patreon.com/cw/iad1tya"><img src="assets/patreon3.png" width="100" style="margin: 10px; border-radius: 8px;"/></a>
</div>

<details>
<summary><b>Cryptocurrency Options</b></summary>
<br>

| Network | Address |
| :--- | :--- |
| **Bitcoin** | `bc1qcvyr7eekha8uytmffcvgzf4h7xy7shqzke35fy` |
| **Ethereum** | `0x51bc91022E2dCef9974D5db2A0e22d57B360e700` |
| **Solana** | `9wjca3EQnEiqzqgy7N5iqS1JGXJiknMQv6zHgL96t94S` |

</details>

## License

Echo Music is licensed under the GPL-3.0 License. See the LICENSE file for details.

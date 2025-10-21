# Changelog

All notable changes to Echo Music will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.1] - 2025-10-22

### Fixed
• Audio routing features for greater flexibility and control 
• Widgets for quick access and improved usability 
• Major bug fixes improving overall stability 
• Enhanced performance for a smoother, more responsive experience

## [2.0] - 2025-10-20

### Changed
- **🎯 Streamlined Integration**
  - Removed Discord RPC integration for cleaner codebase
  - Removed Last.fm scrobbling integration
  - Removed Spotify integration references
  - Focused exclusively on YouTube Music streaming

### Added
- **🔥 Firebase Analytics & Crashlytics**
  - Integrated Firebase for better app stability monitoring
  - Added crash reporting for improved debugging
  - Privacy-focused analytics implementation

### Improved
- **📱 Enhanced User Experience**
  - Cleaner settings interface without external integrations
  - Simplified app architecture
  - Better performance with reduced dependencies

## [1.8.1] - 2025-01-27

### Changed
- **🔧 Settings Improvements**
  - Hidden the "Fetch Home Data Limit" setting from the settings screen
  - Removed the "Buy Me a Coffee" button from the About Echo section
  - Improved settings UI organization and user experience

### Fixed
- **🎨 UI/UX Enhancements**
  - Cleaned up settings interface by removing unused options
  - Streamlined About Echo section for better user experience

## [1.8.0] - 2025-10-12

### Added
- **🎥 Video/Audio Playback Toggle**
  - Seamlessly switch between video and audio modes for video tracks
  - Smart switching logic that maintains playback position
  - Compact, animated toggle button with visual feedback
  - Automatic surface management for optimal video rendering

- **📋 Enhanced Playlist Management**
  - Long-press context menu on playlists in library
  - Quick actions: Play, Shuffle, Add to Queue, Share, Rename, Delete
  - Support for both local and YouTube playlists
  - Direct playlist actions without opening the playlist

- **🔥 Firebase Integration**
  - Google Analytics for usage insights and engagement tracking
  - Firebase Crashlytics for comprehensive crash reporting
  - Privacy-first implementation with user opt-out support
  - Detailed analytics for music playback patterns

### Fixed
- **🐛 Critical Crash Fixes**
  - Fixed DownloadService JobScheduler crash caused by ProGuard obfuscation
  - Fixed ForegroundServiceStartNotAllowedException on Android 12+ devices
  - Fixed ActivityNotFoundException when voice search is not available on device
  - Fixed IndexOutOfBoundsException in queue management when adding tracks
  - Fixed video player black screen issue after app restart
  - Fixed song title disappearance in player UI
  
### Changed
- **🔒 Stability Improvements**
  - Added comprehensive ProGuard keep rules for Media3/ExoPlayer services
  - Improved error handling with graceful fallbacks
  - Added bounds checking for queue operations
  - Enhanced user feedback with helpful error messages
  - Optimized video player surface initialization
  - Improved memory management for video playback

### Technical
- Updated ProGuard rules to prevent service obfuscation
- Added Android 12+ foreground service permission handling
- Implemented defensive programming for queue index operations
- Added try-catch blocks for missing system components
- Implemented smart video/audio switching with surface ready callbacks
- Enhanced ExoPlayer integration for seamless media transitions

## [1.7.4] - 2024-12-XX

### Changed
- Version bump to v1.7.4

## [1.7.2] - 2024-12-XX

### Added
- **📊 Firebase Analytics & Crashlytics Integration**
  - Comprehensive analytics tracking for user engagement
  - Music-specific event tracking (play, pause, skip, playlist operations)
  - Crash reporting and debugging utilities
  - Performance monitoring and memory usage tracking
  - Privacy-compliant data collection with user opt-out support
  - Detailed documentation and implementation guide

- **🔧 Performance Optimizations**
  - Improved app responsiveness and smooth animations
  - Optimized memory usage and resource management
  - Enhanced error handling and recovery mechanisms
  - Better build configuration and dependency management

### Changed
- **🏗️ Build System**
  - Updated Firebase dependencies to latest stable versions
  - Improved build configuration for both FOSS and Full variants
  - Enhanced security with proper keystore management
  - Better development and production environment separation

### Fixed
- **🐛 Stability Improvements**
  - Fixed various compilation issues and build errors
  - Improved error handling throughout the application
  - Enhanced crash recovery mechanisms
  - Better memory management and leak prevention

### Security
- **🔒 Enhanced Security**
  - Removed sensitive information from repository
  - Proper keystore and API key management
  - Secure Firebase configuration handling
  - Comprehensive .gitignore for sensitive files

## [1.7.0] - 2025-01-XX

### Added
- **🌐 Lyrics Translation Feature**
  - Built-in Google ML Kit translation service
  - On-device translation for privacy
  - Support for multiple languages
  - Translation accuracy varies by content type (60-90%)
  - Accessible through Settings > Lyrics section
  - Works offline without internet connection

- **🎨 UI/UX Improvements**
  - Enhanced spacing between sections
  - Improved color scheme for better visibility
  - Professional dark color palette
  - Better font sizing consistency
  - Optimized layout for various screen sizes

### Changed
- **🎵 Home Screen Layout**
  - Improved spacing between "Moods & Moment" and "Genre" sections
  - Better visual hierarchy
  - Enhanced card dimensions and padding
  - Consistent color scheme across sections

- **🔧 Build Configuration**
  - Updated app version to v1.7
  - Enhanced Firebase configuration
  - Improved build system stability

### Fixed
- **🐛 Bug Fixes**
  - Fixed font scaling issues across different devices
  - Resolved color consistency problems
  - Improved text visibility with proper contrast
  - Fixed layout spacing inconsistencies

## [1.6.3] - 2024-12-XX

### Added
- **🎧 Bit Perfect Playback Support**
  - USB DAC detection and configuration
  - Automatic device compatibility checking
  - Bit-perfect audio output for audiophiles
  - Smart audio processing control
  - High-resolution audio support

- **📊 Enhanced Analytics & Crash Reporting**
  - Firebase Analytics integration
  - Firebase Crashlytics integration
  - User-controlled privacy settings
  - Performance monitoring
  - Custom event tracking

- **🎨 Performance Optimizations**
  - Smooth animations and transitions
  - Jitter-free scrolling experience
  - Optimized LazyList performance
  - Enhanced navigation animations
  - Improved memory management

## [1.5.0] - 2024-11-XX

### Added
- **🎧 Bit Perfect Playback Support**
  - USB DAC detection and configuration
  - Automatic device compatibility checking
  - Bit-perfect audio output for audiophiles
  - Smart audio processing control
  - High-resolution audio support

- **📊 Enhanced Analytics & Crash Reporting**
  - Firebase Analytics integration
  - Firebase Crashlytics integration
  - User-controlled privacy settings
  - Performance monitoring
  - Custom event tracking

- **🎨 Performance Optimizations**
  - Smooth animations and transitions
  - Jitter-free scrolling experience
  - Optimized LazyList performance
  - Enhanced navigation animations
  - Improved memory management

- **🔧 Settings Improvements**
  - Reorganized lyrics settings with smart defaults
  - Conditional visibility for lyrics providers
  - Enhanced storage visualization with proper labels
  - Improved settings UI organization

### Changed
- **Login Experience**
  - Removed "login failed" popup for cleaner UX
  - Only shows "login success" confirmation
  - Improved YouTube login flow

- **Storage Management**
  - Fixed Spotify Canvas Cache label in storage legend
  - Improved storage visualization
  - Better cache management

### Technical Improvements
- **Audio Processing**
  - Enhanced ExoPlayer configuration
  - Improved audio sink handling
  - Better USB audio device support
  - Optimized audio processing pipeline

- **Build System**
  - Updated to version 1.5 (version code 8)
  - Enhanced build configurations
  - Improved dependency management
  - Better error handling

### Security
- **Privacy Enhancements**
  - User-controlled analytics
  - Optional crash reporting
  - Enhanced data privacy controls
  - Secure configuration management

## [Unreleased]

### Planned
- Enhanced AI-powered recommendations
- Improved offline playback experience
- Advanced playlist management features
- Cross-platform synchronization
- Enhanced accessibility features

## [2.0.0] - 2025-01-XX

### Added
- **Comprehensive Documentation**
  - Complete architecture documentation
  - Detailed API integration guide
  - Comprehensive features documentation
  - Troubleshooting guide for common issues
  - Development roadmap for future releases

- **Enhanced User Experience**
  - Improved Material Design 3 implementation
  - Better typography and visual hierarchy
  - Enhanced empty state handling
  - Improved navigation and user flow

- **Advanced Features**
  - AI-powered song recommendations
  - Enhanced lyrics support with multiple providers
  - Improved offline download management
  - Advanced audio quality settings

### Changed
- **Documentation Overhaul**
  - Complete rewrite of README.md with professional structure
  - Enhanced CONTRIBUTING.md with detailed guidelines
  - Comprehensive SETUP.md with step-by-step instructions
  - Updated PRIVACY_POLICY.md with detailed privacy information

- **Code Quality Improvements**
  - Enhanced error handling and state management
  - Improved code organization and structure
  - Better separation of concerns
  - Enhanced testing coverage

### Fixed
- **Stability Issues**
  - Resolved memory leaks and performance issues
  - Fixed crash scenarios and edge cases
  - Improved error handling and recovery
  - Enhanced app stability and reliability

- **UI/UX Issues**
  - Fixed layout issues and visual inconsistencies
  - Improved accessibility and usability
  - Enhanced responsive design
  - Better dark mode implementation

### Technical Improvements
- **Architecture Enhancements**
  - Improved MVVM implementation
  - Better dependency injection with Koin
  - Enhanced repository pattern usage
  - Improved state management with StateFlow

- **Performance Optimizations**
  - Reduced memory usage and battery consumption
  - Improved app startup time
  - Enhanced caching mechanisms
  - Better network optimization

## [1.2] - 2024-12-19

### Added
- **Spotify Playlist Import Feature**
  - Added Spotify login flow for playlist import functionality
  - Created `SpotifyImportScreen` with playlist URL input and preview
  - Added `SpotifyImportViewModel` for handling import logic
  - Integrated Spotify import option in Library screen
  - Added navigation support for Spotify import flow
  - Implemented login status checking and user authentication

- **Empty State Handling**
  - Added comprehensive empty state messages for all library sections
  - Created `EmptyStateMessage` composable for consistent empty state display
  - Added helpful guidance messages for each empty section:
    - "No Favorite Songs" with subtitle "Add songs to favorites to see them here"
    - "No Downloaded Songs" with subtitle "Download songs to listen offline"
    - "No Followed Artists" with subtitle "Start following artists to see them here"
    - "No Most Played Songs" with subtitle "Play songs to see your most played tracks"

- **Resource Loading State**
  - Added `Loading` state to the `Resource` sealed class
  - Updated all ViewModels to handle the new `Resource.Loading` state
  - Improved error handling and state management across the app

### Changed
- **License Compliance**
  - Updated LICENSE file from MIT to GPL-3.0 to comply with SimpMusic's license
  - Added proper attribution and modification notices to all source files
  - Updated README.md with fork acknowledgment and modifications section
  - Added modification notices to core library files (`Ytmusic.kt`, `YouTube.kt`)

- **UI/UX Improvements**
  - **Typography System**: Increased font sizes globally for better readability
    - `titleSmall`: 13sp → 14sp
    - `titleMedium`: 18sp → 20sp
    - `titleLarge`: 25sp → 26sp
    - `bodySmall`: 11sp → 12sp
    - `bodyMedium`: 13sp → 14sp
    - `bodyLarge`: 18sp → 18sp (maintained)
    - `displayLarge`: 20sp → 22sp
    - `headlineMedium`: 20sp → 22sp
    - `headlineLarge`: 23sp → 24sp
    - `labelMedium`: 16sp → 16sp (maintained)
    - `labelSmall`: 14sp → 14sp (maintained)
    - Added `headlineSmall`: 20sp

  - **Echo Branding**: Increased "Echo" text size in top app bar (30sp)
  - **Library Filter Buttons**: Updated font size to 18sp to prevent text wrapping
  - **Album Screen**: Added large green play button (72dp) positioned on the right side
  - **About Section**: Changed "Author" to "Modified by" in settings
  - **Credit Screen**: Updated attribution text to "Modified by iad1tya"

- **Widget Improvements**
  - **Song Poster Display**: Fixed widget poster display for YouTube videos and general media
  - **Robust Image Loading**: Implemented multiple fallback URLs for YouTube thumbnails
  - **HTTP to HTTPS Conversion**: Ensured all image URLs use HTTPS for security
  - **Play/Pause State**: Fixed poster disappearing on pause/resume
  - **Song Tracking**: Added SharedPreferences to track last played song ID
  - **Aggressive Image Loading**: Implemented retry mechanisms and fallback strategies

- **Title Display Fix**
  - Fixed "Downloads" button showing "Downloaded" in top bar
  - Updated `LibraryDynamicPlaylistType.name()` to return correct string resource

- **Empty State Centering**
  - Moved empty state messages to center of screen (both horizontally and vertically)
  - Removed background cards and icons for cleaner, minimal design
  - Used Box layout for perfect centering instead of LazyColumn positioning

### Fixed
- **Build Configuration**
  - Created `local.properties` file with proper Android SDK path
  - Added minimal `google-services.json` for FOSS debug build
  - Fixed package name configuration for debug builds
  - Resolved compilation errors related to missing Resource.Loading state

- **Widget Poster Issues**
  - Fixed song poster not showing on home screen widget
  - Resolved poster disappearing when pausing/resuming songs
  - Fixed inconsistent poster display for YouTube videos
  - Implemented proper image loading with fallback mechanisms

- **Navigation and UI**
  - Fixed Spotify import navigation flow
  - Resolved compilation errors in ViewModels
  - Fixed smart cast issues in playlist description handling
  - Corrected dependency injection for SpotifyImportViewModel

### Technical Improvements
- **Code Quality**
  - Added proper error handling for image loading
  - Implemented coroutine-based image loading with retry logic
  - Added comprehensive logging for debugging widget issues
  - Improved state management with proper Resource handling

- **Performance**
  - Optimized widget update frequency to prevent conflicts
  - Reduced aggressive delayed updates for better performance
  - Implemented controlled image loading with proper cancellation

- **Security**
  - Ensured all image URLs use HTTPS
  - Added proper API key management for Google Services

### Configuration Updates
- **Google Services**
  - Updated to use Firebase project configuration
  - Configured for analytics and crash reporting (optional)

- **Version Management**
  - Updated app version to v1.2
  - Incremented version code to 3
  - Updated version display throughout the app

## [1.1] - Previous Version
- Initial fork from SimpMusic
- Basic Echo Music branding and package name changes
- Initial customizations and modifications

---

## Development Notes

### Key Features Added in v1.2
1. **Spotify Integration**: Complete playlist import functionality with authentication
2. **Enhanced Empty States**: User-friendly messages instead of blank screens
3. **Improved Typography**: Better readability with increased font sizes
4. **Widget Reliability**: Robust song poster display with fallback mechanisms
5. **License Compliance**: Proper GPL-3.0 compliance with attribution

### Technical Debt Addressed
- Fixed all compilation errors related to Resource state handling
- Resolved widget image loading inconsistencies
- Improved error handling and state management
- Enhanced build configuration and dependency management

### User Experience Improvements
- Better visual hierarchy with improved typography
- Clearer empty state guidance
- More reliable widget functionality
- Consistent branding and attribution

---

*This changelog documents all significant changes made to Echo Music v1.2, including new features, bug fixes, and technical improvements.*

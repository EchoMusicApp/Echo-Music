# Contributing to Echo Music

Thank you for your interest in contributing to Echo Music! This document provides comprehensive guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Handling Sensitive Information](#handling-sensitive-information)
- [Contributing Guidelines](#contributing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Documentation](#documentation)
- [Release Process](#release-process)
- [Community Guidelines](#community-guidelines)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct:

### Our Pledge

We are committed to providing a welcoming and inspiring community for all. We pledge to:

- Be respectful and inclusive
- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Accept constructive criticism gracefully
- Focus on what's best for the community
- Show empathy towards other community members

### Expected Behavior

- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior

- The use of sexualized language or imagery
- Trolling, insulting/derogatory comments, and personal or political attacks
- Public or private harassment
- Publishing others' private information without explicit permission
- Other conduct which could reasonably be considered inappropriate in a professional setting

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK 26 or later
- Git
- Basic knowledge of Kotlin and Android development
- Understanding of Jetpack Compose (preferred)
- Familiarity with MVVM architecture pattern

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Echo-Music.git
   cd Echo-Music
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/EchoMusicApp/Echo-Music.git
   ```

## Development Setup

### 1. Environment Setup

1. **Install Android Studio** with the latest Android SDK
2. **Set up local.properties**:
   ```bash
   cp local.properties.template local.properties
   ```
   Edit `local.properties` and add your Android SDK path:
   ```properties
   sdk.dir=/path/to/your/Android/sdk
   ```
   
   > **IMPORTANT**: Never commit your `local.properties` file to the repository as it contains local configuration specific to your development environment. The `.gitignore` file is configured to exclude this file.

### 2. Firebase Setup (Optional)

If you want to test Firebase features:

1. Create a Firebase project
2. Add Android apps with package names:
   - `echo.music.iad1tya` (release)
   - `echo.music.iad1tya.debug` (debug)
3. Copy the template and configure it with your Firebase credentials:
   ```bash
   cp app/google-services.json.template app/google-services.json
   ```
4. Edit `app/google-services.json` with your Firebase project details

> **IMPORTANT**: Never commit your actual `google-services.json` file to the repository as it contains sensitive API keys. The `.gitignore` file is configured to exclude this file.

## Handling Sensitive Information

When contributing to Echo Music, it's crucial to handle sensitive information properly:

### Files That Should Never Be Committed

1. **local.properties**
   - Contains your local SDK path
   - Use the provided template (`local.properties.template`) instead

2. **google-services.json**
   - Contains Firebase API keys and project credentials
   - Use the provided template (`app/google-services.json.template`) instead

3. **Build outputs**
   - Never commit `.apk`, `.aab`, `.class`, or other build artifacts
   - These are automatically excluded by `.gitignore`

4. **IDE-specific files**
   - Most `.idea/` directory contents should not be committed
   - Only commit IDE configuration files that are essential for project setup

### Best Practices

- **API Keys**: Never hardcode API keys in the source code
- **Credentials**: Never include usernames, passwords, or tokens in commits
- **Personal Information**: Remove any personal information before committing
- **Before Committing**: Always review your changes to ensure no sensitive information is included
- **If Accidentally Committed**: If you accidentally commit sensitive information, contact a project maintainer immediately

### 3. Build the Project

```bash
./gradlew assembleDebug
```

### 4. Security Considerations

**IMPORTANT**: Never commit sensitive files to the repository:

- `google-services.json` - Contains Firebase API keys
- `local.properties` - Contains local development paths
- `*.keystore` / `*.jks` - App signing keys
- `secrets.properties` - API keys and secrets
- `**/assets/po_token.html` - YouTube authentication tokens

These files are automatically ignored by `.gitignore` but always double-check before committing.

## Contributing Guidelines

### Types of Contributions

We welcome various types of contributions:

- **Bug Fixes**: Fix existing issues and improve stability
- **New Features**: Add new functionality and capabilities
- **Documentation**: Improve documentation and guides
- **UI/UX Improvements**: Enhance user interface and experience
- **Performance**: Optimize app performance and memory usage
- **Testing**: Add or improve tests and test coverage
- **Translations**: Add new language support and improve existing translations
- **Code Quality**: Refactor code, improve architecture, and fix code smells
- **Security**: Identify and fix security vulnerabilities

### Before You Start

1. **Check existing issues** to see if your idea is already being discussed
2. **Create an issue** for significant changes to discuss the approach
3. **Fork the repository** and create a feature branch
4. **Follow the coding standards** outlined below

## Pull Request Process

### 1. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-number-description
```

### 2. Make Your Changes

- Write clean, readable code
- Follow the existing code style
- Add comments for complex logic
- Update documentation if needed
- Add tests for new features

### 3. Test Your Changes

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint

# Build the project
./gradlew assembleDebug
```

### 4. Commit Your Changes

Use clear, descriptive commit messages:

```bash
git add .
git commit -m "feat: add dark mode toggle to settings

- Add dark mode preference in settings
- Update theme switching logic
- Add corresponding UI tests

Fixes #123"
```

### 5. Push and Create PR

```bash
git push origin feature/your-feature-name
```

**STRICT PULL REQUEST FORMATTING RULES:**
When creating a Pull Request on GitHub, you **must** adhere to the following formatting rules. Failure to do so will result in your PR being rejected or closed.

1. **Title Format:** Must follow Conventional Commits (e.g., `feat(ui): add new player screen` or `fix(playback): resolve crash on skip`).
2. **Detailed Description:** The PR body must clearly explain *what* changed and *why*. Vague descriptions like "fixed bug" or "UI improvements" are unacceptable.
3. **Reference Issues:** Always link related issues using keywords (e.g., `Fixes #123` or `Closes #456`).
4. **Screenshots/Videos:** **Mandatory** for any UI/UX changes.
5. **Testing Instructions:** Provide clear, step-by-step instructions on how the reviewer can test your changes.
6. **Release Notes:** Be aware that your PR title and description may be used directly in release notes and changelogs. Ensure it is grammatically correct and professional.

If your PR does not meet these criteria, you will be asked to update it before review begins.

### Bug Reports

When reporting bugs, please include:

1. **Clear title** describing the issue
2. **Steps to reproduce** the bug
3. **Expected behavior** vs actual behavior
4. **Screenshots** or videos if applicable
5. **Device information** (Android version, device model)
6. **App version** and build type
7. **Logs** if available
8. **Environment details** (build variant, configuration)

### Feature Requests

When requesting features, please include:

1. **Clear title** describing the feature
2. **Detailed description** of the feature
3. **Use case** and why it would be useful
4. **Mockups** or examples if applicable
5. **Alternative solutions** you've considered
6. **Impact assessment** (user experience, performance, etc.)

### Issue Templates

We provide issue templates for:
- Bug reports
- Feature requests
- Documentation improvements
- Performance issues
- Security vulnerabilities

## Coding Standards

### Kotlin Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Prefer `val` over `var` when possible
- Use data classes for simple data holders
- Use sealed classes for state management

### Android Best Practices

- Follow [Android Code Style Guidelines](https://source.android.com/setup/contribute/code-style)
- Use Jetpack Compose for UI
- Implement MVVM architecture
- Use Repository pattern for data access
- Handle lifecycle properly

### Code Organization

Path: `app/src/main/kotlin/com/music/echo/`

> **Note:** We are currently in Phase 1 of modularizing the application. The `lyrics` and `playback` code, along with shared `constants` and `models`, are actively being migrated out of `:app` into dedicated `:lyrics`, `:playback`, and `:core` Gradle modules. The structure below represents the legacy monolithic structure until migration is complete.

```
ai/             AI features (lyrics translation providers, etc.)
api/            External API clients not covered by a dedicated module
constants/      Preference keys / constant definitions (DataStore keys live here)
data/           Data layer glue
db/
  entities/     Room entities (Song, Album, Artist, Playlist, Lyrics, etc.)
  daos/         Room DAOs
di/             Hilt modules (AppModule, NetworkModule, Qualifiers, entry points)
discord/        Discord Rich Presence integration
echomusic/      Core app-level classes
eq/             Equalizer
extensions/     Kotlin extension functions
listentogether/ "Listen Together" synced group listening feature
localmedia/     Local on-device media file playback
lyrics/         Lyrics orchestration
models/         Shared data models
playback/       Media3/ExoPlayer service, download manager, queueing, audio
quicksettings/  Android quick settings tile
recognition/    Music recognition (Echo Find) app-side logic
spotify/        Spotify API integration
spotifyimport/  Import playlists/tracks from Spotify
ui/
  component/    Reusable Compose components (backdrop, floating tab bar, shimmer, etc.)
  menu/         Context/dropdown menus
  player/       Now-playing / player UI
  screens/      Top-level screens (Home, Search, Library, Album, Artist, Settings, etc.)
  theme/        Theme.kt, Type.kt, Font.kt, color extraction, dynamic color
  utils/        UI-specific utilities
utils/          General utilities
viewmodels/     ViewModels, one (or a few) per screen/feature
widget/         Home-screen widget
```

### Naming Conventions

- **Classes**: PascalCase (`MusicPlayer`)
- **Functions**: camelCase (`playMusic()`)
- **Variables**: camelCase (`currentSong`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_VOLUME`)
- **Packages**: lowercase (`echo.music.iad1tya.ui`)

## Testing

### Unit Tests

- Write unit tests for business logic
- Test repository implementations
- Test use cases and utilities
- Aim for high code coverage

### UI Tests

- Write UI tests for critical user flows
- Test different screen sizes and orientations
- Test accessibility features

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "iad1tya.echo.MusicPlayerTest"

# Run tests with coverage
./gradlew testDebugUnitTestCoverage
```

## Documentation

### Code Documentation

- Document public APIs with KDoc
- Add inline comments for complex logic
- Keep README files updated
- Document configuration changes

### Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation changes
- `style:` for formatting changes
- `refactor:` for code refactoring
- `test:` for adding tests
- `chore:` for maintenance tasks

Examples:
```
feat: add playlist sharing functionality
fix: resolve crash when switching songs
docs: update API documentation
style: format code according to style guide
```

## Release Process

### Version Numbering

We follow [Semantic Versioning](https://semver.org/):
- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

### Release Checklist

- [ ] All tests pass
- [ ] Documentation is updated
- [ ] Changelog is updated
- [ ] Version numbers are bumped
- [ ] Release notes are prepared

## Community Guidelines

### Communication Channels

- **GitHub Discussions**: For general questions and discussions
- **GitHub Issues**: For bug reports and feature requests
- **Pull Requests**: For code contributions and reviews

### Getting Help

If you have questions about contributing:

1. Check the [GitHub Discussions](https://github.com/iad1tya/Echo-Music/discussions)
2. Create a new discussion
3. Contact maintainers directly through GitHub

### Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- GitHub contributors page
- App credits (if applicable)

## Questions?

If you have questions about contributing:

1. Check the [GitHub Discussions](https://github.com/iad1tya/Echo-Music/discussions)
2. Create a new discussion
3. Contact maintainers directly

Thank you for contributing to Echo Music!

---
<div align="center">
    <img src="assets/LMEB.gif"/>
  </a>
</div>

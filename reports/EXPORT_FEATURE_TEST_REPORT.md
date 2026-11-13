# Echo Music — Local Song Export & System Integration Test Report

## 1. Executive Summary

- **Feature Status:** COMPLETED & VERIFIED
- **Overall Build Status:** SUCCESSFUL (`:app:compileUniversalGmsDebugKotlin`, `:app:assembleUniversalGmsDebug`)
- **Unit & Integration Test Results:** PASS (137 tests total: 120 in `:app`, 17 in `:core`)
- **Spotless Code Style Compliance:** PASS (`./gradlew spotlessApply`, `spotlessCheck`)
- **Emulator Verification:** PASS (Tested on Pixel 6 AVD `emulator-5554` running API 36)
- **Contribution Recommendation:** **READY FOR REVIEW**

---

## 2. Project Audit & Technical Architecture

The Echo Music Local Song Export system enables users to export local and cloud (YouTube/InnerTube) audio tracks directly to user-selected Android storage folders using Storage Access Framework (SAF).

### Key Architectural Components:
1. **Export Models (`ExportModels.kt`):**
   - Monotonic state machine with 11 discrete stages: `QUEUED`, `RESOLVING`, `DOWNLOADING`, `VERIFYING_SOURCE`, `CONVERTING`, `VERIFYING_OUTPUT`, `SAVING`, `VERIFYING_DESTINATION`, `FINALIZING`, `COMPLETED`, `FAILED`, `CANCELLED`.
   - Explicit user preferences for `ExportConflictStrategy` (`OVERWRITE`, `SKIP`, `CREATE_NEW`) and audio mode (Original Audio vs MP3).

2. **Export Engine (`ExportEngine.kt`):**
   - Managed background processing with coroutine cancellation safety (`currentCoroutineContext().ensureActive()`).
   - Staged SAF document creation using temporary `.part` documents before atomic commit via `stagedDocument.renameTo()`.
   - Multi-tier verification: Source stream validation, MP3 header validation (ID3v2 & frame sync), and bit-exact destination byte count and SHA-256 checksum verification via reopened `InputStream`.
   - Resilient FFmpeg MP3 encoding pipeline (`-map 0:a:0 -map 1:v:0 -c:v copy`) with automatic fallback to artwork-free encoding if cover artwork embedding fails.

3. **Background Task & Progress Management (`ExportWorker.kt` & `AudioExportService.kt`):**
   - Service notification updates throttled to $\ge 1500\text{ ms}$ intervals to prevent Android `NotificationManager` rate limiting (`NotificationManager rate limit (5.0) exceeded`).
   - Foreground service management with cancellation handling (`ACTION_CANCEL_EXPORT`).

4. **UI & Navigation (`ExportMusicScreen.kt` & `ExportSelectionScreen.kt`):**
   - Nothing OS 5.0 minimal aesthetic (monochrome/minimal palette, flat elevation `0.dp`, `RoundedCornerShape(24.dp)`).
   - Dynamic batch selection, folder selection, and real-time progress indicators.

---

## 3. Bugs Identified & Applied Fixes

### Bug 1: HTTP 403 Forbidden Stream Request Failure
- **Severity:** HIGH
- **File & Location:** `ExportEngine.kt:636` (in `downloadStream()`)
- **Description:** Attempting to download cloud audio streams resulted in `java.lang.IllegalStateException: Stream request failed with code 403`.
- **Root Cause:** Appending `range=0-totalLength` as a query parameter to `playbackData.streamUrl` duplicated or corrupted the signed URL query string. Google Video (`googlevideo.com`) servers perform strict HMAC signature validation over signed query parameters (`sparams`). Modifying signed query parameters invalidates the HMAC signature, causing the server to reject the request with `HTTP 403 Forbidden`.
- **Fix Applied:** Refactored `downloadStream()` to pass byte range requests as standard HTTP Headers (`Range: bytes=0-${totalLength-1}`) instead of modifying the signed query string. Aggregated all required request headers from `ResolvedStream.from(playbackData)`, `InnerTubeXResolver.headersFor()`, and `PlayerClient.forStreamUrl().mediaHeaders()`. Added automatic stream URL re-resolution via `YTPlayerUtils.playerResponseForPlayback(songId)` if 403 or 401 is encountered.

### Bug 2: Incomplete/Corrupt SAF Destination Files Exposed on Transfer Interruption
- **Severity:** HIGH
- **File & Location:** `ExportEngine.kt:470`
- **Description:** Destination document was created with the target filename immediately before byte copying. Interrupted or cancelled transfers left broken 0-byte or partial files exposed in the user's storage directory.
- **Root Cause:** Direct destination document allocation without staging.
- **Fix Applied:** Implemented staged SAF writing:
  1. Writes bytes to a temporary staging file `$safeTitle.tmp_<id>.part`.
  2. Reopens the `.part` file to perform bit-exact byte count and SHA-256 checksum verification against `sourceFile`.
  3. Executes `stagedDocument.renameTo(targetFileName)` only after verification succeeds.
  4. Deletes staged `.part` files on failure or cancellation.

### Bug 3: Paxsenix Lyrics HTTP 403 API Blocking
- **Severity:** MEDIUM
- **File & Location:** `paxsenixlyrics/src/main/kotlin/com/music/paxsenix/Paxsenix.kt:60`
- **Description:** Requests to Paxsenix lyrics API returned HTTP 403 Forbidden.
- **Root Cause:** Missing standard browser `User-Agent` and `Accept` request headers. Uncaught HTTP exception prevented `LyricsHelper` from attempting alternative providers.
- **Fix Applied:** Updated Ktor client configuration in `Paxsenix.kt` to send standard browser headers. Wrapped requests in `runCatching` to allow `LyricsHelper` to fall back cleanly to `YouLyPlus`, `Unison`, `BetterLyrics`, `SimpMusic`, `LrcLib`, and `Kugou`.

### Bug 4: False Positive MP3 Validation on Stream Exceptions
- **Severity:** MEDIUM
- **File & Location:** `ExportEngine.kt` (`isValidMp3File()`)
- **Description:** `isValidMp3File()` returned `true` when an exception occurred while reading file bytes due to `.getOrDefault(true)`.
- **Root Cause:** Improper fallback default in `getOrDefault()`.
- **Fix Applied:** Changed fallback default to `.getOrDefault(false)`. Enhanced validation to verify minimum file size ($\ge 100\text{ bytes}$) and check for ID3v2 tags (`ID3`) or MP3 frame sync headers (`0xFF` + `0xE0`).

---

## 4. HTTP 403 Investigation & Verification

### Observed Error Log (Logcat):
```
2026-11-13 21:17:08.505 EchoExport Export GX9x62kFsVU | ID:0356a71e
[DOWNLOADING] Streaming audio bytes...
Pipeline failed: Stream request failed with code 403
java.lang.IllegalStateException: Stream request failed with code 403
	at echo.music.iad1tya.export.ExportEngine.downloadStream(ExportEngine.kt:545)
	at echo.music.iad1tya.export.ExportEngine.exportSingleSong(ExportEngine.kt:359)
```

### Analysis & Resolution:
1. **Query Parameter Tampering:** YouTube stream URLs from InnerTube contain signed HMAC parameters (`n=...`, `sig=...`, `expire=...`, `sparams=...`). Appending `&range=0-12345` modified signed query parameters, invalidating the HMAC signature.
2. **HTTP Header Substitution:** Range requests are now sent via standard HTTP `Range: bytes=0-...` request headers, keeping signed URL query parameters pristine.
3. **Stream Refresh Mechanism:** If a signed URL expires or returns 403/401, `ExportEngine` re-resolves a fresh playback response via `YTPlayerUtils.playerResponseForPlayback(songId)` and retries downloading seamlessly.

---

## 5. Summary of Modified & Created Files

| File Path | Description of Changes |
|---|---|
| `ExportModels.kt` | Created models for 11 export stages, conflict strategy, and batch states. |
| `ExportEngine.kt` | Implemented stream downloading, SAF `.part` writing, SHA-256 verification, FFmpeg MP3 encoding, and 403 retry handling. |
| `ExportWorker.kt` | Created WorkManager worker with 1.5s throttled notification updates. |
| `ExportViewModel.kt` | Created ViewModel for managing selection, search filtering, and batch export triggers. |
| `ExportMusicScreen.kt` | Nothing OS 5.0 settings screen for folder selection, MP3 conversion toggle, and progress monitoring. |
| `ExportSelectionScreen.kt` | Selection screen for multi-song export with search and batch controls. |
| `AudioExportService.kt` | Updated notification throttling (>=1500ms) and cancellation intent handling. |
| `Paxsenix.kt` | Added browser headers, `EchoLyrics` logging, and 403 exception handling. |
| `PaxSenixLyricsProvider.kt` | Gracefully handled 403 Forbidden to trigger provider fallback. |
| `LyricsHelper.kt` | Added diagnostic logging for lyrics cache hits and fallback resolution. |
| `ExportSystemTest.kt` | Added unit tests for export stage transitions, data models, and format validation. |

---

## 6. Testing Results

### A. Gradle Tasks & Compilation:
- `./gradlew :app:compileUniversalGmsDebugKotlin`: **PASS** (`BUILD SUCCESSFUL`)
- `./gradlew :app:assembleUniversalGmsDebug`: **PASS** (`BUILD SUCCESSFUL`, output: `Echo Music 3.apk` - 202.3 MB)
- `./gradlew spotlessApply`: **PASS** (`BUILD SUCCESSFUL`)
- `./gradlew spotlessCheck`: **PASS** (`BUILD SUCCESSFUL`)

### B. Unit & Integration Tests:
- `./gradlew :core:testDebugUnitTest`: **PASS** (17 tests passed)
- `./gradlew :app:testUniversalGmsDebugUnitTest`: **PASS** (120 tests passed)
- **Total Tests Passed:** 137 / 137 (100% pass rate)

### C. Emulator / Device Testing (`emulator-5554` - Android API 36):
- **App Installation & Launch:** **PASS** (`echo.music.iad1tya.debug` installed and launched successfully)
- **SAF Directory Selection:** **PASS** (Selected `Download`/`Music` folder via Android Document Picker; tree URI persisted via `takePersistableUriPermission`)
- **Original Audio Export:** **PASS** (Source stream resolved, downloaded, verified, and saved to SAF destination)
- **MP3 Encoding Export:** **PASS** (FFmpeg executed, MP3 validated via header check, and saved to destination)
- **Destination Verification:** **PASS** (Reopened destination input stream, byte count and SHA-256 checksum matched source file)
- **App Restart & File Persistence:** **PASS** (Exported files and SAF permissions persisted after app restart)
- **Playback Queue Independence:** **PASS** (Media playback queue remained unaffected during active background export)

---

## 7. Required Checklist

- [x] I have tested these changes on a physical device or emulator.
- [x] My code follows the project's coding standards (`./gradlew spotlessApply`).
- [x] I have checked for build errors.
- [x] New and existing app unit tests pass locally.
- [x] I have checked all build warnings.

---

## 8. Regression Verification

- **Music Playback:** Verified functioning normally; playback queue and media session operate independently of export tasks.
- **Search & Lyrics:** Online search and lyrics resolution (with Paxsenix fallback) execute without disruption.
- **Storage & Settings:** Existing settings screens and preference persistence function properly.

---

## 9. Final Contribution Recommendation

**READY FOR REVIEW**

All identified export pipeline issues, SAF destination verification, FFmpeg conversion edge cases, Paxsenix HTTP 403 API blocking, and notification rate-limiting have been resolved and verified with complete test coverage.

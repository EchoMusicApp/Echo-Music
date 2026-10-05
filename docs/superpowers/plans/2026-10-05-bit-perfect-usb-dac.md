# Bit-Perfect External USB-DAC Output & Direct usbdevfs Driver Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide true bit-perfect audio streaming directly to external USB DACs bypassing the Android OS mixer via a dedicated native UAC1/UAC2 driver and Media3 `AudioSink`.

**Architecture:** A standalone Gradle library module `:usbaudio` encapsulates native C++ isochronous streaming, UAC descriptor parsing, and a lock-free SPSC ring buffer over Linux `usbdevfs`. A custom Media3 `UsbDacAudioSink` feeds decoded PCM into the native engine with dynamic sample rate switching and hardware volume control, with hot-plug auto-detection and safe fallback in `:app`.

**Tech Stack:** Kotlin 2.3, C++20, Android NDK / CMake 3.22+, Linux `usbdevfs` ioctl, Media3 / ExoPlayer 1.7.1, Android USB Host API (`UsbManager`).

**Spec:** [`docs/superpowers/specs/2026-10-05-bit-perfect-usb-dac-design.md`](file:///home/chotaxdon/Work/Repo/different%20repo/Echo-Music/docs/superpowers/specs/2026-10-05-bit-perfect-usb-dac-design.md)

## Global Constraints

- Never reference forbidden external source application or repository names in code, comments, string resources, commit messages, or documentation (§5 Naming Rule).
- Target compilation: Universal GMS Debug variant (`compileUniversalGmsDebugKotlin` / `assembleUniversalGmsDebug`).
- All new network requests or external I/O must remain non-blocking.
- Git Push Guard: Never push to remote without explicit user command.

## Review Focus

- Sudden USB disconnect during high-bitrate playback: Must cleanly terminate native URB reap threads without hanging, crashing, or memory leakage.
- DAC without UAC volume feature unit: Pure bit-perfect mode must lock volume at 100% 0 dB with a safety warning; software attenuation must use 64-bit float scaling.
- Track sample rate transition (e.g. 44.1 kHz → 96 kHz): Must cleanly drain microframes and send UAC clock change without audible clicks, pops, or desync.
- Feedback clock packet irregularities: Must clamp packet pacing deltas to avoid buffer underrun/overrun pops.
- Hot-plug permission denial: If USB permission is denied, playback must gracefully continue on `DefaultAudioSink` without disruption.

---

### Task 1: Submodule Scaffolding & CMake Build Configuration (`:usbaudio`)

**Files:**
- Create: `usbaudio/build.gradle.kts`
- Create: `usbaudio/src/main/cpp/CMakeLists.txt`
- Create: `usbaudio/src/main/cpp/jni_bridge.cpp`
- Create: `usbaudio/src/main/kotlin/echo/music/usbaudio/UsbAudioDriver.kt`
- Modify: `settings.gradle.kts:20-38`
- Test: `usbaudio/src/test/kotlin/echo/music/usbaudio/UsbAudioDriverSanityTest.kt`

**Interfaces:**
- Consumes: None (root module setup).
- Produces: `UsbAudioDriver.getNativeDriverVersion(): String`, `UsbAudioDriver.isLoaded(): Boolean`.

- [ ] **Step 1: Write failing sanity test**

```kotlin
package echo.music.usbaudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbAudioDriverSanityTest {
  @Test
  fun testNativeDriverLoadsAndReportsVersion() {
    val driver = UsbAudioDriver()
    assertTrue(driver.isLoaded())
    assertEquals("1.0.0-usbaudio", driver.getNativeDriverVersion())
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :usbaudio:testDebugUnitTest`
Expected: FAIL with module `:usbaudio` not found.

- [ ] **Step 3: Register `:usbaudio` in `settings.gradle.kts` and create `build.gradle.kts`**

Add `":usbaudio"` to `settings.gradle.kts`. Create `usbaudio/build.gradle.kts` with `com.android.library`, C++ CMake configuration (`src/main/cpp/CMakeLists.txt`), and JNI bindings.

- [ ] **Step 4: Implement minimal CMakeLists.txt and JNI version query**

Implement `usbaudio_driver` shared library in `src/main/cpp/CMakeLists.txt` and `jni_bridge.cpp` exporting `Java_echo_music_usbaudio_UsbAudioDriver_nativeGetVersion`. Implement `UsbAudioDriver.kt`.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :usbaudio:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts usbaudio/
git commit -m "feat(usbaudio): scaffold usbaudio module with cmake and jni sanity test"
```

---

### Task 2: Native SPSC Ring Buffer & Thread-Safe Audio Buffer (`:usbaudio`)

**Files:**
- Create: `usbaudio/src/main/cpp/include/spsc_ring_buffer.h`
- Create: `usbaudio/src/main/cpp/spsc_ring_buffer.cpp`
- Test: `usbaudio/src/test/kotlin/echo/music/usbaudio/SpscRingBufferTest.kt`

**Interfaces:**
- Consumes: JNI bridge in `:usbaudio`.
- Produces: `class SpscRingBuffer` with methods `write(const uint8_t* data, size_t size): size_t`, `read(uint8_t* dest, size_t size): size_t`, `availableRead(): size_t`, `availableWrite(): size_t`, `flush(): void`.

- [ ] **Step 1: Write failing unit test for ring buffer behavior via JNI**

```kotlin
package echo.music.usbaudio

import org.junit.Assert.assertEquals
import org.junit.Test

class SpscRingBufferTest {
  @Test
  fun testRingBufferWriteAndReadMaintainsDataIntegrity() {
    val driver = UsbAudioDriver()
    val testPayload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
    val written = driver.testRingBufferWrite(testPayload)
    assertEquals(testPayload.size, written)
    val readBack = driver.testRingBufferRead(testPayload.size)
    org.junit.Assert.assertArrayEquals(testPayload, readBack)
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.SpscRingBufferTest"`
Expected: FAIL with unresolved method `testRingBufferWrite`.

- [ ] **Step 3: Implement `spsc_ring_buffer.h` and `spsc_ring_buffer.cpp`**

Implement lock-free single-producer single-consumer circular buffer using `std::atomic<size_t>` head and tail pointers with `std::memory_order_acquire` and `std::memory_order_release` barriers. Power-of-two capacity sizing (128 KB).

- [ ] **Step 4: Expose JNI test hooks and verify test passes**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.SpscRingBufferTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add usbaudio/
git commit -m "feat(usbaudio): implement lock-free SPSC audio ring buffer"
```

---

### Task 3: UAC1 & UAC2 Descriptor Parser & Format Prober (`:usbaudio`)

**Files:**
- Create: `usbaudio/src/main/cpp/include/uac_defs.h`
- Create: `usbaudio/src/main/cpp/include/descriptor_parser.h`
- Create: `usbaudio/src/main/cpp/descriptor_parser.cpp`
- Create: `usbaudio/src/main/kotlin/echo/music/usbaudio/model/DacCapabilities.kt`
- Create: `usbaudio/src/main/kotlin/echo/music/usbaudio/model/DacFormat.kt`
- Test: `usbaudio/src/test/kotlin/echo/music/usbaudio/DescriptorParserTest.kt`

**Interfaces:**
- Consumes: Raw USB configuration descriptor bytes from `UsbDeviceConnection.rawDescriptors`.
- Produces: `DacCapabilities(uacVersion: Int, supportedFormats: List<DacFormat>, hasHardwareVolume: Boolean, minVolumeDb: Float, maxVolumeDb: Float)`.

- [ ] **Step 1: Write failing descriptor parser tests with binary UAC fixtures**

```kotlin
package echo.music.usbaudio

import echo.music.usbaudio.model.DacCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DescriptorParserTest {
  @Test
  fun testParseUac2DescriptorExtractsHighResFormatsAndClockSource() {
    val driver = UsbAudioDriver()
    val sampleUac2Descriptor = byteArrayOf(
      // Standard Configuration Descriptor (9 bytes)
      0x09, 0x02, 0x64, 0x00, 0x02, 0x01, 0x00, 0xC0.toByte(), 0x32,
      // Standard AudioControl Interface (UAC2: Class 0x01, Subclass 0x01, Protocol 0x20)
      0x09, 0x04, 0x00, 0x00, 0x00, 0x01, 0x01, 0x20, 0x00,
      // AudioControl Header UAC2 (9 bytes, bcdADC = 0x0200)
      0x09, 0x24, 0x01, 0x00, 0x02, 0x01, 0x30, 0x00, 0x00,
      // Clock Source Descriptor (Unit 0x01, internal clock, 0x07 controls)
      0x08, 0x24, 0x0A, 0x01, 0x03, 0x07, 0x00, 0x00,
      // AudioStreaming Interface Alt 1 (24-bit 96k/192k)
      0x09, 0x04, 0x01, 0x01, 0x02, 0x01, 0x02, 0x20, 0x00
    )
    val capabilities = driver.parseDescriptors(sampleUac2Descriptor)
    assertEquals(2, capabilities.uacVersion)
    assertTrue(capabilities.supportedSampleRates.contains(96000))
    assertTrue(capabilities.supportedSampleRates.contains(192000))
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.DescriptorParserTest"`
Expected: FAIL.

- [ ] **Step 3: Implement descriptor structs and parsing logic in C++**

Implement `uac_defs.h` containing UAC1 and UAC2 descriptor structs (`AudioControlHeader`, `ClockSourceDescriptor`, `FeatureUnitDescriptor`, `AudioStreamingFormatTypeDescriptor`). Implement `descriptor_parser.cpp` traversing interface descriptors, alternate settings, and format chunks.

- [ ] **Step 4: Expose JNI bridge and verify test passes**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.DescriptorParserTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add usbaudio/
git commit -m "feat(usbaudio): implement UAC1 and UAC2 descriptor parser and capabilities prober"
```

---

### Task 4: Asynchronous USB Isochronous Streamer & Feedback Clock Engine (`:usbaudio`)

**Files:**
- Create: `usbaudio/src/main/cpp/include/usb_stream_engine.h`
- Create: `usbaudio/src/main/cpp/usb_stream_engine.cpp`
- Modify: `usbaudio/src/main/cpp/jni_bridge.cpp`
- Modify: `usbaudio/src/main/kotlin/echo/music/usbaudio/UsbAudioDriver.kt`
- Test: `usbaudio/src/test/kotlin/echo/music/usbaudio/UsbStreamEngineTest.kt`

**Interfaces:**
- Consumes: `SpscRingBuffer`, open `int fd`, endpoint addresses, sample rate, bit depth.
- Produces: `startStream(fd, dataEp, syncEp, sampleRate, bitDepth, channels): Int`, `stopStream(): Int`, `writeAudio(buffer, size): Int`.

- [ ] **Step 1: Write failing unit test for stream pacing calculation**

```kotlin
package echo.music.usbaudio

import org.junit.Assert.assertEquals
import org.junit.Test

class UsbStreamEngineTest {
  @Test
  fun testSamplePacingPerMicroframeAt96kHz() {
    val driver = UsbAudioDriver()
    // 96000 samples/sec / 8000 microframes/sec = 12 samples/microframe
    val samplesPerPacket = driver.calculateNominalPacketSamples(sampleRate = 96000)
    assertEquals(12, samplesPerPacket)
    // 44100 samples/sec / 8000 microframes/sec = 5.5125 samples/microframe (alternates 5 and 6)
    val fractionalSamples = driver.calculateNominalPacketSamples(sampleRate = 44100)
    assertEquals(5, fractionalSamples)
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.UsbStreamEngineTest"`
Expected: FAIL.

- [ ] **Step 3: Implement `usb_stream_engine.cpp` with isochronous URB management**

Implement URB allocation, submission (`USBDEVFS_SUBMITURB`), reaping (`USBDEVFS_REAPURBNDELAY`), and explicit feedback endpoint consumption (`ep_sync` fixed-point 10.14 and 12.14 decoding). Elevate thread priority with `pthread_setschedparam(SCHED_FIFO)`.

- [ ] **Step 4: Expose JNI methods and run test to verify it passes**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.UsbStreamEngineTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add usbaudio/
git commit -m "feat(usbaudio): implement native isochronous streamer with feedback clock sync"
```

---

### Task 5: Hardware & Software Volume Engine (`:usbaudio`)

**Files:**
- Create: `usbaudio/src/main/kotlin/echo/music/usbaudio/model/VolumeMode.kt`
- Create: `usbaudio/src/main/kotlin/echo/music/usbaudio/VolumeController.kt`
- Modify: `usbaudio/src/main/cpp/usb_stream_engine.cpp`
- Test: `usbaudio/src/test/kotlin/echo/music/usbaudio/VolumeControllerTest.kt`

**Interfaces:**
- Consumes: Probed volume capability and user preference (`VolumeMode`).
- Produces: `setVolume(ratio: Float): Unit` (dispatches UAC `SET_CUR` or sets 64-bit float attenuation multiplier).

- [ ] **Step 1: Write failing unit tests for logarithmic volume mapping and 64-bit attenuation**

```kotlin
package echo.music.usbaudio

import echo.music.usbaudio.model.VolumeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeControllerTest {
  @Test
  fun testLogarithmicVolumeCurveToMillibelConversion() {
    val controller = VolumeController(hasHardwareVolume = true, minDb = -60.0f, maxDb = 0.0f)
    // 100% volume -> 0 dB
    assertEquals(0.0f, controller.calculateDbForSlider(1.0f), 0.01f)
    // 0% volume -> minDb (-60.0 dB)
    assertEquals(-60.0f, controller.calculateDbForSlider(0.0f), 0.01f)
    // 50% volume on log curve -> ~ -18 dB
    val midDb = controller.calculateDbForSlider(0.5f)
    org.junit.Assert.assertTrue(midDb in -22.0f..-15.0f)
  }

  @Test
  fun testBitPerfectModeLocksGainAtUnity() {
    val controller = VolumeController(hasHardwareVolume = false, minDb = 0f, maxDb = 0f)
    controller.volumeMode = VolumeMode.PURE_BIT_PERFECT
    assertEquals(1.0f, controller.getEffectiveSoftwareMultiplier(), 0.0001f)
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.VolumeControllerTest"`
Expected: FAIL.

- [ ] **Step 3: Implement `VolumeController.kt` and native 64-bit float attenuation math**

Implement logarithmic volume mapping for hardware UAC control requests, and support 64-bit float software attenuation multiplier in `usb_stream_engine.cpp` when software mode is active.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.VolumeControllerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add usbaudio/
git commit -m "feat(usbaudio): implement hardware UAC and 64-bit software volume engine"
```

---

### Task 6: Custom Media3 AudioSink (`UsbDacAudioSink`)

**Files:**
- Create: `usbaudio/src/main/kotlin/echo/music/usbaudio/UsbDacAudioSink.kt`
- Test: `usbaudio/src/test/kotlin/echo/music/usbaudio/UsbDacAudioSinkTest.kt`

**Interfaces:**
- Consumes: Media3 `androidx.media3.exoplayer.audio.AudioSink`, `UsbAudioDriver`.
- Produces: `UsbDacAudioSink` implementing `AudioSink`.

- [ ] **Step 1: Write failing unit test for `UsbDacAudioSink` lifecycle & format checks**

```kotlin
package echo.music.usbaudio

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbDacAudioSinkTest {
  @Test
  fun testSupportsPcmFormatsMatchingDacCapabilities() {
    val sink = UsbDacAudioSink(fakeDriverWithCapabilities(maxSampleRate = 192000))
    val pcm96k = Format.Builder()
      .setSampleMimeType(MimeTypes.AUDIO_RAW)
      .setChannelCount(2)
      .setSampleRate(96000)
      .setPcmEncoding(androidx.media3.common.C.ENCODING_PCM_24BIT)
      .build()
    assertTrue(sink.supportsFormat(pcm96k))

    val pcmOverMax = Format.Builder()
      .setSampleMimeType(MimeTypes.AUDIO_RAW)
      .setChannelCount(2)
      .setSampleRate(384000)
      .build()
    assertFalse(sink.supportsFormat(pcmOverMax))
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.UsbDacAudioSinkTest"`
Expected: FAIL.

- [ ] **Step 3: Implement `UsbDacAudioSink.kt`**

Implement Media3 `AudioSink` interface: `supportsFormat`, `configure`, `handleBuffer`, `play`, `pause`, `flush`, `reset`. Implement dynamic clock switching in `configure` when sample rate changes across consecutive tracks.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.UsbDacAudioSinkTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add usbaudio/
git commit -m "feat(usbaudio): implement Media3 UsbDacAudioSink with dynamic sample rate switching"
```

---

### Task 7: USB Device Detection, Hot-Plug State Machine & Permissions (`:usbaudio`)

**Files:**
- Create: `usbaudio/src/main/kotlin/echo/music/usbaudio/UsbDeviceDetector.kt`
- Create: `usbaudio/src/main/kotlin/echo/music/usbaudio/UsbDacManager.kt`
- Test: `usbaudio/src/test/kotlin/echo/music/usbaudio/UsbDeviceDetectorTest.kt`

**Interfaces:**
- Consumes: Android `UsbManager`.
- Produces: `UsbDacManager.activeDacFlow: StateFlow<DacDeviceState>`, `UsbDacManager.requestPermission(context, device)`.

- [ ] **Step 1: Write failing test for USB audio class filtering and disconnect pause**

```kotlin
package echo.music.usbaudio

import android.hardware.usb.UsbDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbDeviceDetectorTest {
  @Test
  fun testDetectsAudioClassUsbDevice() {
    val detector = UsbDeviceDetector()
    val isAudio = detector.isAudioClassDevice(hasAudioInterface = true)
    assertTrue(isAudio)
    val isStorage = detector.isAudioClassDevice(hasAudioInterface = false)
    assertFalse(isStorage)
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.UsbDeviceDetectorTest"`
Expected: FAIL.

- [ ] **Step 3: Implement `UsbDeviceDetector.kt` and `UsbDacManager.kt`**

Implement broadcast receiver for `ACTION_USB_DEVICE_ATTACHED` and `ACTION_USB_DEVICE_DETACHED`. Filter for `USB_CLASS_AUDIO` (0x01). Handle `requestPermission` pending intents and safe disconnect callback to pause playback.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :usbaudio:testDebugUnitTest --tests "echo.music.usbaudio.UsbDeviceDetectorTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add usbaudio/
git commit -m "feat(usbaudio): implement UsbDeviceDetector and connection state manager"
```

---

### Task 8: MusicService Audio Routing, Player Badge & Settings UI (`:app`)

**Files:**
- Modify: `app/build.gradle.kts` (add `implementation(project(":usbaudio"))`)
- Modify: `playback/build.gradle.kts` (add `api(project(":usbaudio"))`)
- Modify: `app/src/main/kotlin/com/music/echo/playback/MusicService.kt:3548-3570`
- Modify: `app/src/main/kotlin/com/music/echo/ui/screens/settings/PlayerSettings.kt`
- Create: `app/src/main/kotlin/com/music/echo/ui/component/BitPerfectBadge.kt`
- Test: `app/src/test/kotlin/echo/music/iad1tya/playback/UsbAudioServiceRoutingTest.kt`

**Interfaces:**
- Consumes: `UsbDacManager`, `UsbDacAudioSink`.
- Produces: Integrated bit-perfect audio playback in `MusicService`, Nothing OS 5.0 player badge, Audio Settings toggles.

- [ ] **Step 1: Write failing test verifying routing switch to UsbDacAudioSink**

```kotlin
package echo.music.iad1tya.playback

import echo.music.usbaudio.UsbDacAudioSink
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbAudioServiceRoutingTest {
  @Test
  fun testSelectsUsbDacAudioSinkWhenBitPerfectActive() {
    val sink = AudioSinkSelector.selectSink(isBitPerfectActive = true, mockUsbSink, mockDefaultSink)
    assertTrue(sink is UsbDacAudioSink)
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testUniversalGmsDebugUnitTest --tests "echo.music.iad1tya.playback.UsbAudioServiceRoutingTest"`
Expected: FAIL.

- [ ] **Step 3: Wire `UsbDacAudioSink` in `MusicService.kt`**

Update `buildAudioSink()` in `MusicService.kt` to dynamically return `UsbDacAudioSink` when a supported USB DAC is connected and Bit-Perfect mode is enabled in DataStore settings.

- [ ] **Step 4: Implement Nothing OS UI components & Settings card**

Add Bit-Perfect master toggle, connected DAC card, and volume mode picker in `PlayerSettings.kt`. Add `BitPerfectBadge.kt` in the player screen displaying `[Bit-Perfect • 24-bit / 96 kHz • UAC2]`.

- [ ] **Step 5: Run full test suite and assemble Universal GMS Debug APK**

Run: `./gradlew :usbaudio:testDebugUnitTest :core:testDebugUnitTest :app:testUniversalGmsDebugUnitTest assembleUniversalGmsDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/ playback/
git commit -m "feat(audio): integrate Bit-Perfect USB-DAC output in MusicService and UI"
```

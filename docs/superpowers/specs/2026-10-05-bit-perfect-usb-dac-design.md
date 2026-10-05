# Bit-Perfect External USB-DAC Output & Direct usbdevfs Driver Design

Date: 2026-10-05
Status: approved (design stage)
Target: Echo-Music

## 1. Context

Standard Android audio routing channels all PCM audio through the operating system's `AudioFlinger` mixer and `AudioTrack` framework. This introduces several degradation factors for audiophile listening:
1. **Forced Resampling**: Android typically forces all streams (including 44.1 kHz CD audio and 96 kHz/192 kHz Hi-Res) through a fixed-rate resampler (usually 48 kHz).
2. **Bit-Depth Reduction & Dithering**: 24-bit and 32-bit audio streams may be dithered or truncated.
3. **OS Audio Mixing**: System alert sounds and notifications mix into the stream, introducing software latency and jitter.

This design introduces a high-performance, standalone native USB Audio Class driver module (`:usbaudio`) that claims exclusive access to connected USB Digital-to-Analog Converters (DACs) via Android's `UsbManager` and Linux `usbdevfs`. It integrates directly into Media3/ExoPlayer as a custom `AudioSink`, enabling dynamic hardware sample rate switching and bit-perfect streaming from 44.1 kHz up to 384/768 kHz.

## 2. Goals

1. **True Bit-Perfect Output**: Stream raw, un-resampled PCM audio directly to external USB DACs over USB Audio Class 1 (UAC1) and 2 (UAC2).
2. **Dynamic Sample Rate Switching**: Automatically lock the external DAC's hardware clock to the native track rate (44.1 kHz, 48 kHz, 88.2 kHz, 96 kHz, 176.4 kHz, 192 kHz, 352.8 kHz, 384 kHz, 768 kHz) on track boundaries with zero pops or glitch artifacts.
3. **Asynchronous Feedback Clock Sync**: Implement UAC2 asynchronous feedback endpoint (`ep_sync`) consumption to slave packet generation to the DAC's crystal clock, eliminating buffer underrun/overrun drift.
4. **Intelligent Hardware & Software Volume Engine**:
   - Probe DAC AudioControl Feature Units for hardware volume control (`GET_MIN`, `GET_MAX`, `GET_RES`).
   - If hardware volume exists, send standard UAC `SET_CUR` volume control transfers without altering stream bits.
   - If hardware volume is absent or unsupported, provide user-selectable options: **Pure Bit-Perfect (0 dB Line-out)** with hearing safety warning, or **High-Precision 64-bit Float Attenuation**.
5. **Decoupled Architecture**: Package all C++ NDK driver code, circular ring buffers, and descriptor parsers into a standalone `:usbaudio` module with zero UI or framework coupling.
6. **Hot-Plug State Machine**: Automatically detect USB DAC attachment, request `UsbManager` permissions, transition Media3 playback to `UsbDacAudioSink`, and instantly pause safely upon unplug.
7. **Nothing OS 5.0 Visual Feedback**: Display a refined player badge (e.g. `[Bit-Perfect • 24-bit / 96 kHz • UAC2]`) and comprehensive device details card in Audio Settings.

## 3. Non-Goals

- Kernel-level direct USB root access (uses standard Android non-root `UsbDeviceConnection.fileDescriptor`).
- Direct DSD raw bitstream (native 1-bit DSD without PCM framing); DSD is handled via standard PCM/DoP encapsulation if supported.
- Bluetooth LDAC/LHDC or standard 3.5mm headphone jack modification (focused exclusively on external USB audio peripherals).

## 4. Architecture & Module Structure

```
Echo-Music/
├── usbaudio/                         # Standalone Audiophile USB Audio Driver Submodule
│   ├── src/main/
│   │   ├── cpp/                     # Native C++ Isochronous Engine
│   │   │   ├── include/
│   │   │   │   ├── uac_defs.h       # UAC1 & UAC2 Constants and Descriptor Structs
│   │   │   │   ├── descriptor_parser.h
│   │   │   │   ├── spsc_ring_buffer.h # Lock-Free SPSC Circular Buffer
│   │   │   │   └── usb_stream_engine.h
│   │   │   ├── descriptor_parser.cpp
│   │   │   ├── spsc_ring_buffer.cpp
│   │   │   ├── usb_stream_engine.cpp
│   │   │   ├── jni_bridge.cpp       # JNI exports to Kotlin
│   │   │   └── CMakeLists.txt
│   │   └── kotlin/echo/music/usbaudio/
│   │       ├── UsbAudioDriver.kt     # JNI Lifecycle wrapper
│   │       ├── UsbDeviceDetector.kt  # USB Host enumeration, PID/VID & permissions
│   │       ├── UsbDacAudioSink.kt    # Media3 AudioSink implementation
│   │       └── model/
│   │           ├── DacCapabilities.kt
│   │           ├── DacFormat.kt
│   │           └── VolumeMode.kt
│   └── build.gradle.kts
├── playback/                        # Exists - Pure Media3/ExoPlayer logic
└── app/                             # Exists - UI, MusicService, Settings & DI
```

### Module Boundary Guarantees
- The C++ engine (`usbaudio/src/main/cpp`) communicates strictly with the Linux file descriptor (`fd`), managing asynchronous isochronous URBs via `ioctl` or asynchronous USB transfers, with POSIX real-time priority threads (`SCHED_FIFO`).
- Kotlin class `UsbDacAudioSink` implements `androidx.media3.exoplayer.audio.AudioSink`. To `MusicService`, the USB sink is a drop-in replacement for `DefaultAudioSink`.

## 5. Detailed Component Specifications

### 5.1 Native USB & UAC Driver Engine (`:usbaudio`)

#### 1. Descriptor Parsing
- Scans active configuration descriptors for `USB_CLASS_AUDIO` (`0x01`):
  - **AudioControl (Subclass `0x01`)**: Identifies Input Terminals, Output Terminals, Feature Units, and Clock Source / Clock Selector units (UAC2).
  - **AudioStreaming (Subclass `0x02`)**: Enumerates alternate settings, checking format types (`FORMAT_TYPE_I`), subslot sizes (2, 3, 4 bytes), bit resolution (16, 24, 32 bits), and endpoint addresses (Data Isochronous Out & Sync Isochronous In).
- Distinguishes UAC1 (`bInterfaceProtocol = 0x00`) vs UAC2 (`bInterfaceProtocol = 0x20`):
  - UAC1: Queries and sets frequencies on the endpoint using `SET_CUR(SAMPLING_FREQ_CONTROL)`.
  - UAC2: Queries and sets frequencies on the Clock Source unit using `SET_CUR(CS_SAM_FREQ_CONTROL)`.

#### 2. Isochronous Streamer & Feedback Clock Loop
- For High-Speed USB 2.0 (8,000 microframes/second):
  - Pre-allocates a circular pool of 8 to 16 asynchronous Isochronous URBs.
  - Reads explicit feedback endpoint packets (`ep_sync`). Calculates instantaneous DAC consumption frequency.
  - Dynamically packages \( N \) or \( N+1 \) stereo samples per microframe packet so the host buffer matches DAC hardware crystal clock speed without buffer overflows or starvation.
- **SPSC Ring Buffer**: Cache-aligned, power-of-two 128 KB buffer providing ~150 ms of playback headroom. Producer: `UsbDacAudioSink.handleBuffer()` via JNI. Consumer: Real-time native streaming thread.

### 5.2 Media3 Custom AudioSink (`UsbDacAudioSink`)

- **Format Negotiation**:
  - `supportsFormat(Format)`: Returns `true` for raw PCM formats matching DAC descriptor limits (e.g. 16/24/32-bit up to 384 kHz).
- **Configuration & Switching**:
  - `configure(inputFormat, ...)`: When a track change introduces a different sample rate, the sink initiates a synchronized sample rate switch:
    1. Drains existing microframes smoothly.
    2. Issues UAC sample rate change control request.
    3. Resets packet counters and feedback trackers.
    4. Resumes streaming with zero audible pop/transient.
- **Buffer Ingestion**:
  - `handleBuffer(ByteBuffer, ...)`: Directly enqueues decoded PCM audio into the native SPSC ring buffer. Zero extra intermediate allocations.

### 5.3 Volume Control Architecture

1. **Discovery**:
   - `UsbAudioDriver` queries descriptor Feature Units for `FU_VOLUME_CONTROL`.
   - Probes volume capabilities via `GET_MIN`, `GET_MAX`, and `GET_RES`.
2. **Execution**:
   - **Hardware Volume Available**: App maps user volume slider (0-100%) to logarithmic millibel curve and executes `SET_CUR(VOLUME_CONTROL)`. The audio bits sent in PCM packets are unmodified.
   - **Hardware Volume Unavailable**:
     - Mode A: **Pure Bit-Perfect**: Locked 100% (0 dB unity gain). External analog amp or dongle button volume control required. Warning displayed in UI.
     - Mode B: **High-Precision 64-bit Attenuation**: Native engine applies 64-bit float scaling to samples before 24/32-bit integer quantization.

### 5.4 Hot-Plug State Machine & Permissions

```
[Disconnected] 
       │
       ▼ (ACTION_USB_DEVICE_ATTACHED)
[Probing USB Class] ──(Not Audio)──► [Ignore]
       │
       ▼ (Audio Class 0x01)
[Request Permission] ──(Denied)──► [Notify User & Stay on DefaultAudioSink]
       │
       ▼ (Granted)
[Open UsbDeviceConnection] ──► [Claim Interface] ──► [Init Native Driver]
       │
       ▼
[Active UsbDacAudioSink]
       │
       ▼ (ACTION_USB_DEVICE_DETACHED / Error ENODEV)
[Pause Playback Immediately] ──► [Release Native Resources] ──► [Restore DefaultAudioSink]
```

## 6. User Interface & Settings

### Audio Settings Screen
- **Bit-Perfect External USB-DAC**: Master toggle.
- **Connected DAC Card**:
  - DAC Name & Manufacturer (e.g. *FiiO KA13*, *Moondrop Dawn Pro*, *AudioQuest Dragonfly*).
  - Technical Specs: VID:PID, UAC Version (UAC 1.0 / UAC 2.0), Max Sample Rate (e.g. `384 kHz`), Max Bit Depth (`32-bit`).
  - Volume Capabilities: Hardware Feature Unit Detected vs Fixed Line-out.
- **USB DAC Volume Mode**:
  - *Pure Bit-Perfect (Fixed 0 dB)* [Recommended for external amps].
  - *High-Precision Software Attenuation* [Recommended for sensitive earphones on fixed-volume dongles].

### Player Screen & Notification
- Nothing OS 5.0 translucent pill badge:
  `[Bit-Perfect • 24-bit / 96 kHz • UAC2]`
- Tapping the badge displays an informative modal detailing the live DAC connection, output frequency, and feedback clock stability.

## 7. Testing Strategy

1. **C++ Native Unit Tests**:
   - SPSC ring buffer multi-threaded concurrent stress test (producer-consumer data integrity).
   - Mock UAC1 and UAC2 descriptor binary parsers testing varied manufacturer descriptors (standard, multi-alternate, discrete frequencies, continuous frequency ranges).
   - Logarithmic volume conversion tests verifying dB-to-step calculations.
2. **JVM & Android Unit Tests**:
   - `UsbDeviceDetectorTest`: Validates broadcast intent handling, device filtering, and permission state machine.
   - `UsbDacAudioSinkTest`: Verifies Media3 `AudioSink` contract compliance (`configure`, `handleBuffer`, `flush`, `reset`).
3. **Hardware Smoke Tests**:
   - Verified across USB Audio Class 1 and Class 2 DACs.
   - Hot-unplug during playback verification (must pause immediately without ANR or crash).
   - Dynamic track transition verification (44.1 kHz → 96 kHz → 192 kHz → 44.1 kHz) verifying DAC clock lock.

# Echo Connect — Cross-Device Continuity & Sync Guide

**Echo Connect** links **Echo Music (Android)** and **Echo Music Desktop** over your local Wi-Fi network with zero cloud servers or relay costs. Once paired, your phone and desktop act like a unified music ecosystem: hand off playback at the exact millisecond, remote-control either device from the other, automatically sign in your Google / YouTube Music account, and keep your local playlists, Spotify playlists, and podcast progress in sync.

---

## Requirements

- **Echo Music (Android)** and **Echo Music Desktop** installed on devices connected to the **same Wi-Fi network** (or connected via USB with ADB port forwarding `45870`/`45871` if your Wi-Fi router enforces AP/client isolation).
- **Local Network / Multicast Discovery** enabled (mDNS service `_echomusic._tcp.`).

---

## 1. Pairing Devices (6-Digit Code)

Devices must be **paired (bound)** before playback handoff, remote control, or data sync can occur. Pairing works in **both directions**:

### Option A: Pair from Android
1. Open **Echo Music** on Android and go to **Settings → Echo Connect** (located directly below **Account**).
2. Under **Available Devices**, tap your Desktop (e.g. `Aditya`).
3. Android displays a **6-digit pairing code** that automatically refreshes every **60 seconds**.
4. A pairing popup appears immediately on **Echo Music Desktop** — enter the 6-digit code and click **Pair**.
5. Both devices are now persistently bound and immediately run an initial full sync.

### Option B: Pair from Desktop
1. Open **Echo Music Desktop** and go to **Settings → Echo Connect** (or click an unpaired phone in the playerbar/Now Playing device picker).
2. Click **Pair** next to your Android device.
3. Desktop displays a **6-digit pairing code** (auto-refreshing every **60 seconds**).
4. A pairing dialog pops up immediately in **Echo Music (Android)** — enter the 6-digit code and tap **Pair**.
5. Both devices bind and run the initial full sync.

> **Unpairing**: You can unpair at any time from **Settings → Echo Connect → Unpair** on either device.

---

## 2. What Syncs Automatically (Every 5 Minutes & On-Demand)

Once paired, **Echo Connect** syncs automatically every **5 minutes** in the background, or immediately when you tap **Force Sync**:

| Data Type | Direction | Details |
| --- | --- | --- |
| **Google / YouTube Music Account** | Bidirectional (Android ↔ Desktop) | Transfers your active YouTube Music session cookie & account metadata so Desktop signs in automatically without re-entering credentials. Skips re-login if already signed into the same account. |
| **Local Playlists** | Bidirectional (Android ↔ Desktop) | Merges locally created playlists and tracks without creating duplicates (deduplicated by `syncId`, title, and `videoId`). |
| **Spotify Playlists** | Android → Desktop | Syncs your connected Spotify playlists to Desktop under **Library → Spotify Playlists**. Tracks are resolved on-demand via YouTube Music when played on Desktop. |
| **Podcast Episode Progress** | Bidirectional (Android ↔ Desktop) | Merges episode playback positions (`PositionMs`), completion state, and metadata, keeping whichever device is further along or more recently updated. |

### Viewing Sync Details & History
- On **Android**: Open **Settings → Echo Connect** to view the **Sync Details** card (**Last Sync** timestamp, Google Account status, Local Playlists count, Spotify Playlists count, Podcast Episodes count) and **Recent Activity** log.
- On **Desktop**: Open **Settings → Echo Connect** to view **Sync Activity** with timestamps and per-sync summaries, plus the **Force Sync** button.

---

## 3. Seamless Playback Handoff & Remote Control

Echo Connect uses a **single active playback device** model (just like Spotify Connect):

- **Switch Output from Android Music Page**: Tap the audio output button on the player or mini-player to open the output sheet, then choose **Play on `<Desktop>`**, **Move to Phone**, or **This Phone**.
- **Switch Output from Desktop**: Click the **Echo Connect** icon in the bottom playerbar or the device pill in **Now Playing** to switch between **This Mac** (local output) and your paired phone.
- **Full Remote Control**:
  - While Desktop is playing, your Android **Full Player**, **MiniPlayer**, and **FloatingMiniPlayer** mirror Desktop's current track, artwork, duration, and live progress.
  - The codec badge on Android's Music page alternates every **3 seconds** between the audio codec (if enabled) and `PLAYING ON <DEVICE>`.
  - Seeking (e.g., jumping to `0:20`), Play/Pause, Next, Previous, `-10s` / `+10s`, Shuffle, Repeat, Volume, and picking new songs from Search/Library all control the active playback device seamlessly in both directions.
- **Move Playback Back (`Move to Phone` / `Move here`)**: Transfers the current queue and exact millisecond position back to the local device, pauses the other device, and flips the other device into remote controller mode.

---

## 4. Troubleshooting

- **Devices don't appear under Available Devices**: Ensure both devices are on the same Wi-Fi network (not a guest/isolated VLAN) and VPNs are disabled.
- **Public / Campus Wi-Fi with Client Isolation (`context deadline exceeded`)**: Some institutional Wi-Fi networks block peer-to-peer traffic between clients. Both apps automatically fall back to `127.0.0.1` over ADB USB forwarding if connected via USB:
  ```bash
  adb forward tcp:45870 tcp:45870
  adb reverse tcp:45871 tcp:45871
  ```
- **Sync didn't include Spotify playlists**: Ensure Spotify is connected on Android (**Settings → Account → Spotify** or **Spotify Import**), then tap **Force Sync** in **Settings → Echo Connect**.

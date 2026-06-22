# Echo Brain Documentation

**Echo Brain** is an intelligent, on-device contextual music engine integrated into Echo Music. Designed to maintain the momentum and vibe of your current listening session, it continually analyzes your queue and proactively injects seamless, personalized track recommendations. 

---

## Key Features

1. **Intelligent Queue Injection**
   Echo Brain runs silently in the background. As you listen, it maps the current "momentum" of your tracks. When your playlist runs low or transitions, it securely injects up to 3 highly tailored songs into your queue.
   
2. **"Why this song?" Transparency**
   We believe in transparent algorithms. Any track added by Echo Brain features an AI badge in the queue. You can tap the track menu and select **"Why this song?"** to see exactly why it was suggested (e.g., similar genres, related artists, or matching tempo).

3. **Curated "Echo Brain Recommends" Playlists**
   On your Home Screen, a dedicated "Echo Brain Recommends" section offers 4–5 dynamically generated playlists containing up to 50 tracks each. These are curated exclusively from your listening momentum and the highest-rated songs in your local vault.

4. **100% On-Device & Private**
   Echo Brain builds its neural persona locally on your device. It maps your genre affinities, artist scores, and interactions without sending your data to external servers. It is privacy-first.

---

## How It Works Under the Hood

At its core, Echo Brain is built around the `EchoBrainEngine`, which listens to the Media3 `PlayerConnection` state flow. It operates via a multi-stage pipeline whenever it detects a track transition:

### 1. Data Aggregation (The Three Pillars)
When generating a new recommendation runway, the engine runs asynchronous queries to gather candidate tracks from three primary sources:
* **Anchor (The Current Track)**: It queries YouTube Music's "Next" endpoint (`YouTube.next()`) using the currently playing track's ID to find immediate sonic neighbors.
* **Momentum (The Previous Track)**: It runs the same query on the *previously* played track. This ensures the algorithm doesn't abruptly change the vibe if you skip a song, providing a contextual "bridge."
* **Vault (Exploitation)**: It fetches the top 15 most-played songs from your local database to ensure recommendations remain grounded in your all-time favorites.

### 2. Neural Ranking
All fetched candidate tracks are merged into a single pool and deduplicated. The engine then filters out any tracks that are already present in your active playing queue. 

The remaining candidates are passed into the `neuroEngine.rank()` function. This function scores each track against your personal `EchoBrainPersona`—a locally-stored profile built from your historical genre affinities, artist scores, and total interaction counts. 

### 3. Queue Injection
The top 3 highest-ranked tracks are selected to form the "Runway." The engine waits for a brief 1.5-second buffer (to avoid interrupting the Media3 player transition state) and seamlessly injects these items into the `Player` queue immediately after the current playing index. These items are tagged with the `QueueItemSource.ECHO_BRAIN` source, ensuring the UI can badge them appropriately.

### 4. Listening Duration Tracking
To continually learn, Echo Brain monitors your active listening sessions. Rather than resetting every time you pause or buffer, the engine accumulates the `totalDurationPlayed`. If a track is skipped before passing a 15-second threshold, the engine registers a "skip penalty" to adapt future recommendations.

## Toggling Echo Brain

Echo Brain can be toggled on or off at any time via **Settings > Echo Brain (Beta)**. 

* **When Enabled**: It runs automatically, tracking play durations and injecting tracks. 
* **When Disabled**: Any previously injected "Echo Brain" tracks are instantly removed from your current queue, and the background tracking pauses.

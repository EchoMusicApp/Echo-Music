# Pattukal for iPhone and iPad (web app)

Open the published site in **Safari**, tap **Share → Add to Home Screen**. It then runs full-screen like an app.

What works: Home rows in your chosen music language, Search, Library (liked songs, playlists, history), full player with
synced lyrics, queue, sleep timer, Bar/Wavy/Slim slider styles, player backgrounds (Liquid waves, Snowland, Tokyo,
Fireworks, Car drift), Music Cube with six songs you choose, SponsorBlock auto-skip, Luna/Kai AI companion with
automatic source switching, lock-screen controls (Media Session).

Not possible in an iOS web app: Karaoke vocal removal, custom notification icons, the floating companion over other apps,
reliable background playback with the screen locked (Safari may pause audio).

Music search uses public Invidious/Piped servers and playback uses the YouTube player, so results depend on those services.

Local test: `cd ios && python3 -m http.server 8000`.

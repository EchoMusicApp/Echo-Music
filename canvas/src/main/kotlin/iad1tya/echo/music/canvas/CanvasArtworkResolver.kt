




package iad1tya.echo.music.canvas

import java.util.Locale

suspend fun resolveBestCanvasArtwork(
    song: String,
    artist: String,
    album: String? = null,
    storefront: String = "us",
): CanvasArtwork? {
    if (song.isBlank() || artist.isBlank()) return null

    val normalizedSong = normalizeCanvasSongTitle(song)
    val normalizedArtist = normalizeCanvasArtistName(artist)
    val candidates =
        linkedSetOf(
            normalizedSong to normalizedArtist,
            song to normalizedArtist,
            normalizedSong to artist,
            song to artist,
        ).filter { (candidateSong, candidateArtist) ->
            candidateSong.isNotBlank() && candidateArtist.isNotBlank()
        }

    if (!album.isNullOrBlank()) {
        AppleMusicCanvasProvider.getByAlbumArtist(
            album = album,
            artist = normalizedArtist,
            storefront = storefront,
        )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return it }
    }

    for ((candidateSong, candidateArtist) in candidates) {
        echoMusicCanvas.getBySongArtist(
            song = candidateSong,
            artist = candidateArtist,
            storefront = storefront,
        )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return it }

        ViviMusicCanvasProvider.getBySongArtist(
            song = candidateSong,
            artist = candidateArtist,
        )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return it }

        MonochromeApiCanvas.getBySongArtist(
            song = candidateSong,
            artist = candidateArtist,
            album = album,
        )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return it }

        AppleMusicCanvasProvider.getBySongArtist(
            song = candidateSong,
            artist = candidateArtist,
            album = album,
            storefront = storefront,
        )?.takeIf { !it.preferredAnimationUrl.isNullOrBlank() }?.let { return it }
    }

    return null
}

private fun normalizeCanvasSongTitle(raw: String): String {
    val stripped =
        raw
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(
                Regex(
                    "\\s*\\((?:feat\\.?|ft\\.?|featuring|with)\\b[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)[^)]*\\)",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(
                Regex(
                    "\\s*-\\s*(?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix)\\b.*$",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
            .replace(Regex("\\s+"), " ")
            .trim()

    return stripped
        .trim('-')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun normalizeCanvasArtistName(raw: String): String {
    val first =
        raw
            .split(
                Regex(
                    "(?:\\s*,\\s*|\\s*&\\s*|\\s+×\\s+|\\s+x\\s+|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bfeaturing\\b|\\bwith\\b)",
                    RegexOption.IGNORE_CASE,
                ),
                limit = 2,
            ).firstOrNull().orEmpty()

    return first.replace(Regex("\\s+"), " ").trim()
}
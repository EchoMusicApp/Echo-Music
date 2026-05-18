

package iad1tya.echo.music.playback

internal fun resolveStreamChunkLength(
    requestedLength: Long,
    position: Long,
    knownContentLength: Long?,
    chunkLength: Long,
): Long? {
    if (chunkLength <= 0L || position < 0L) return null

    val remainingLength = knownContentLength?.minus(position)?.takeIf { it > 0L }
    val resolvedLength =
        listOfNotNull(
            chunkLength,
            requestedLength.takeIf { it > 0L },
            remainingLength,
        ).minOrNull()

    return resolvedLength?.takeIf { it > 0L }
}

package echo.desktop

import com.music.innertube.models.SongItem
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Shows the current song in Discord ("Listening to") through Discord's local named pipe.
 * Needs an application id from discord.com/developers (Settings). Fails silently when Discord is closed.
 */
object Discord {
    private var pipe: RandomAccessFile? = null
    private var connectedId = ""

    private fun send(op: Int, json: String) {
        val body = json.toByteArray()
        val buf = ByteBuffer.allocate(8 + body.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(op).putInt(body.size).put(body)
        pipe!!.write(buf.array())
        val head = ByteArray(8)
        pipe!!.readFully(head) // Discord always answers; drain it
        val len = ByteBuffer.wrap(head, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        pipe!!.readFully(ByteArray(len))
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

    @Synchronized
    fun show(song: SongItem) {
        val id = Settings.discordId.trim()
        if (id.isEmpty()) return
        runCatching {
            if (pipe == null || connectedId != id) {
                pipe?.close()
                pipe = (0..9).firstNotNullOfOrNull { runCatching { RandomAccessFile("\\\\.\\pipe\\discord-ipc-$it", "rw") }.getOrNull() }
                connectedId = id
                send(0, """{"v":1,"client_id":"${esc(id)}"}""")
            }
            val activity =
                """{"details":"${esc(song.title)}","state":"${esc(song.artists.joinToString { it.name })}",""" +
                    """"timestamps":{"start":${System.currentTimeMillis() / 1000}}}"""
            send(1, """{"cmd":"SET_ACTIVITY","args":{"pid":${ProcessHandle.current().pid()},"activity":$activity},"nonce":"${System.nanoTime()}"}""")
        }.onFailure {
            runCatching { pipe?.close() }
            pipe = null
        }
    }
}

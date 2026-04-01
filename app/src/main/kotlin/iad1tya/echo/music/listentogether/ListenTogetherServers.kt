/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package iad1tya.echo.music.listentogether

data class ListenTogetherServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String
)

object ListenTogetherServers {
    val servers: List<ListenTogetherServer> = listOf(
        ListenTogetherServer(
            name = "The Meowery",
            url = "wss://metroserverx.meowery.eu/ws",
            location = "Poland",
            operator = "Nyx",
        ),
    )

    val defaultServerUrl: String
        get() = servers.first().url

    fun findByUrl(url: String): ListenTogetherServer? = servers.firstOrNull { it.url == url }
}

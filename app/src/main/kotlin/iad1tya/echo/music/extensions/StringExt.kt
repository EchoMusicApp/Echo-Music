package iad1tya.echo.music.extensions

import androidx.sqlite.db.SimpleSQLiteQuery
import java.net.InetSocketAddress
import java.net.InetSocketAddress.createUnresolved

inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T =
    if (this == null) {
        defaultValue
    } else {
        try {
            enumValueOf(this)
        } catch (e: IllegalArgumentException) {
            defaultValue
        }
    }

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

fun String.toInetSocketAddress(): InetSocketAddress {
    val (host, port) = split(":")
    return createUnresolved(host, port.toInt())
}

fun String.resize(width: Int, height: Int): String {
    return if (contains("ggpht.com")) {
        if (contains("=w")) {
            replace(Regex("=w\\d+-h\\d+"), "=w$width-h$height")
        } else {
            "$this=w$width-h$height"
        }
    } else {
        this
    }
}

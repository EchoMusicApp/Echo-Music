package echo.music.iad1tya.models

import kotlinx.serialization.Serializable

@Serializable
data class AccountData(
    val name: String,
    val email: String,
    val channelHandle: String,
    val cookie: String,
    val visitorData: String,
    val dataSyncId: String,
    val avatarUrl: String = ""
)

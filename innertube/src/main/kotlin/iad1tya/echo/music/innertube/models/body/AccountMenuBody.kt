




package iad1tya.echo.music.innertube.models.body

import iad1tya.echo.music.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class AccountMenuBody(
    val context: Context,
    val deviceTheme: String = "DEVICE_THEME_SELECTED",
    val userInterfaceTheme: String = "USER_INTERFACE_THEME_DARK",
)

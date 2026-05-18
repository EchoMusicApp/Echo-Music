


package iad1tya.echo.music.together

import kotlinx.serialization.json.Json

object TogetherJson {
    val json: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
            classDiscriminator = "type"
        }
}

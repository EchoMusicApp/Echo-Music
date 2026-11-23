package echo.music.dsp.core.models

import kotlinx.serialization.Serializable

@Serializable
enum class FilterType {
  PK,
  LSC,
  HSC,
  LPQ,
  HPQ
}

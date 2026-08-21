package echo.music.iad1tya.eq.data

import kotlinx.serialization.Serializable

@Serializable
enum class FilterType {
    
    PK,
    
    LSC,
    
    HSC,
    
    LPQ,
    
    HPQ
}
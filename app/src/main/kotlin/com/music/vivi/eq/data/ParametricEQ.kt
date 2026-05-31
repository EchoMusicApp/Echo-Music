package iad1tya.echo.music.eq.data

import kotlinx.serialization.Serializable


@Serializable
data class ParametricEQBand(
    val frequency: Double,                      
    val gain: Double,                           
    val q: Double = 1.41,                       
    val filterType: FilterType = FilterType.PK, 
    val enabled: Boolean = true                 
)


@Serializable
data class ParametricEQ(
    val preamp: Double,                         
    val bands: List<ParametricEQBand>,          
    val metadata: Map<String, String> = emptyMap()  
) {
    companion object {
        const val MAX_BANDS = 20  
    }
}
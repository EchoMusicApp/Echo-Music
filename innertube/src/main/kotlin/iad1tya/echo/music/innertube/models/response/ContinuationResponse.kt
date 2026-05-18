




package iad1tya.echo.music.innertube.models.response
 
 import iad1tya.echo.music.innertube.models.MusicShelfRenderer
 import kotlinx.serialization.Serializable
 
 @Serializable
 data class ContinuationResponse(
     val onResponseReceivedActions: List<ResponseAction>?,
 ) {
 
     @Serializable
     data class ResponseAction(
         val appendContinuationItemsAction: ContinuationItems?,
     )
 
     @Serializable
     data class ContinuationItems(
         val continuationItems: List<MusicShelfRenderer.Content>?,
     )
 }

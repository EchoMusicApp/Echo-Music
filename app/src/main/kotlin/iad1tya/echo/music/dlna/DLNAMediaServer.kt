package iad1tya.echo.music.dlna

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream

class DLNAMediaServer(private val port: Int = 8080) : NanoHTTPD(port) {
    private val TAG = "DLNAMediaServer"
    private val httpClient = OkHttpClient()
    
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d(TAG, "Serving request: $uri")
        
        return try {
            // Extract the original URL from the path
            // Format: /proxy?url=<encoded_url>
            val params = session.parameters
            val originalUrl = params["url"]?.firstOrNull()
            
            if (originalUrl.isNullOrEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing URL parameter")
            }
            
            // Fetch the media from the original URL and proxy it
            val request = Request.Builder()
                .url(originalUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    MIME_PLAINTEXT,
                    "Failed to fetch media: ${response.code}"
                )
            }
            
            val body = response.body ?: return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Empty response body"
            )
            
            // Get content type from original response
            val contentType = response.header("Content-Type") ?: "audio/mpeg"
            val contentLength = body.contentLength()
            
            // Handle range requests for seeking
            val rangeHeader = session.headers["range"]
            if (rangeHeader != null) {
                Log.d(TAG, "Range request: $rangeHeader")
                return handleRangeRequest(body.byteStream(), contentLength, rangeHeader, contentType)
            }
            
            // Return full content
            newChunkedResponse(Response.Status.OK, contentType, body.byteStream())
        } catch (e: Exception) {
            Log.e(TAG, "Error serving media", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Error: ${e.message}"
            )
        }
    }
    
    private fun handleRangeRequest(
        inputStream: InputStream,
        totalLength: Long,
        rangeHeader: String,
        contentType: String
    ): Response {
        // Parse range header: bytes=start-end
        val range = rangeHeader.removePrefix("bytes=").split("-")
        val start = range[0].toLongOrNull() ?: 0
        val end = if (range.size > 1 && range[1].isNotEmpty()) {
            range[1].toLong()
        } else {
            totalLength - 1
        }
        
        val contentLength = end - start + 1
        
        // Skip to start position
        inputStream.skip(start)
        
        val response = newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT,
            contentType,
            inputStream,
            contentLength
        )
        
        response.addHeader("Content-Range", "bytes $start-$end/$totalLength")
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Length", contentLength.toString())
        
        return response
    }
    
    fun getProxyUrl(originalUrl: String): String {
        // Get local IP address
        val localAddress = getLocalIpAddress()
        // URL encode the original URL
        val encodedUrl = java.net.URLEncoder.encode(originalUrl, "UTF-8")
        return "http://$localAddress:$port/proxy?url=$encodedUrl"
    }
    
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP", e)
        }
        return "127.0.0.1"
    }
}

package iad1tya.echo.music.dlna

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DLNAPlayer(private val device: DLNADevice) {
    private val TAG = "DLNAPlayer"
    
    suspend fun setAVTransportURI(mediaUrl: String, title: String, artist: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val soapAction = "urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI"
            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                            <InstanceID>0</InstanceID>
                            <CurrentURI>$mediaUrl</CurrentURI>
                            <CurrentURIMetaData>
                                &lt;DIDL-Lite xmlns=&quot;urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/&quot; 
                                           xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot; 
                                           xmlns:upnp=&quot;urn:schemas-upnp-org:metadata-1-0/upnp/&quot;&gt;
                                    &lt;item id=&quot;0&quot; parentID=&quot;-1&quot; restricted=&quot;1&quot;&gt;
                                        &lt;dc:title&gt;$title&lt;/dc:title&gt;
                                        &lt;dc:creator&gt;$artist&lt;/dc:creator&gt;
                                        &lt;upnp:class&gt;object.item.audioItem.musicTrack&lt;/upnp:class&gt;
                                        &lt;res protocolInfo=&quot;http-get:*:audio/mpeg:*&quot;&gt;$mediaUrl&lt;/res&gt;
                                    &lt;/item&gt;
                                &lt;/DIDL-Lite&gt;
                            </CurrentURIMetaData>
                        </u:SetAVTransportURI>
                    </s:Body>
                </s:Envelope>
            """.trimIndent()
            
            sendSOAPRequest(soapAction, soapBody)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting AV transport URI", e)
            false
        }
    }
    
    suspend fun play(): Boolean = withContext(Dispatchers.IO) {
        try {
            val soapAction = "urn:schemas-upnp-org:service:AVTransport:1#Play"
            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                            <InstanceID>0</InstanceID>
                            <Speed>1</Speed>
                        </u:Play>
                    </s:Body>
                </s:Envelope>
            """.trimIndent()
            
            sendSOAPRequest(soapAction, soapBody)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing", e)
            false
        }
    }
    
    suspend fun pause(): Boolean = withContext(Dispatchers.IO) {
        try {
            val soapAction = "urn:schemas-upnp-org:service:AVTransport:1#Pause"
            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        <u:Pause xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                            <InstanceID>0</InstanceID>
                        </u:Pause>
                    </s:Body>
                </s:Envelope>
            """.trimIndent()
            
            sendSOAPRequest(soapAction, soapBody)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
            false
        }
    }
    
    suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        try {
            val soapAction = "urn:schemas-upnp-org:service:AVTransport:1#Stop"
            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        <u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                            <InstanceID>0</InstanceID>
                        </u:Stop>
                    </s:Body>
                </s:Envelope>
            """.trimIndent()
            
            sendSOAPRequest(soapAction, soapBody)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
            false
        }
    }
    
    suspend fun seek(target: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val soapAction = "urn:schemas-upnp-org:service:AVTransport:1#Seek"
            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        <u:Seek xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                            <InstanceID>0</InstanceID>
                            <Unit>REL_TIME</Unit>
                            <Target>$target</Target>
                        </u:Seek>
                    </s:Body>
                </s:Envelope>
            """.trimIndent()
            
            sendSOAPRequest(soapAction, soapBody)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
            false
        }
    }
    
    suspend fun setVolume(volume: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val soapAction = "urn:schemas-upnp-org:service:RenderingControl:1#SetVolume"
            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        <u:SetVolume xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                            <InstanceID>0</InstanceID>
                            <Channel>Master</Channel>
                            <DesiredVolume>$volume</DesiredVolume>
                        </u:SetVolume>
                    </s:Body>
                </s:Envelope>
            """.trimIndent()
            
            // Note: Volume control might use a different URL, but we'll try the control URL first
            sendSOAPRequest(soapAction, soapBody)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
            false
        }
    }
    
    private fun sendSOAPRequest(soapAction: String, soapBody: String): Boolean {
        return try {
            val url = URL(device.controlUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
            connection.setRequestProperty("SOAPAction", "\"$soapAction\"")
            
            // Write SOAP body
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(soapBody)
            writer.flush()
            writer.close()
            
            // Read response
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            }
            
            connection.disconnect()
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "SOAP request successful: $soapAction")
                true
            } else {
                Log.e(TAG, "SOAP request failed with code $responseCode: $response")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SOAP request: $soapAction", e)
            false
        }
    }
}

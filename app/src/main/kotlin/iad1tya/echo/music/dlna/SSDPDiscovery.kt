package iad1tya.echo.music.dlna

import android.util.Log
import kotlinx.coroutines.*
import org.w3c.dom.Element
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import javax.xml.parsers.DocumentBuilderFactory

class SSDPDiscovery {
    private val TAG = "SSDPDiscovery"
    private val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
    private val SSDP_PORT = 1900
    private val SEARCH_TARGET = "urn:schemas-upnp-org:device:MediaRenderer:1"
    
    private var isSearching = false
    private var searchJob: Job? = null
    
    fun startDiscovery(
        scope: CoroutineScope,
        onDeviceFound: (DLNADevice) -> Unit
    ) {
        if (isSearching) return
        
        isSearching = true
        searchJob = scope.launch(Dispatchers.IO) {
            try {
                discoverDevices(onDeviceFound)
            } catch (e: Exception) {
                Log.e(TAG, "Error during discovery", e)
            }
        }
    }
    
    fun stopDiscovery() {
        isSearching = false
        searchJob?.cancel()
    }
    
    private suspend fun discoverDevices(onDeviceFound: (DLNADevice) -> Unit) {
        val searchMessage = buildSSDPSearchMessage()
        val socket = DatagramSocket(null).apply {
            reuseAddress = true
            broadcast = true
        }
        
        try {
            // Send M-SEARCH request
            val group = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
            val packet = DatagramPacket(
                searchMessage.toByteArray(),
                searchMessage.length,
                group,
                SSDP_PORT
            )
            
            socket.send(packet)
            Log.d(TAG, "Sent SSDP M-SEARCH request")
            
            // Listen for responses
            socket.soTimeout = 5000
            val buffer = ByteArray(8192)
            
            while (isSearching) {
                try {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    
                    val response = String(responsePacket.data, 0, responsePacket.length)
                    Log.d(TAG, "Received SSDP response from ${responsePacket.address}")
                    
                    parseResponse(response)?.let { location ->
                        fetchDeviceDescription(location)?.let { device ->
                            withContext(Dispatchers.Main) {
                                onDeviceFound(device)
                            }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // Timeout is expected, resend search
                    socket.send(packet)
                } catch (e: Exception) {
                    if (isSearching) {
                        Log.e(TAG, "Error receiving response", e)
                    }
                }
            }
        } finally {
            socket.close()
        }
    }
    
    private fun buildSSDPSearchMessage(): String {
        return "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: $SSDP_MULTICAST_ADDRESS:$SSDP_PORT\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 3\r\n" +
                "ST: $SEARCH_TARGET\r\n" +
                "\r\n"
    }
    
    private fun parseResponse(response: String): String? {
        val lines = response.split("\r\n")
        for (line in lines) {
            if (line.startsWith("LOCATION:", ignoreCase = true)) {
                return line.substring(9).trim()
            }
        }
        return null
    }
    
    private suspend fun fetchDeviceDescription(location: String): DLNADevice? = withContext(Dispatchers.IO) {
        try {
            val url = URL(location)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val xml = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            parseDeviceDescription(xml, location)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device description from $location", e)
            null
        }
    }
    
    private fun parseDeviceDescription(xml: String, location: String): DLNADevice? {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(xml.byteInputStream())
            
            val deviceElement = doc.getElementsByTagName("device").item(0) as? Element ?: return null
            
            val udn = getElementValue(deviceElement, "UDN") ?: return null
            val friendlyName = getElementValue(deviceElement, "friendlyName") ?: "Unknown Device"
            val manufacturer = getElementValue(deviceElement, "manufacturer") ?: "Unknown"
            val modelName = getElementValue(deviceElement, "modelName") ?: "Unknown"
            
            // Find AVTransport service
            val serviceList = deviceElement.getElementsByTagName("service")
            var controlUrl = ""
            var eventSubUrl = ""
            
            for (i in 0 until serviceList.length) {
                val service = serviceList.item(i) as Element
                val serviceType = getElementValue(service, "serviceType") ?: continue
                
                if (serviceType.contains("AVTransport")) {
                    controlUrl = getElementValue(service, "controlURL") ?: ""
                    eventSubUrl = getElementValue(service, "eventSubURL") ?: ""
                    break
                }
            }
            
            val baseUrl = location.substring(0, location.indexOf("/", 8))
            val fullControlUrl = if (controlUrl.startsWith("http")) {
                controlUrl
            } else {
                baseUrl + controlUrl
            }
            
            val fullEventSubUrl = if (eventSubUrl.startsWith("http")) {
                eventSubUrl
            } else {
                baseUrl + eventSubUrl
            }
            
            return DLNADevice(
                id = udn,
                name = friendlyName,
                location = location,
                controlUrl = fullControlUrl,
                eventSubUrl = fullEventSubUrl,
                manufacturer = manufacturer,
                modelName = modelName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing device description", e)
            return null
        }
    }
    
    private fun getElementValue(element: Element, tagName: String): String? {
        val nodeList = element.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            val node = nodeList.item(0)
            return node.textContent
        }
        return null
    }
}

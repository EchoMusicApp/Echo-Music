package android.net

// shortcut: desktop is always treated as "online over ethernet"; real reachability is checked by the actual requests failing.
class Network
class NetworkCapabilities {
    fun hasTransport(t: Int) = t == TRANSPORT_ETHERNET
    fun hasCapability(c: Int) = true
    companion object {
        const val TRANSPORT_CELLULAR = 0
        const val TRANSPORT_WIFI = 1
        const val TRANSPORT_ETHERNET = 3
        const val NET_CAPABILITY_INTERNET = 12
        const val NET_CAPABILITY_VALIDATED = 16
    }
}
class ConnectivityManager {
    val activeNetwork: Network? get() = Network()
    val isActiveNetworkMetered: Boolean get() = false
    fun getNetworkCapabilities(network: Network?): NetworkCapabilities? = NetworkCapabilities()
}

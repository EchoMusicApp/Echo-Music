package iad1tya.echo.music.dlna

data class DLNADevice(
    val id: String,
    val name: String,
    val location: String,
    val controlUrl: String,
    val eventSubUrl: String,
    val manufacturer: String,
    val modelName: String,
    val isConnected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DLNADevice

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

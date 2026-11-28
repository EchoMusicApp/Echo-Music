package android.util

// Desktop stand-in for android.util.Base64 (only the calls the app uses).
object Base64 {
    const val DEFAULT = 0
    const val NO_PADDING = 1
    const val NO_WRAP = 2
    fun encodeToString(input: ByteArray, flags: Int): String = java.util.Base64.getEncoder().encodeToString(input)
    fun decode(input: String, flags: Int): ByteArray = java.util.Base64.getMimeDecoder().decode(input)
}

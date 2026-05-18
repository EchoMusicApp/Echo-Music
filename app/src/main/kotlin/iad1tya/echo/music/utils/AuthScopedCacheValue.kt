


package iad1tya.echo.music.utils

data class AuthScopedCacheValue(
    val url: String,
    val expiresAtMs: Long,
    val authFingerprint: String,
) {
    fun isValidFor(
        authFingerprint: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        return this.authFingerprint == authFingerprint && expiresAtMs > nowMs
    }
}

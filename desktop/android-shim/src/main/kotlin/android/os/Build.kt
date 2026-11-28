package android.os

// Desktop stand-in: reports a recent Android level so "is this feature available" checks in the original code pass.
// shortcut: checks that really test for old Android versions are always true here, fine for a desktop port.
object Build {
    const val MODEL = "Windows PC"
    const val DEVICE = "desktop"
    const val BRAND = "windows"
    const val MANUFACTURER = "PC"
    const val PRODUCT = "desktop"
    const val HARDWARE = "desktop"
    const val FINGERPRINT = "desktop"
    object VERSION {
        const val SDK_INT = 36
        const val RELEASE = "16"
    }
    object VERSION_CODES {
        const val M = 23
        const val O = 26
        const val P = 28
        const val Q = 29
        const val R = 30
        const val S = 31
        const val TIRAMISU = 33
        const val UPSIDE_DOWN_CAKE = 34
        const val BAKLAVA = 36
    }
}

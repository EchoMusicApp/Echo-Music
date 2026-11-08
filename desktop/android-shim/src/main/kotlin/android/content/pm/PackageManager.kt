package android.content.pm

class PackageInfo { var versionName: String? = "desktop" }

class PackageManager {
    fun getPackageInfo(packageName: String, flags: Int): PackageInfo = PackageInfo()
    // shortcut: no launcher on desktop; the restart-the-app code path just gets a plain intent.
    fun getLaunchIntentForPackage(packageName: String): android.content.Intent = android.content.Intent()
}

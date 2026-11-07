package android.content.pm

class PackageInfo { var versionName: String? = "desktop" }

class PackageManager { fun getPackageInfo(packageName: String, flags: Int): PackageInfo = PackageInfo() }

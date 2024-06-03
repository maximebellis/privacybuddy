package be.kuleuven.privacybuddy.extension

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import be.kuleuven.privacybuddy.R

fun Context.getAppIconByName(appName: String): Drawable? {
    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED) {
        return ContextCompat.getDrawable(this, R.mipmap.ic_launcher)
    }
    val appIcon = packageManager.getInstalledApplications(PackageManager.GET_META_DATA).find {
        packageManager.getApplicationLabel(it).toString() == appName
    }?.loadIcon(packageManager)
    return appIcon ?: ContextCompat.getDrawable(this, R.mipmap.ic_launcher)
}

fun Context.getAppNameByPackageName(packageName: String): String {
    val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
    return packageManager.getApplicationLabel(applicationInfo).toString()
}

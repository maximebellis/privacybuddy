package be.kuleuven.privacybuddy

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

fun Context.getAppIconByName(appName: String): Drawable? {
    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED) {
        // Consider showing rationale and requesting permission if necessary.
        return null
    }
    return packageManager.getInstalledApplications(PackageManager.GET_META_DATA).find {
        packageManager.getApplicationLabel(it).toString() == appName
    }?.loadIcon(packageManager)
}

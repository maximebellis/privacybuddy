package be.kuleuven.privacybuddy.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executor


object AppOpsUtility {
    // Checks if a specific app has accessed location recently for Android 11 and above


    fun setupLocationAccessListener(context: Context) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        // Define an executor that runs on the main thread
        val mainThreadExecutor = Executor { command ->
            Handler(Looper.getMainLooper()).post(command)
        }

        val listener = AppOpsManager.OnOpActiveChangedListener { op, uid, packageName, active ->
            // Log the location access details
            if (op == AppOpsManager.OPSTR_FINE_LOCATION || op == AppOpsManager.OPSTR_COARSE_LOCATION) {
                Log.d("LocationAccess", "App $packageName (UID: $uid) accessed location. Active: $active")
            }
        }

        // Start watching for both fine and coarse location operations using the defined executor
        appOps.startWatchingActive(
            arrayOf(AppOpsManager.OPSTR_FINE_LOCATION, AppOpsManager.OPSTR_COARSE_LOCATION),
            mainThreadExecutor,
            listener
        )

        // Remember to properly manage the lifecycle of this listener
    }

}


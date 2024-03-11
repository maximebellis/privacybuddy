package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_main)

        // Ensure you call setContentView() before accessing window features
        supportActionBar?.hide()

        // Hide the status bar and navigation bar
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Make the status bar transparent
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        // For navigation bar color as well if needed
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        // Ensure any padding or insets are handled by your layout
        // This can be done by handling WindowInsets in your layout's View(s)
    }
}

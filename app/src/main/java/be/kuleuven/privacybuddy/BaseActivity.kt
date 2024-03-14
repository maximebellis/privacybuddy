package be.kuleuven.privacybuddy

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_main) // Assuming this is common
        setupToolbar()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d("ToolbarActions", "onCreateOptionsMenu called")
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("ToolbarActions", "onOptionsItemSelected called")
        return when (item.itemId) {
            R.id.action_back -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                // Handle settings action
                true
            }
            R.id.action_timespan -> {
                // Handle settings action
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }


}

package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar


abstract class BaseActivity : AppCompatActivity() {

    object AppSettings {
        var daysFilter: Int = 21
    }


    abstract fun filterData(days: Int)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background_prim)

    }



    protected fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_prim)
    }

    protected fun setupToolbarWithNestedScrollListener(scrollableViewId: Int, titleViewId: Int, toolbarTitle: String) {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val titleView: TextView = findViewById(titleViewId)
        val nestedScrollView: NestedScrollView = findViewById(scrollableViewId)

        nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            toolbar.title = if (scrollY >= titleView.bottom) toolbarTitle else ""
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menuInflater.inflate(R.menu.timespan_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {

                startActivity(intent)
                true
            }
            R.id.action_one_day -> {
                filterData(1)
                true
            }
            R.id.action_seven_days -> {
                filterData(7)
                true
            }
            R.id.action_twenty_one_days -> {
                filterData(21)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}


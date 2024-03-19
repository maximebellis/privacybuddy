package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.appbar.MaterialToolbar


abstract class BaseActivity : AppCompatActivity() {

    object AppSettings {
        var daysFilter: Int = 1
    }

    abstract fun filterData(days: Int)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    protected fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_prim)
    }

    protected fun setupToolbarWithScrollListener(scrollableViewId: Int, titleViewId: Int, toolbarTitle: String) {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val titleView: TextView = findViewById(titleViewId)
        val nestedScrollView: NestedScrollView = findViewById(scrollableViewId)

        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY >= titleView.bottom) {
                toolbar.title = toolbarTitle
            } else {
                toolbar.title = ""
            }
        })
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
                Toast.makeText(this, "Refresh clicked", Toast.LENGTH_SHORT).show()
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


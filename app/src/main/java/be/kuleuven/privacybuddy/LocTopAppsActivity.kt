package be.kuleuven.privacybuddy

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.adapter.DisplayMode
import be.kuleuven.privacybuddy.adapter.TopAppsAdapter
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import be.kuleuven.privacybuddy.data.AppAccessStats


class LocTopAppsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var topAppsAdapter: TopAppsAdapter
    private lateinit var textViewDescription: TextView

    override fun filterData(days: Int) {
        AppSettings.daysFilter = days
        initUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_top_apps)

        initUI()
        setupToolbar()

        val topAppsTextViewChoice: TextView = findViewById(R.id.topAppsTextViewChoice)
        topAppsTextViewChoice.setOnClickListener {
            showSortingPopup(it)
        }
    }

    private fun initUI() {
        LocationDataUtils.buildAppAccessStatsFromGeoJson(this)

        recyclerView = findViewById(R.id.recyclerViewTopAppsLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)
        textViewDescription = findViewById(R.id.textViewTimeline)
        AppState.topAccessedAppsCache?.let { updateTopAccessedApps(it, DisplayMode.ACCESS_COUNT) }
    }

    private fun showSortingPopup(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.top_apps_sorting_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            val textViewChoice: TextView = findViewById(R.id.topAppsTextViewChoice)
            when (menuItem.itemId) {
                R.id.menu_location_accesses -> {
                    sortAppsByAccessCount()
                    textViewChoice.text = getString(R.string.sort_by_accesses)
                    textViewDescription.text = getString(R.string.top_apps_text)
                }
                R.id.menu_access_frequency -> {
                    sortAppsByFrequency()
                    textViewChoice.text = getString(R.string.sort_by_frequency)
                    textViewDescription.text = getString(R.string.top_apps_access_frequency)
                }
                R.id.menu_privacy_score -> {
                    sortAppsByPrivacyScore()
                    textViewChoice.text = getString(R.string.sort_by_privacy_score)
                    textViewDescription.text = getString(R.string.top_apps_privacy_score)
                }
                else -> false
            }
            true
        }
        popup.show()
    }


    private fun sortAppsByAccessCount(): Boolean {
        val sortedList = AppState.topAccessedAppsCache?.sortedByDescending { it.totalAccesses } ?: emptyList()
        updateTopAccessedApps(sortedList, DisplayMode.ACCESS_COUNT)
        return true
    }

    private fun sortAppsByFrequency(): Boolean {
        val sortedList = AppState.topAccessedAppsCache?.sortedByDescending { it.totalAccesses.toDouble() / it.days } ?: emptyList()
        updateTopAccessedApps(sortedList, DisplayMode.FREQUENCY)
        return true
    }

    private fun sortAppsByPrivacyScore(): Boolean {
        val sortedList = AppState.topAccessedAppsCache?.sortedByDescending { it.privacyScore } ?: emptyList()
        updateTopAccessedApps(sortedList, DisplayMode.PRIVACY_SCORE)
        return true
    }

    private fun updateTopAccessedApps(sortedList: List<AppAccessStats>, mode: DisplayMode) {
        topAppsAdapter = TopAppsAdapter(this, sortedList, mode)
        recyclerView.adapter = topAppsAdapter
    }



}

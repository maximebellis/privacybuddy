package be.kuleuven.privacybuddy

import android.content.res.Resources
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.DisplayMode
import be.kuleuven.privacybuddy.adapter.TopAppsAdapter
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import be.kuleuven.privacybuddy.data.AppAccessStats

class LocTopAppsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var topAppsAdapter: TopAppsAdapter
    private lateinit var textViewDescription: TextView
    private var currentDisplayMode: DisplayMode = DisplayMode.PRIVACY_SCORE

    override fun filterData(days: Int) {
        daysFilter = days
        LocationDataUtils.buildAppAccessStatsFromGeoJson(this)
        updateUIBasedOnDays(days)
    }


    private fun updateUIBasedOnDays(days: Int) {
        if (days == 1) {
            textViewDescription.text = getString(R.string.privacy_scores_not_calculated)
            updateTopAccessedApps(emptyList(), currentDisplayMode)
        } else {
            textViewDescription.text = "This list ranks your apps by their privacy score, where a higher score indicates better privacy practices.\nShowing data for $days days."
            AppState.topAccessedAppsCache?.let { updateTopAccessedApps(it, currentDisplayMode)
                sortAppsBasedOnCurrentDisplayMode()
            }
        }
    }

    private fun sortAppsBasedOnCurrentDisplayMode() {
        when (currentDisplayMode) {
            DisplayMode.ACCESS_COUNT -> sortAppsByAccessCount()
            DisplayMode.FREQUENCY -> sortAppsByFrequency()
            DisplayMode.PRIVACY_SCORE -> sortAppsByPrivacyScore()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_top_apps)
        initUI()
        setupToolbar()
        AppState.topAccessedAppsCache?.let { updateTopAccessedApps(it, currentDisplayMode) }
        val topAppsTextViewChoice: TextView = findViewById(R.id.topAppsTextViewChoice)
        topAppsTextViewChoice.setOnClickListener {
            showSortingPopup(it)
        }
        updateUIBasedOnDays(daysFilter)
    }

    private fun initUI() {
        recyclerView = findViewById(R.id.recyclerViewTopAppsLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)
        textViewDescription = findViewById(R.id.textViewTimeline)
        AppState.topAccessedAppsCache?.let { updateTopAccessedApps(it, currentDisplayMode) }
    }

    private fun showSortingPopup(anchor: View) {
        val wrapper = ContextThemeWrapper(this, R.style.PopupMenuStyle)
        val popup = PopupMenu(wrapper, anchor)
        popup.menuInflater.inflate(R.menu.top_apps_sorting_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            val textViewChoice: TextView = findViewById(R.id.topAppsTextViewChoice)
            when (menuItem.itemId) {
                R.id.menu_location_accesses -> {
                    currentDisplayMode = DisplayMode.ACCESS_COUNT
                    sortAppsByAccessCount()
                    textViewChoice.text = getString(R.string.sort_by_accesses)
                    textViewDescription.text = getString(R.string.top_apps_text)
                }
                R.id.menu_access_frequency -> {
                    currentDisplayMode = DisplayMode.FREQUENCY
                    sortAppsByFrequency()
                    textViewChoice.text = getString(R.string.sort_by_frequency)
                    textViewDescription.text = getString(R.string.top_apps_access_frequency)
                }
                R.id.menu_privacy_score -> {
                    currentDisplayMode = DisplayMode.PRIVACY_SCORE
                    sortAppsByPrivacyScore()
                    textViewChoice.text = getString(R.string.sort_by_privacy_score)
                    textViewDescription.text = getString(R.string.top_apps_privacy_score)
                }
                else -> false
            }
            true
        }

        anchor.post {
            val location = IntArray(2)
            anchor.getLocationOnScreen(location)
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels
            popup.menu.findItem(R.id.menu_location_accesses).actionView?.layoutParams?.width = screenWidth - location[0] - anchor.paddingRight
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
        val sortedList = AppState.topAccessedAppsCache?.sortedBy { it.privacyScore } ?: emptyList()
        updateTopAccessedApps(sortedList, DisplayMode.PRIVACY_SCORE)
        return true
    }

    private fun updateTopAccessedApps(sortedList: List<AppAccessStats>, mode: DisplayMode) {
        topAppsAdapter = TopAppsAdapter(this, sortedList, mode)
        recyclerView.adapter = topAppsAdapter
    }

}

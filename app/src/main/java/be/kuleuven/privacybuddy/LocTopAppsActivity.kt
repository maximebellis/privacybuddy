package be.kuleuven.privacybuddy

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.PopupWindow
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
    private lateinit var textViewChoice: TextView
    private lateinit var infoButton: ImageView
    private var currentDisplayMode: DisplayMode = DisplayMode.PRIVACY_SCORE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_top_apps)
        initUI()
        setupToolbar()
        updateDataAndView()
    }

    private fun initUI() {
        recyclerView = findViewById(R.id.recyclerViewTopAppsLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)
        textViewDescription = findViewById(R.id.textViewTimeline)
        textViewChoice = findViewById(R.id.topAppsTextViewChoice)
        infoButton = findViewById(R.id.infoButtonPrivacyScore)

        textViewChoice.setOnClickListener { showSortingPopup(it) }
        if (currentDisplayMode == DisplayMode.PRIVACY_SCORE) {
            infoButton.visibility = View.VISIBLE
            infoButton.setOnClickListener { showInfoPopup(it, getString(R.string.privacy_score_explanation)) }
        } else {
            infoButton.visibility = View.GONE
        }
    }

    override fun filterData(days: Int) {
        daysFilter = days
        LocationDataUtils.buildAppAccessStatsFromGeoJson(this)
        updateDataAndView()
    }

    private fun showInfoPopup(anchor: View, text: String) {
        val inflater = LayoutInflater.from(anchor.context)
        val popupView = inflater.inflate(R.layout.popup_info, null)
        val textViewPopupContent: TextView = popupView.findViewById(R.id.textViewPopupContent)
        textViewPopupContent.text = text

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = 10f
            isOutsideTouchable = true
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val xOff = anchor.width / 2 - popupView.measuredWidth / 2
        val yOff = -popupView.measuredHeight

        popupWindow.showAsDropDown(anchor, xOff, yOff, Gravity.START)
    }

    private fun updateDataAndView() {
        val days = daysFilter
        if (days == 1) {
            textViewDescription.text = getString(R.string.privacy_scores_not_calculated)
            updateTopAccessedApps(emptyList())
        } else {
            textViewDescription.text = "This list ranks your apps by their privacy score, where a higher score indicates better privacy practices.\nShowing data for $days days."
            AppState.topAccessedAppsCache?.let {
                updateTopAccessedApps(it)
                sortAppsBasedOnCurrentDisplayMode()
            }
        }
    }

    private fun showSortingPopup(anchor: View) {
        val popup = PopupMenu(ContextThemeWrapper(this, R.style.PopupMenuStyle), anchor)
        popup.menuInflater.inflate(R.menu.top_apps_sorting_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_location_accesses -> setDisplayMode(DisplayMode.ACCESS_COUNT, getString(R.string.sort_by_accesses))
                R.id.menu_access_frequency -> setDisplayMode(DisplayMode.FREQUENCY, getString(R.string.sort_by_frequency))
                R.id.menu_privacy_score -> setDisplayMode(DisplayMode.PRIVACY_SCORE, getString(R.string.sort_by_privacy_score))
            }
            true
        }
        popup.show()
    }

    private fun setDisplayMode(mode: DisplayMode, text: String) {
        currentDisplayMode = mode
        textViewChoice.text = text
        sortAppsBasedOnCurrentDisplayMode()
    }

    private fun sortAppsBasedOnCurrentDisplayMode() {
        val sortedList = AppState.topAccessedAppsCache?.let { cache ->
            when (currentDisplayMode) {
                DisplayMode.ACCESS_COUNT -> cache.sortedByDescending { it.totalAccesses }
                DisplayMode.FREQUENCY -> cache.sortedByDescending { it.totalAccesses.toDouble() / it.days }
                DisplayMode.PRIVACY_SCORE -> cache.sortedBy { it.privacyScore }
            }
        } ?: emptyList()
        updateTopAccessedApps(sortedList)
        infoButton.visibility = if (currentDisplayMode == DisplayMode.PRIVACY_SCORE) View.VISIBLE else View.GONE
    }

    private fun updateTopAccessedApps(sortedList: List<AppAccessStats>) {
        topAppsAdapter = TopAppsAdapter(this, sortedList, currentDisplayMode)
        recyclerView.adapter = topAppsAdapter
    }
}


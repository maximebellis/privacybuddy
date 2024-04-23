package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.AppState.globalData
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.LocationEventAdapter
import be.kuleuven.privacybuddy.adapter.SpinnerAdapter
import be.kuleuven.privacybuddy.data.SpinnerItem
import be.kuleuven.privacybuddy.extension.getAppIconByName
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import kotlinx.coroutines.*

class LocTimelineActivity : BaseActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var adapter: LocationEventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_timeline_location)
        setupToolbar()

        setupRecyclerView()
        setupSpinner()
        refreshEvents()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTimelineLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LocationEventAdapter(this)
        recyclerView.adapter = adapter
    }

    override fun onSpinnerItemSelected() {
        refreshEvents()
    }



    override fun onResume() {
        super.onResume()
        refreshEvents()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun refreshEvents() = coroutineScope.launch {
        toggleViewsVisibility(View.VISIBLE, View.GONE, View.GONE)

        val events = withContext(Dispatchers.IO) {
            filterGlobalData(selectedAppName)
        }
        val groupedEvents = withContext(Dispatchers.Default) {
            LocationDataUtils.prepareTimelineItems(events)
        }
        withContext(Dispatchers.Main) {
            adapter.submitList(groupedEvents)
            toggleViewsVisibility(View.GONE, View.VISIBLE, View.VISIBLE)
        }
        updateTimelineText()
    }

    private fun toggleViewsVisibility(progressBarVisibility: Int, textViewVisibility: Int, recyclerViewVisibility: Int) {
        findViewById<ProgressBar>(R.id.progressBar).visibility = progressBarVisibility
        findViewById<TextView>(R.id.textViewTimeline).visibility = textViewVisibility
        findViewById<RecyclerView>(R.id.recyclerViewTimelineLocation).visibility = recyclerViewVisibility
    }

    override fun filterData(days: Int) {
        super.filterData(days)
        refreshEvents()
    }

    private fun updateTimelineText() {
        val timelineTextId = if (daysFilter > 1) R.string.timeline_text else R.string.timeline_text_single_day
        findViewById<TextView>(R.id.textViewTimeline).text = getString(timelineTextId, daysFilter)
    }
}
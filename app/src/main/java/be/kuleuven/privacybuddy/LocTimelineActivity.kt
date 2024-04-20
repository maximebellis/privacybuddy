package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.LocationEventAdapter
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import kotlinx.coroutines.*

class LocTimelineActivity : BaseActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var selectedAppName: String? = null
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

    private fun setupSpinner() {
        val spinner: Spinner = findViewById(R.id.spinnerChooseApp)
        val apps = getUniqueAppNamesFromGeoJson(daysFilter).sorted().toMutableList()
        apps.add(0, "All apps")
        ArrayAdapter(this, R.layout.spinner_item, apps).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedAppName = if (parent.getItemAtPosition(position) == "All apps") null else parent.getItemAtPosition(position) as String
                refreshEvents()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
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
            LocationDataUtils.loadGeoJsonFromAssets(selectedAppName, this@LocTimelineActivity, days = daysFilter)
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
        daysFilter = days
        refreshEvents()
    }

    private fun updateTimelineText() {
        val timelineTextId = if (daysFilter > 1) R.string.timeline_text else R.string.timeline_text_single_day
        findViewById<TextView>(R.id.textViewTimeline).text = getString(timelineTextId, daysFilter)
    }
}
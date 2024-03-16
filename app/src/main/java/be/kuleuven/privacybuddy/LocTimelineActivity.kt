package be.kuleuven.privacybuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.adapter.LocationEventAdapter
import be.kuleuven.privacybuddy.adapter.TimelineItem
import be.kuleuven.privacybuddy.extension.getAppIconByName
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.*

data class LocationEvent(
    val timestamp: String,
    val date: Date,
    val appName: String,
    val usageType: String,
    val interactionType: String
)

class LocTimelineActivity : BaseActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var recyclerView: RecyclerView
    private var selectedAppName: String? = null
    private lateinit var appNameTextView: TextView
    private lateinit var appIconView: ImageView
    private lateinit var adapter: LocationEventAdapter // Declare the adapter

    private val chooseAppLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newAppName = result.data?.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)
            if (newAppName != selectedAppName) {
                selectedAppName = newAppName
                updateChooseAppDisplay(selectedAppName)
                refreshEvents()
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctp_timeline_location)
        setupToolbar()

        recyclerView = findViewById(R.id.recyclerViewTimelineLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LocationEventAdapter(this) // Initialize the adapter
        recyclerView.adapter = adapter // Set the adapter

        appIconView = findViewById(R.id.imageViewAppLogo)
        appNameTextView = findViewById(R.id.textViewSelectedApp)


        selectedAppName = intent.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)
        setupChooseAppButton()
        updateChooseAppDisplay(selectedAppName)
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
        val events = withContext(Dispatchers.IO) {
            loadGeoJsonFromAssets(selectedAppName)
        }
        val groupedEvents = withContext(Dispatchers.Default) {
            prepareTimelineItems(events)
        }
        withContext(Dispatchers.Main) {
            adapter.submitList(groupedEvents) // Use submitList to update data
        }
    }

    private fun loadGeoJsonFromAssets(selectedAppName: String?): List<LocationEvent> {
        return try {
            val featureCollection = parseGeoJsonFromAssets("dummy_location_data.geojson")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            featureCollection.features()?.mapNotNull { feature ->
                val timestamp = feature.getStringProperty("timestamp")
                val date = dateFormat.parse(timestamp)

                date?.let {
                    LocationEvent(
                        timestamp,
                        it,
                        feature.getStringProperty("appName"),
                        feature.getStringProperty("usageType"),
                        feature.getStringProperty("interactionType")
                    )
                }
            }?.filter { selectedAppName == null || it.appName == selectedAppName } ?: emptyList()
        } catch (e: Exception) {
            Log.e("LocTimelineActivity", "Error loading or parsing data", e)
            emptyList()
        }
    }

    private fun prepareTimelineItems(events: List<LocationEvent>): List<TimelineItem> {
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault()) // Reuse formatter
        val sortedEvents = events.sortedByDescending { it.date }
        return sortedEvents
            .groupBy { dateFormatter.format(it.date) }
            .flatMap { (dateString, events) ->
                listOf(TimelineItem.DateLabel(dateFormatter.parse(dateString)!!)) +
                        events.map { TimelineItem.EventItem(it) }
            }
    }


    private fun parseGeoJsonFromAssets(filename: String): FeatureCollection =
        assets.open(filename).use {
            FeatureCollection.fromJson(it.bufferedReader().use(BufferedReader::readText))
        }

    private fun setupChooseAppButton() {
        findViewById<View>(R.id.buttonChooseApp).setOnClickListener {
            chooseAppLauncher.launch(Intent(this, ChooseAppActivity::class.java))
        }
    }

    private fun updateChooseAppDisplay(appName: String?) {
        Log.d("LocTimelineActivity", "Selected app: $appName")
        appNameTextView.text = appName ?: "All Apps"
        appIconView.visibility = if (appName != null) View.VISIBLE else View.GONE
        if (appName != null) {
            appIconView.setImageDrawable(applicationContext.getAppIconByName(appName))
        }
    }

}

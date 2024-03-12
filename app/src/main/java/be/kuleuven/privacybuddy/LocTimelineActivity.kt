package be.kuleuven.privacybuddy


import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.*
import java.io.BufferedReader


data class LocationEvent(
    val timestamp: String,
    val appName: String,
    val usageType: String,
    val interactionType: String
)

class LocTimelineActivity : BaseActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var recyclerView: RecyclerView // Initialize later

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctp_timeline_location)

        recyclerView = findViewById(R.id.recyclerViewTimelineLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)

        coroutineScope.launch {
            val selectedAppName = null // Add logic to retrieve selectedAppName if needed

            try {
                val events = loadGeoJsonFromAssets(selectedAppName)
                displayEvents(events)
            } catch (e: Exception) {
                Log.e("LocTimelineActivity", "Error loading or parsing data", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun displayEvents(events: List<LocationEvent>) {
        recyclerView.adapter = LocationEventAdapter(events, this)
    }

    // Optimized parsing and error handling
    private suspend fun loadGeoJsonFromAssets(selectedAppName: String?): List<LocationEvent> =
        withContext(Dispatchers.IO) {
            try {
                val featureCollection = parseGeoJsonFromAssets("dummy_location_data.geojson")
                featureCollection.features()?.mapNotNull { feature ->
                    LocationEvent(
                        feature.getStringProperty("timestamp"),
                        feature.getStringProperty("appName"),
                        feature.getStringProperty("usageType"),
                        feature.getStringProperty("interactionType")
                    )
                }?.filter { selectedAppName == null || it.appName == selectedAppName } ?: emptyList()
            } catch (e: Exception) {
                Log.e("LocTimelineActivity", "Error loading or parsing data", e)
                emptyList() // Return empty on error
            }
        }

    private fun parseGeoJsonFromAssets(filename: String): FeatureCollection =
        assets.open(filename).use {
            FeatureCollection.fromJson(it.bufferedReader().use(BufferedReader::readText))
        }
}

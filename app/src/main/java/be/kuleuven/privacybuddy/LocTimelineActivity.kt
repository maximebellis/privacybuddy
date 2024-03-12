package be.kuleuven.privacybuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.adapter.LocationEventAdapter
import be.kuleuven.privacybuddy.extension.getAppIconByName
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedReader


data class LocationEvent(
    val timestamp: String,
    val appName: String,
    val usageType: String,
    val interactionType: String
)

class LocTimelineActivity : BaseActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var recyclerView: RecyclerView
    private var selectedAppName: String? = null
    private lateinit var appNameTextView: TextView
    private lateinit var appIconView: ImageView

    private val chooseAppLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        recyclerView = findViewById(R.id.recyclerViewTimelineLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)

        appIconView = findViewById(R.id.imageViewAppLogo)
        appNameTextView = findViewById(R.id.textViewSelectedApp)

        selectedAppName = intent.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)
        setupChooseAppButton()
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

    private fun refreshEvents() {
        coroutineScope.launch {
            try {
                val events = loadGeoJsonFromAssets(selectedAppName)
                displayEvents(events)
            } catch (e: Exception) {
                Log.e("LocTimelineActivity", "Error loading or parsing data", e)
            }
        }
    }

    private fun displayEvents(events: List<LocationEvent>) {
        recyclerView.adapter = LocationEventAdapter(events, this)
    }

    private fun loadGeoJsonFromAssets(selectedAppName: String?): List<LocationEvent> {
        return try {
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
            emptyList()
        }
    }

    private fun parseGeoJsonFromAssets(filename: String): FeatureCollection =
        assets.open(filename).use {
            FeatureCollection.fromJson(it.bufferedReader().use(BufferedReader::readText))
        }

    private fun setupChooseAppButton() {
        updateChooseAppDisplay(selectedAppName)
        findViewById<View>(R.id.buttonChooseApp).setOnClickListener {
            chooseAppLauncher.launch(Intent(this, ChooseAppActivity::class.java))
        }
    }

    private fun updateChooseAppDisplay(appName: String?) {
        Log.d("LocTimelineActivity", "Selected app: $appName")
        appNameTextView.text = appName ?: "All Apps"
        appIconView.visibility = if (appName == null) View.GONE else View.VISIBLE
        if (appName != null) {
            appIconView.setImageDrawable(applicationContext.getAppIconByName(appName))
        }
    }
}





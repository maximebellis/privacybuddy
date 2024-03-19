package be.kuleuven.privacybuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.extension.getAppIconByName
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocMapActivity : BaseActivity(){

    private lateinit var mapView: MapView
    private var selectedAppName: String? = null
    private lateinit var appIconView: ImageView
    private lateinit var appNameTextView: TextView

    private val chooseAppLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val newAppName = result.data?.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)
                if (newAppName != selectedAppName) {
                    selectedAppName = newAppName
                    updateChooseAppDisplay(selectedAppName)
                    setupMapView(selectedAppName)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctp_map_location)
        setupToolbar()

        appIconView = findViewById(R.id.imageViewAppLogo)
        appNameTextView = findViewById(R.id.textViewSelectedApp)
        selectedAppName = intent.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)

        setupChooseAppButton()
        setupMapView(selectedAppName)
    }

    private fun updateChooseAppDisplay(appName: String?) {
        appNameTextView.text = appName ?: "All Apps"
        appIconView.visibility = if (appName == null) View.GONE else View.VISIBLE
        if (appName != null) {
            appIconView.setImageDrawable(applicationContext.getAppIconByName(appName))
        }
    }


    private fun setupMapView(selectedAppName: String?) {
        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            val geoJsonSource = geoJsonSource(APP_USAGE_SOURCE_ID) {
                featureCollection(loadGeoJsonFromAssets(selectedAppName, daysFilter))
                cluster(true)
                clusterMaxZoom(14)
                clusterRadius(50)
            }
            style.addSource(geoJsonSource)
            addMapLayers(style)
            centerMapOnLocation()
            updateMapText()
        }
    }

    private fun addMapLayers(style: Style) {
        style.addLayer(circleLayer(CLUSTERS_LAYER_ID, APP_USAGE_SOURCE_ID) {
            circleColor("#2c4b6e")
            circleRadius(15.0)
        })

        style.addLayer(symbolLayer(CLUSTER_COUNT_LAYER_ID, APP_USAGE_SOURCE_ID) {
            textField("{point_count_abbreviated}")
            textSize(12.0)
            textColor("#ffffff")
            textIgnorePlacement(true)
            textAllowOverlap(true)
            textAnchor(TextAnchor.CENTER)
        })
    }

    private fun loadGeoJsonFromAssets(selectedAppName: String?, days: Int): FeatureCollection {
        return try {
            val assetsJson = assets.open("dummy_location_data.geojson").bufferedReader().use { it.readText() }
            val originalFeatureCollection = FeatureCollection.fromJson(assetsJson)

            // Define a formatter matching your timestamp format
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            // Calculate the cutoff LocalDateTime
            val cutoffDateTime = LocalDateTime.now().minusDays(days.toLong())

            val filteredFeatures = originalFeatureCollection.features()?.filter { feature ->
                val timestampStr = feature.getStringProperty("timestamp")
                val featureDateTime = LocalDateTime.parse(timestampStr, formatter)

                // Check if the feature's app name matches (if specified) and the date is within the desired range
                (selectedAppName == null || feature.getStringProperty("appName") == selectedAppName) &&
                        featureDateTime.isAfter(cutoffDateTime)
            }

            FeatureCollection.fromFeatures(filteredFeatures ?: emptyList())
        } catch (e: Exception) {
            // Handle error if file reading fails
            FeatureCollection.fromFeatures(emptyList())
        }
    }

    private fun centerMapOnLocation() {
        mapView.mapboxMap.setCamera(cameraOptions {
            center(Point.fromLngLat(4.7012, 50.8789))
            zoom(12.0)
        })
    }

    private fun setupChooseAppButton() {
        updateChooseAppDisplay(selectedAppName)
        findViewById<View>(R.id.buttonChooseApp).setOnClickListener {
            chooseAppLauncher.launch(Intent(this, ChooseAppActivity::class.java))
        }

        val buttonShowTimeline = findViewById<CardView>(R.id.buttonShowTimeline)
        buttonShowTimeline.setOnClickListener {
            val intent = Intent(this, LocTimelineActivity::class.java)
            intent.putExtra(ChooseAppActivity.SELECTED_APP_NAME, selectedAppName)
            startActivity(intent)
        }
    }

    override fun filterData(days: Int) {
        daysFilter = days
        setupMapView(selectedAppName)
    }

    private fun updateMapText() {
        val mapText = if (daysFilter > 1) {
            getString(R.string.dashboard_text, daysFilter)
        } else {
            getString(R.string.dashboard_text_single_day)
        }

        findViewById<TextView>(R.id.textViewMap).text = mapText
    }



    companion object {
        private const val APP_USAGE_SOURCE_ID = "app-usage-source"
        private const val CLUSTERS_LAYER_ID = "clusters"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count"
    }
}
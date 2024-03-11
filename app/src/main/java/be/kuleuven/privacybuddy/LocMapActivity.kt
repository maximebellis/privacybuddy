package be.kuleuven.privacybuddy

import android.Manifest.permission.QUERY_ALL_PACKAGES
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource

class LocMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctp_map_location)
        setupUI()
    }

    private fun setupUI() {
        // Check if a specific app name was provided, otherwise handle "All apps" case
        val selectedAppName = intent.getStringExtra(SELECTED_APP_NAME)
        if (selectedAppName == null) {
            // Handle "All apps" case
            handleAllAppsSelection()
        } else {
            // Handle specific app selection
            setAppIconAndName(selectedAppName)
            setupMapView(selectedAppName)
        }
        setupLocationButton()
    }

    private fun handleAllAppsSelection() {
        findViewById<TextView>(R.id.textViewSelectedApp).text = "All apps"
        findViewById<ImageView>(R.id.imageViewAppLogo).visibility = View.GONE // Hide the app icon for "All apps"
        setupMapViewForAllApps()
    }
    private fun setAppIconAndName(appName: String) {
        val appIcon = applicationContext.getAppIconByName(appName)
        findViewById<TextView>(R.id.textViewSelectedApp).text = appName
        findViewById<ImageView>(R.id.imageViewAppLogo).setImageDrawable(appIcon)
    }

    private fun setupMapView(selectedAppName: String) {
        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            val geoJsonSource = geoJsonSource(APP_USAGE_SOURCE_ID) {
                featureCollection(loadGeoJsonFromAssets(selectedAppName))
                cluster(true)
                clusterMaxZoom(14)
                clusterRadius(50)
            }
            style.addSource(geoJsonSource)
            addMapLayers(style)
            centerMapOnLocation()
        }
    }

    private fun setupMapViewForAllApps() {
        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            // Here, adjust the logic to load and display GeoJSON data for all apps
            // This could mean loading a broader dataset or altering your data filtering approach
            val geoJsonSource = geoJsonSource(APP_USAGE_SOURCE_ID) {
                featureCollection(loadGeoJsonFromAssets(null)) // Adjust this method to handle null
                cluster(true)
                clusterMaxZoom(14)
                clusterRadius(50)
            }
            style.addSource(geoJsonSource)
            addMapLayers(style)
            centerMapOnLocation()
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

    private fun loadGeoJsonFromAssets(selectedAppName: String?): FeatureCollection =
        assets.open("dummy_location_data.geojson").bufferedReader().use { reader ->
            val originalFeatureCollection = FeatureCollection.fromJson(reader.readText())
            if (selectedAppName != null) {
                // Filter for a specific app
                originalFeatureCollection.features()?.filter {
                    it.getStringProperty("appName") == selectedAppName
                }?.let { FeatureCollection.fromFeatures(it) }
            } else {
                // Return all features for "All apps"
                originalFeatureCollection
            } ?: FeatureCollection.fromFeatures(emptyList())
        }

    private fun centerMapOnLocation() {
        mapView.mapboxMap.setCamera(cameraOptions {
            center(Point.fromLngLat(4.7012, 50.8789))
            zoom(12.0)
        })
    }

    private fun setupLocationButton() {
        findViewById<View>(R.id.buttonChooseApp).setOnClickListener {
            startActivity(Intent(this, ChooseAppActivity::class.java))
        }
    }


    companion object {
        private const val APP_USAGE_SOURCE_ID = "app-usage-source"
        private const val CLUSTERS_LAYER_ID = "clusters"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count"
        const val SELECTED_APP_NAME = "SELECTED_APP_NAME"
    }
}

package be.kuleuven.privacybuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource

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

    private fun loadGeoJsonFromAssets(selectedAppName: String?): FeatureCollection {
        return try {
            val originalFeatureCollection = FeatureCollection.fromJson(assets.open("dummy_location_data.geojson").bufferedReader().readText())
            if (selectedAppName != null) {
                originalFeatureCollection.features()?.filter { it.getStringProperty("appName") == selectedAppName }
                    ?.let { FeatureCollection.fromFeatures(it) } ?: FeatureCollection.fromFeatures(emptyList())
            } else {
                originalFeatureCollection
            }
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
    }


    companion object {
        private const val APP_USAGE_SOURCE_ID = "app-usage-source"
        private const val CLUSTERS_LAYER_ID = "clusters"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count"
    }
}
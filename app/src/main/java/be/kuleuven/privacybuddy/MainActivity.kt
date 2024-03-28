package be.kuleuven.privacybuddy


import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.LocationEventAdapter
import be.kuleuven.privacybuddy.extension.getAppIconByName
import be.kuleuven.privacybuddy.utils.AppOpsUtility
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import be.kuleuven.privacybuddy.utils.LocationDataUtils.loadGeoJsonFromAssets
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
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.gestures
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : BaseActivity() {

    private lateinit var locationEventAdapter: LocationEventAdapter
    private lateinit var mapView: MapView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_dashboard_main)
        setupToolbar()
        setupLocationEventsRecyclerView()
        initUI()
        AppOpsUtility.setupLocationAccessListener(this)
    }

    private fun initUI() {
        setupToolbarWithScrollListener(R.id.nestedScrollView, R.id.dashboardTitleTextView, getString(R.string.dashboard_title))
        setupWidgetClickListeners()
        setAppIcons()
        setupMapView(null)
        updateDashboardText()
    }

    private fun updateWidgetEvents() {
        loadGeoJsonFromAssets(null, applicationContext, days = daysFilter).let {
            val lastThreeItems = LocationDataUtils.getFirstThreeTimelineItems(it)
            locationEventAdapter.submitList(lastThreeItems)
        }
    }


    private fun setupLocationEventsRecyclerView() {
        findViewById<RecyclerView>(R.id.latestEventsRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = LocationEventAdapter(this@MainActivity).also {
                locationEventAdapter = it
            }
        }
        updateWidgetEvents()
    }

    private fun setupMapView(selectedAppName: String?) {
        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            val geoJsonSource = geoJsonSource(LocMapActivity.APP_USAGE_SOURCE_ID) {
                featureCollection(loadGeoJsonFromAssets(selectedAppName, daysFilter))
                cluster(true)
                clusterMaxZoom(14)
                clusterRadius(50)
            }
            style.addSource(geoJsonSource)
            addMapLayers(style)
            centerMapOnLocation()
        }
        mapView.gestures.pitchEnabled = false
        mapView.gestures.scrollEnabled = false
        mapView.gestures.rotateEnabled = false
        mapView.gestures.doubleTapToZoomInEnabled = false
        mapView.gestures.doubleTouchToZoomOutEnabled = false
        mapView.gestures.quickZoomEnabled = false
        mapView.gestures.pinchToZoomEnabled = false
        mapView.gestures.simultaneousRotateAndPinchToZoomEnabled = false
        mapView.gestures.pinchScrollEnabled = false
        mapView.gestures.scrollDecelerationEnabled = false
        mapView.gestures.rotateDecelerationEnabled  = false
        mapView.gestures.increasePinchToZoomThresholdWhenRotating = false
        mapView.gestures.pinchToZoomDecelerationEnabled = false





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
            val assetsJson = assets.open(AppState.selectedGeoJsonFile).bufferedReader().use { it.readText() }
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

    companion object {
        const val APP_USAGE_SOURCE_ID = "app-usage-source"
        private const val CLUSTERS_LAYER_ID = "clusters"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count"
    }


    private fun setupWidgetClickListeners() {
        setClickListener(R.id.widgetMapLocation, LocMapActivity::class.java)
        setClickListener(R.id.widgetLocation, LocTimelineActivity::class.java)
        setClickListener(R.id.widgetLocationTimeline, LocTimelineActivity::class.java)
    }

    private fun <T> setClickListener(viewId: Int, activityClass: Class<T>) where T : BaseActivity {
        findViewById<CardView>(viewId).setOnClickListener {
            startActivity(Intent(this, activityClass))
        }
    }

    private fun setAppIcons() {
        mapOf(
            R.id.imageViewTikTok to "TikTok",
            R.id.imageViewGmail to "Gmail",
            R.id.imageViewAppLogo to "YouTube"
        ).forEach { (viewId, appName) ->
            findViewById<ImageView>(viewId).setImageDrawable(getAppIconByName(appName))
        }
    }

    override fun filterData(days: Int) {
        daysFilter = days
        initUI()
    }

    private fun updateDashboardText() {
        val dashboardText = if (daysFilter > 1) {
            getString(R.string.dashboard_text, daysFilter)
        } else {
            getString(R.string.dashboard_text_single_day)
        }

        findViewById<TextView>(R.id.pageSubTitleTextView).text = dashboardText
    }
}

package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
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
import com.mapbox.maps.plugin.gestures.gestures
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


abstract class BaseActivity : AppCompatActivity() {

    object AppSettings {
        var daysFilter: Int = 21
    }


    abstract fun filterData(days: Int)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background_prim)

    }

    protected fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background_prim)
    }

    protected fun setupToolbarWithNestedScrollListener(scrollableViewId: Int, titleViewId: Int, toolbarTitle: String) {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        val titleView: TextView = findViewById(titleViewId)
        val nestedScrollView: NestedScrollView = findViewById(scrollableViewId)

        nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            toolbar.title = if (scrollY >= titleView.bottom) toolbarTitle else ""
        }
    }

    protected fun centerMapOnLocation(mapView: MapView) {
        mapView.mapboxMap.setCamera(cameraOptions {
            center(Point.fromLngLat(4.7012, 50.8789))
            zoom(12.0)
        })
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

    protected fun setupMapView(mapView: MapView, selectedAppName: String?) {
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            val geoJsonSource = geoJsonSource(APP_USAGE_SOURCE_ID) {
                featureCollection(loadGeoJsonFromAssets(selectedAppName, AppSettings.daysFilter))
                cluster(true)
                clusterMaxZoom(14)
                clusterRadius(50)
            }
            style.addSource(geoJsonSource)
            addMapLayers(style)
            centerMapOnLocation(mapView)
        }
    }

    protected fun disableMapGestures(mapView: MapView) {
        mapView.gestures.apply {
            listOf(
                ::pitchEnabled, ::scrollEnabled, ::rotateEnabled, ::doubleTapToZoomInEnabled,
                ::doubleTouchToZoomOutEnabled, ::quickZoomEnabled, ::pinchToZoomEnabled,
                ::simultaneousRotateAndPinchToZoomEnabled, ::pinchScrollEnabled,
                ::scrollDecelerationEnabled, ::rotateDecelerationEnabled,
                ::increasePinchToZoomThresholdWhenRotating, ::pinchToZoomDecelerationEnabled
            ).forEach { it.set(false) }
        }
    }

    private fun loadGeoJsonFromAssets(selectedAppName: String?, days: Int): FeatureCollection {
        return try {
            val originalFeatureCollection = FeatureCollection.fromJson(assets.open(AppState.selectedGeoJsonFile).bufferedReader().use { it.readText() })

            val cutoffDateTime = LocalDateTime.now().minusDays(days.toLong())
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            val filteredFeatures = originalFeatureCollection.features()?.mapNotNull { feature ->
                val featureDateTime = LocalDateTime.parse(feature.getStringProperty("timestamp"), formatter)

                if ((selectedAppName == null || feature.getStringProperty("appName") == selectedAppName) &&
                    featureDateTime.isAfter(cutoffDateTime)) {
                    feature.addNumberProperty("point_count_abbreviated", 1)
                    feature
                } else null
            }

            FeatureCollection.fromFeatures(filteredFeatures ?: emptyList())
        } catch (e: Exception) {
            FeatureCollection.fromFeatures(emptyList())
        }
    }

    companion object {
        const val APP_USAGE_SOURCE_ID = "app-usage-source"
        private const val CLUSTERS_LAYER_ID = "clusters"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count"
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menuInflater.inflate(R.menu.timespan_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val days = when (item.itemId) {
            R.id.action_one_day -> 1
            R.id.action_seven_days -> 7
            R.id.action_twenty_one_days -> 21
            else -> null
        }

        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_refresh -> {
                startActivity(intent)
                true
            }
            R.id.action_one_day, R.id.action_seven_days, R.id.action_twenty_one_days -> {
                days?.let { filterData(it) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}


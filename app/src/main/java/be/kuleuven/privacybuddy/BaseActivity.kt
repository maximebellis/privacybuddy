package be.kuleuven.privacybuddy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import be.kuleuven.privacybuddy.AppState.globalData
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.SpinnerAdapter
import be.kuleuven.privacybuddy.data.LocationData
import be.kuleuven.privacybuddy.data.SpinnerItem
import be.kuleuven.privacybuddy.extension.getAppIconByName
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import com.google.android.material.appbar.MaterialToolbar
import com.mapbox.geojson.Feature
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
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


abstract class BaseActivity : AppCompatActivity() {

    // Constants
    companion object {
        const val APP_USAGE_SOURCE_ID = "app-usage-source"
        private const val CLUSTERS_LAYER_ID = "clusters"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count"
    }

    // Properties
    protected var selectedAppName: String? = null
    object AppSettings {
        var daysFilter: Int = 21
    }

    // Lifecycle methods
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

    protected fun setupMapView(mapView: MapView, selectedAppName: String?) {
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            val featureCollection = loadGeoJsonFromAssets(selectedAppName, daysFilter)
            val geoJsonSource = geoJsonSource(APP_USAGE_SOURCE_ID) {
                featureCollection(featureCollection)
                cluster(true)
                clusterMaxZoom(14)
                clusterRadius(50)
            }
            style.addSource(geoJsonSource)
            addMapLayers(style)

            val biggestCluster = findBiggestCluster(featureCollection)
            if (biggestCluster != null) {
                centerMapOnLocation(mapView, biggestCluster)
            }
        }
    }

    protected fun setupSpinner() {
        val spinner: Spinner = findViewById(R.id.spinnerChooseApp)
        spinner.dropDownVerticalOffset = 100

        val apps = getUniqueAppNamesFromGeoJson().sorted().toMutableList()
        apps.add(0, "All apps")

        val spinnerItems = apps.map { appName ->
            SpinnerItem(getAppIconByName(appName)!!, appName)
        }

        spinner.adapter = SpinnerAdapter(this, R.layout.spinner_item, spinnerItems)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as SpinnerItem
                selectedAppName = if (selectedItem.appName == "All apps") null else selectedItem.appName
                onSpinnerItemSelected()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    fun updateMapView(mapView: MapView, selectedAppName: String?) {
        CoroutineScope(Dispatchers.Default).launch {
            val newFeatureCollection = loadGeoJsonFromAssets(selectedAppName, daysFilter)
            withContext(Dispatchers.Main) {
                val style = mapView.mapboxMap.style
                val source = style?.getSourceAs<GeoJsonSource>(APP_USAGE_SOURCE_ID)
                source?.featureCollection(newFeatureCollection)

                val biggestCluster = findBiggestCluster(newFeatureCollection)
                if (biggestCluster != null) {
                    centerMapOnLocation(mapView, biggestCluster)
                }
            }
        }
    }
    protected open fun filterData(days: Int) {
        daysFilter = days
        CoroutineScope(Dispatchers.IO).launch {
            LocationDataUtils.cacheAllLocationData(this@BaseActivity, days = daysFilter)
            LocationDataUtils.buildAppAccessStatsFromGeoJson(this@BaseActivity)
        }
    }

    private fun loadGeoJsonFromAssets(selectedAppName: String?, days: Int): FeatureCollection {
        return try {
            val filteredData = filterGlobalData(selectedAppName)
            val features = filteredData.map { data ->
                // Create a point for the location data
                val point = data.longitude?.let { data.latitude?.let { it1 ->
                    Point.fromLngLat(it,
                        it1
                    )
                } }

                val feature = Feature.fromGeometry(point)

                feature.addStringProperty("timestamp", data.timestamp.toString())
                feature.addStringProperty("appName", data.appName)
                feature.addNumberProperty("point_count_abbreviated", 1)

                feature
            }

            FeatureCollection.fromFeatures(features)
        } catch (e: Exception) {
            FeatureCollection.fromFeatures(emptyList())
        }
    }

    fun filterGlobalData(appName: String? = null): List<LocationData> {
        return globalData.filter { data ->
            appName == null || data.appName == appName
        }
    }

    // Helper methods
    private fun centerMapOnLocation(mapView: MapView, location: Point) {
        mapView.mapboxMap.setCamera(cameraOptions {
            center(location)
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

    private fun findBiggestCluster(featureCollection: FeatureCollection): Point? {
        var biggestCluster: Point? = null
        var maxCount = 0

        featureCollection.features()?.forEach { feature ->
            val count = feature.getNumberProperty("point_count_abbreviated").toInt()
            if (count > maxCount) {
                maxCount = count
                biggestCluster = feature.geometry() as Point
            }
        }

        return biggestCluster
    }

    private fun getUniqueAppNamesFromGeoJson(): List<String> {
        val filteredData = filterGlobalData()

        return runCatching {
            filteredData.map { data ->
                data.appName
            }.distinct()
        }.getOrElse {
            Log.e("GeoJsonUtils", "Error getting unique app names from GeoJson", it)
            emptyList()
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

    // Event handlers
    protected open fun onSpinnerItemSelected() {
    }

    // Menu methods
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
            R.id.action_live_data -> {
                startActivity(Intent(this, LiveActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }






}


package be.kuleuven.privacybuddy


import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.gestures

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
        updateTopAccessedAppsWidget()
    }

    private fun initUI() {
        setupToolbarWithNestedScrollListener(R.id.nestedScrollView, R.id.dashboardTitleTextView, getString(R.string.dashboard_title))
        setupWidgetClickListeners()
        setupMapWidget(null)
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

    private fun setupMapWidget(selectedAppName: String?) {
        mapView = findViewById(R.id.mapView)
        setupMapView(mapView, selectedAppName)
        disableMapGestures(mapView)
        setupMapClickListeners()
    }

    private fun setupMapClickListeners() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                startActivity(Intent(this@MainActivity, LocMapActivity::class.java))
                mapView.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                return true
            }
        })

        mapView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }


    private fun setupWidgetClickListeners() {
        listOf(
            R.id.widgetMapLocation to LocMapActivity::class.java,
            //R.id.widgetLocation to LocTimelineActivity::class.java,
            R.id.widgetLocationTimeline to LocTimelineActivity::class.java
        ).forEach { (viewId, activityClass) ->
            findViewById<CardView>(viewId).setOnClickListener {
                startActivity(Intent(this, activityClass))
            }
        }
    }

    override fun filterData(days: Int) {
        daysFilter = days
        initUI()
    }

    private fun getAllAccessedAppsFromGeoJson(): List<AppAccessInfo> {
        val geoJsonString = assets.open(AppState.selectedGeoJsonFile).bufferedReader().use { it.readText() }
        val featureCollection = FeatureCollection.fromJson(geoJsonString)
        val accessCounts = featureCollection.features()?.groupingBy { it.getStringProperty("appName") }?.eachCount() ?: emptyMap()
        return accessCounts.entries.map { AppAccessInfo(it.key, it.value) }
    }

    private fun updateDashboardText() {
        val allApps = getAllAccessedAppsFromGeoJson()
        val distinctAppsCount = allApps.map { it.appName }.distinct().size

        val dashboardTextId = if (daysFilter > 1) R.string.dashboard_text else R.string.dashboard_text_single_day
        findViewById<TextView>(R.id.pageSubTitleTextView).text = getString(dashboardTextId, daysFilter)

        //findViewById<TextView>(R.id.textViewLocationUsage).text = "Used by $distinctAppsCount app${if (distinctAppsCount > 1) "s" else ""}"
    }

    private fun getTopAccessedAppsFromGeoJson(): List<AppAccessInfo> {
        val geoJsonString = assets.open(AppState.selectedGeoJsonFile).bufferedReader().use { it.readText() }
        val featureCollection = FeatureCollection.fromJson(geoJsonString)
        val accessCounts = featureCollection.features()?.groupingBy { it.getStringProperty("appName") }?.eachCount() ?: emptyMap()
        return accessCounts.entries.sortedByDescending { it.value }.take(3).map { AppAccessInfo(it.key, it.value) }
    }

    private fun updateTopAccessedAppsWidget() {
        val topApps = getTopAccessedAppsFromGeoJson()
        val maxAccessCount = topApps.maxOfOrNull { it.accessCount } ?: 1
        val widgetSlots = listOf(
            listOf(R.id.imageViewMostLocationAcessesApp1, R.id.textViewMostLocationAcessesDataApp1, R.id.progressBarMostLocationAcessesApp1, R.id.textViewMostLocationAccessesApp1),
            listOf(R.id.imageViewostLocationAccessesApp2, R.id.textViewostLocationAccessesDataApp2, R.id.progressBarMostLocationAccessesApp2, R.id.textViewMostLocationAccessesApp2),
            listOf(R.id.imageViewMostLocationAccessesApp3, R.id.textViewMostLocationAccessesDataApp3, R.id.progressBarMostLocationAccessesApp3, R.id.textViewMostLocationAccessesApp3)
        )

        widgetSlots.forEachIndexed { index, (imageViewId, textViewAccessId, progressBarId, textViewAppNameId) ->
            val imageView = findViewById<ImageView>(imageViewId)
            val textViewAccess = findViewById<TextView>(textViewAccessId)
            val progressBar = findViewById<ProgressBar>(progressBarId)
            val textViewAppName = findViewById<TextView>(textViewAppNameId)

            if (index < topApps.size) {
                val appAccessInfo = topApps[index]
                imageView.visibility = View.VISIBLE
                imageView.setImageDrawable(getAppIconByName(appAccessInfo.appName))
                textViewAccess.text = "${appAccessInfo.accessCount} accesses"
                progressBar.max = maxAccessCount
                progressBar.progress = appAccessInfo.accessCount
                textViewAppName.text = appAccessInfo.appName
            } else {
                imageView.visibility = View.INVISIBLE
                textViewAccess.text = "0 accesses"
                progressBar.progress = 0
                textViewAppName.text = ""
            }
        }
    }

    data class AppAccessInfo(val appName: String, val accessCount: Int)
}

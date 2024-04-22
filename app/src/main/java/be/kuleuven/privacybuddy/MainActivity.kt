package be.kuleuven.privacybuddy


import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
import be.kuleuven.privacybuddy.data.AppAccessStats

class MainActivity : BaseActivity() {

    private lateinit var locationEventAdapter: LocationEventAdapter
    private lateinit var mapView: MapView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_main_dashboard)
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
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        updateTopAccessedAppsWidget()
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
            adapter = LocationEventAdapter(this@MainActivity, true).also {
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
            R.id.widgetLocationTimeline to LocTimelineActivity::class.java,
            R.id.widgetTopApps to LocTopAppsActivity::class.java

        ).forEach { (viewId, activityClass) ->
            findViewById<CardView>(viewId).setOnClickListener {
                startActivity(Intent(this, activityClass))
            }
        }
    }

    override fun filterData(days: Int) {
        daysFilter = days
        LocationDataUtils.buildAppAccessStatsFromGeoJson(this)
        initUI()
    }


    private fun updateDashboardText() {
        val distinctAppsCount = AppState.topAccessedAppsCache?.map { it.appName }?.distinct()?.size ?: 0

        val dashboardTextId = if (daysFilter > 1) R.string.dashboard_text else R.string.dashboard_text_single_day
        findViewById<TextView>(R.id.pageSubTitleTextView).text = getString(dashboardTextId, daysFilter, distinctAppsCount)
    }


    private fun updateTopAccessedAppsWidget() {
        val container = findViewById<LinearLayout>(R.id.containerAppViews)
        val topApps = AppState.topAccessedAppsCache?.sortedByDescending { it.totalAccesses }?.take(3) ?: emptyList()
        container.removeAllViews()

        val maxAccessCount = topApps.maxOfOrNull { it.totalAccesses } ?: 1

        topApps.forEach { appStats ->
            val appView = LayoutInflater.from(this).inflate(R.layout.component_top_app, container, false)

            appView.findViewById<TextView>(R.id.textViewAppName).text = appStats.appName
            appView.findViewById<TextView>(R.id.textViewAppAccesses).text = "${appStats.totalAccesses} accesses"
            val progressBar = appView.findViewById<ProgressBar>(R.id.progressBarAppUsage)
            progressBar.max = maxAccessCount
            progressBar.progress = appStats.totalAccesses

            val appIcon = this.getAppIconByName(appStats.appName)
            appView.findViewById<ImageView>(R.id.imageViewAppIcon).setImageDrawable(appIcon)

            container.addView(appView)
        }
    }



}

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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class MainActivity : BaseActivity() {

    private lateinit var locationEventAdapter: LocationEventAdapter


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
        loadStaticMap()
        setupWidgetClickListeners()
        setAppIcons()
        updateDashboardText()
    }

    private fun updateWidgetEvents() {
        loadGeoJsonFromAssets(AppState.selectedGeoJsonFile, applicationContext, days = daysFilter).let {
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


    private fun loadStaticMap() {
        val imageView = findViewById<ImageView>(R.id.staticMapView)
        imageView.post {
            val mapboxAccessToken = getString(R.string.mapbox_access_token)
            val staticMapUrl = "https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/4.7012,50.8789,14/${imageView.width}x${imageView.height}?access_token=$mapboxAccessToken"
            Glide.with(this)
                .load(staticMapUrl)
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(imageView)
        }
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

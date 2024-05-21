package be.kuleuven.privacybuddy


import android.content.Intent
import android.database.Cursor
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.AppState.selectedInteractionTypes
import be.kuleuven.privacybuddy.AppState.selectedUsageTypes
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.LocationEventAdapter
import be.kuleuven.privacybuddy.extension.getAppIconByName
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import com.mapbox.maps.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private lateinit var locationEventAdapter: LocationEventAdapter
    private lateinit var mapView: MapView
    private var cacheDataJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppState.clearTopAccessedAppsCache()

        LocationDataUtils.buildAppAccessStatsFromGeoJson(this)
        LocationDataUtils.cacheAllLocationData(this)

        setContentView(R.layout.page_main_dashboard)
        setupToolbar()
        initUI()

        setupCardView(R.id.cardViewSubliminal, R.id.textViewSubliminal, "subliminal", false)
        setupCardView(R.id.cardViewForeground, R.id.textViewForeground, "foreground", false)
        setupCardView(R.id.cardViewBackground, R.id.textViewBackground, "background", false)
        setupCardView(R.id.cardViewApproximate, R.id.textViewApproximate, "approximate", true)
        setupCardView(R.id.cardViewPrecise, R.id.textViewPrecise, "precise", true)

        val uri: Uri = Uri.parse("content://be.kuleuven.locationusagescontentprovider/data")
        Log.d("PermissionLogDebug", "URI: $uri")
        try {
            Log.d("PermissionLogDebug", "Trying to access the provider")
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            Log.d("PermissionLogDebug", "Cursor: $cursor")
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val packageNameIndex = cursor.getColumnIndex("packageName")
                    val permissionGroupIndex = cursor.getColumnIndex("permissionGroup")
                    val accessTimeIndex = cursor.getColumnIndex("accessTime")

                    val packageName = if (packageNameIndex != -1) cursor.getString(packageNameIndex) else null
                    val permissionGroup = if (permissionGroupIndex != -1) cursor.getString(permissionGroupIndex) else null
                    val accessTime = if (accessTimeIndex != -1) cursor.getString(accessTimeIndex) else null
                    // Log the data
                    Log.d("PermissionLogDebug", "packageName: $packageName, permissionGroup: $permissionGroup, accessTime: $accessTime")
                } while (cursor.moveToNext())
                cursor.close()
            }
        } catch (e: Exception) {
            Log.d("PermissionLogDebug", "Exception: ${e.message}")
        }

    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    private fun initUI() {

        setupToolbarWithNestedScrollListener(R.id.nestedScrollView, R.id.dashboardTitleTextView, getString(R.string.dashboard_title))
        setupWidgetClickListeners()
        setupLocationEventsRecyclerView() // widget timeline
        setupMapWidget(null) // widget map
        updateTopAccessedAppsWidget() // widget top apps

        updateSubtitleText()
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        val textView = findViewById<TextView>(R.id.forgetMeTitleTextView)
        val content = textView.text.toString()
        val spannableString = SpannableString(content)
        spannableString.setSpan(UnderlineSpan(), 0, content.length, 0)
        textView.text = spannableString

    }
    private fun updateWidgetEvents() {
        filterGlobalData().let {
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
        // link dashboardInfoIcon to page_info.xml
        findViewById<ImageView>(R.id.dashboardInfoIcon).setOnClickListener {
            startActivity(Intent(this, LocInfoActivity::class.java))
        }
    }

    override fun filterData(days: Int) {
        super.filterData(days)
        initUI()
    }


    private fun updateSubtitleText() {
        val distinctAppsCount = AppState.topAccessedAppsCache?.map { it.appName }?.distinct()?.size ?: 0

        val dashboardTextId = if (daysFilter > 1) R.string.dashboard_text else R.string.dashboard_text_single_day
        findViewById<TextView>(R.id.pageSubTitleTextView).text = getString(dashboardTextId, daysFilter, distinctAppsCount)
    }

    private fun updateTopAccessedAppsWidget() {
        val container = findViewById<LinearLayout>(R.id.containerAppViews)
        container.removeAllViews()

        if (daysFilter == 1) {
            val messageView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(10, 10, 20, 20)
                }
                gravity = Gravity.START
                setText(R.string.privacy_scores_not_calculated)
                setTextColor(ContextCompat.getColor(context, R.color.text))
                textSize = 15f
            }
            container.addView(messageView)
        } else {
            val topApps = AppState.topAccessedAppsCache?.sortedBy { it.privacyScore }?.take(3) ?: emptyList()
            val maxScore = 100

            topApps.forEach { appStats ->
                val appView = LayoutInflater.from(this).inflate(R.layout.component_top_app, container, false)

                appView.findViewById<TextView>(R.id.textViewAppName).text = appStats.appName
                val formattedPrivacyScore = String.format("%.1f", appStats.privacyScore)
                appView.findViewById<TextView>(R.id.textViewAppAccesses).text = "Privacy Score: $formattedPrivacyScore"

                val progressBar = appView.findViewById<ProgressBar>(R.id.progressBarAppUsage)
                progressBar.max = maxScore
                progressBar.progress = appStats.privacyScore.toInt()
                progressBar.progressDrawable.setColorFilter(LocationDataUtils.getPrivacyScoreColor(appStats.privacyScore), PorterDuff.Mode.SRC_IN)

                val appIcon = getAppIconByName(appStats.appName)
                appView.findViewById<ImageView>(R.id.imageViewAppIcon).setImageDrawable(appIcon)

                container.addView(appView)
            }
        }
    }

    private fun setupCardView(cardViewId: Int, textViewId: Int, type: String, isUsageType: Boolean) {
        val cardView = findViewById<CardView>(cardViewId)
        val textView = cardView.findViewById<TextView>(textViewId)

        cardView.tag = Triple(type, isUsageType, false)

        cardView.setOnClickListener {
            val (type, isUsageType, isSelected) = it.tag as Triple<String, Boolean, Boolean>
            it.tag = Triple(type, isUsageType, !isSelected)
            updateCardViewState(it, isSelected, textView, type, isUsageType)
            cacheDataJob?.cancel()
            cacheDataJob = CoroutineScope(Dispatchers.IO).launch {
                LocationDataUtils.cacheAllLocationData(this@MainActivity)
                withContext(Dispatchers.Main) {
                    updateContent()
                }
            }
        }
    }

    private fun updateCardViewState(view: View, isSelected: Boolean, textView: TextView, type: String, isUsageType: Boolean) {
        val color = if (isSelected) R.color.background_sec else R.color.text
        val textColor = if (isSelected) R.color.text else R.color.background_sec
        val list = if (isUsageType) selectedUsageTypes else selectedInteractionTypes

        (view as CardView).setCardBackgroundColor(ContextCompat.getColor(this, color))
        textView.setTextColor(ContextCompat.getColor(this, textColor))
        textView.setTypeface(null, if (isSelected) Typeface.NORMAL else Typeface.BOLD)

        if (isSelected) list.remove(type) else list.add(type)
    }

    private fun updateContent() {
        setupLocationEventsRecyclerView()
        setupMapWidget(null)
        updateTopAccessedAppsWidget()
    }

}

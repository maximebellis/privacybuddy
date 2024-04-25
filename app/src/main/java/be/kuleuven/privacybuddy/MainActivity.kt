package be.kuleuven.privacybuddy


import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
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
import be.kuleuven.privacybuddy.utils.AppOpsUtility
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import com.mapbox.maps.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private lateinit var locationEventAdapter: LocationEventAdapter
    private lateinit var mapView: MapView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_main_dashboard)
        setupToolbar()
        initUI()
        AppOpsUtility.setupLocationAccessListener(this)

        setupCardView(R.id.cardViewSubliminal, R.id.textViewSubliminal, "subliminal", false)
        setupCardView(R.id.cardViewForeground, R.id.textViewForeground, "foreground", false)
        setupCardView(R.id.cardViewBackground, R.id.textViewBackground, "background", false)
        setupCardView(R.id.cardViewApproximate, R.id.textViewApproximate, "approximate", true)
        setupCardView(R.id.cardViewPrecise, R.id.textViewPrecise, "precise", true)
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
                progressBar.progressDrawable.setColorFilter(getPrivacyScoreColor(appStats.privacyScore), PorterDuff.Mode.SRC_IN)

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
            CoroutineScope(Dispatchers.IO).launch {
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

        // Set text to bold if selected, normal otherwise
        textView.setTypeface(null, if (isSelected) Typeface.NORMAL else Typeface.BOLD)

        if (isSelected) list.remove(type) else list.add(type)
    }

    private fun updateContent() {
        setupLocationEventsRecyclerView()
        setupMapWidget(null)
        updateTopAccessedAppsWidget()
    }


    private fun getPrivacyScoreColor(score: Double): Int {
        val colorStops = arrayOf(
            Pair(100.0, Color.parseColor("#4CAF50")), // Vibrant Green
            Pair(80.0, Color.parseColor("#8BC34A")), // Light Green
            Pair(60.0, Color.parseColor("#CDDC39")), // Lime
            Pair(40.0, Color.parseColor("#FFEB3B")), // Bright Yellow
            Pair(20.0, Color.parseColor("#FFC107")), // Amber
            Pair(0.0, Color.parseColor("#F44336"))   // Bright Red
        )

        // Ensure there's a color stop that covers all possible scores
        if (score > 100) return colorStops.first().second
        if (score < 0) return colorStops.last().second

        val lowerStop = colorStops.lastOrNull { it.first <= score }
        val upperStop = colorStops.firstOrNull { it.first >= score }

        if (lowerStop == null || upperStop == null) {
            // Fallback to red if something unexpected happens
            return Color.parseColor("#F44336")
        }

        val ratio = (score - lowerStop.first) / (upperStop.first - lowerStop.first)
        return interpolateColor(lowerStop.second, upperStop.second, ratio.toFloat())
    }


    private fun interpolateColor(colorStart: Int, colorEnd: Int, fraction: Float): Int {
        val startA = Color.alpha(colorStart)
        val startR = Color.red(colorStart)
        val startG = Color.green(colorStart)
        val startB = Color.blue(colorStart)

        val endA = Color.alpha(colorEnd)
        val endR = Color.red(colorEnd)
        val endG = Color.green(colorEnd)
        val endB = Color.blue(colorEnd)

        val newA = interpolate(startA, endA, fraction)
        val newR = interpolate(startR, endR, fraction)
        val newG = interpolate(startG, endG, fraction)
        val newB = interpolate(startB, endB, fraction)

        return Color.argb(newA, newR, newG, newB)
    }

    private fun interpolate(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).toInt()
    }






}

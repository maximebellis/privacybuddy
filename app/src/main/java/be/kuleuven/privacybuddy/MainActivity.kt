package be.kuleuven.privacybuddy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import be.kuleuven.privacybuddy.extension.getAppIconByName
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.MaterialToolbar
import com.mapbox.geojson.FeatureCollection
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_main)
        setupToolbar()

        setupToolbarWithScrollListener(R.id.nestedScrollView, R.id.dashboardTitleTextView, getString(R.string.dashboard_title))

        loadStaticMap()
        setupClickListener()
        setAppIcons()
        addTimelineAccessViews()
    }
    private fun addTimelineAccessViews() {
        val events = loadGeoJsonFromAssets(null).takeLast(3) // Get the last 3 entries
        val timelineContainer = findViewById<LinearLayout>(R.id.widgetLocationTimelineWithin)

        events.forEach { event ->
            val accessView = layoutInflater.inflate(R.layout.widget_location_timeline_unit, timelineContainer, false)
            // Update the text views within accessView with data from event
            accessView.findViewById<TextView>(R.id.textViewTime).text = formatTimestamp(event.timestamp)
            accessView.findViewById<TextView>(R.id.textViewAppName).text = event.appName
            accessView.findViewById<TextView>(R.id.textViewAccuracy).text = event.usageType

            // Set the app icon
            val appIconView = accessView.findViewById<ImageView>(R.id.imageViewAppLogo)
            val iconDrawable = applicationContext.getAppIconByName(event.appName)
            appIconView.setImageDrawable(iconDrawable)

            timelineContainer.addView(accessView)
        }
    }

    private fun formatTimestamp(timestamp: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return outputFormat.format(inputFormat.parse(timestamp)!!)
    }

    private fun loadStaticMap() {
        val imageView = findViewById<ImageView>(R.id.staticMapView)
        imageView.post {
            val width = imageView.width // Get the width of the ImageView
            val height = imageView.height // Get the height of the ImageView

            // Validate dimensions to avoid Glide load failures
            if (width > 0 && height > 0) {
                val mapboxAccessToken = getString(R.string.mapbox_access_token)
                val staticMapUrl = "https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/4.7012,50.8789,14/${width}x${height}?access_token=$mapboxAccessToken"

                Glide.with(this)
                    .load(staticMapUrl)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)) // Utilize disk caching
                    .into(imageView)
            }
        }
    }

    private fun setupClickListener() {
        val widgetMapLocation = findViewById<CardView>(R.id.widgetMapLocation)
        widgetMapLocation.setOnClickListener {
            val intent = Intent(this, LocMapActivity::class.java)
            startActivity(intent)
        }

        val widgetLocation = findViewById<CardView>(R.id.widgetLocation)
        widgetLocation.setOnClickListener {
            val intent = Intent(this, LocTimelineActivity::class.java)
            startActivity(intent)
        }

        val widgetLocationTimeline = findViewById<LinearLayout>(R.id.widgetLocationTimelineWithin)
        widgetLocationTimeline.setOnClickListener {
            // Replace CtpTimelineLocationActivity with the actual Activity class you want to launch
            val intent = Intent(this, LocTimelineActivity::class.java)
            startActivity(intent)
        }
    }



    private fun setAppIcons() {
        mapOf(
            R.id.imageViewTikTok to "TikTok",
            R.id.imageViewGmail to "Gmail",
            R.id.imageViewAppLogo to "YouTube"
        ).forEach { (viewId, appName) ->
            val iconDrawable = getAppIconByName(appName)
            findViewById<ImageView>(viewId).setImageDrawable(iconDrawable)
        }
    }

    //Copied code from the timelineactivity
    private fun loadGeoJsonFromAssets(selectedAppName: String?): List<LocationEvent> {
        return try {
            val featureCollection = parseGeoJsonFromAssets("dummy_location_data.geojson")
            featureCollection.features()?.mapNotNull { feature ->
                LocationEvent(
                    feature.getStringProperty("timestamp"),
                    feature.getStringProperty("appName"),
                    feature.getStringProperty("usageType"),
                    feature.getStringProperty("interactionType")
                )
            }?.filter { selectedAppName == null || it.appName == selectedAppName } ?: emptyList()
        } catch (e: Exception) {
            Log.e("LocTimelineActivity", "Error loading or parsing data", e)
            emptyList()
        }
    }

    private fun parseGeoJsonFromAssets(filename: String): FeatureCollection =
        assets.open(filename).use {
            FeatureCollection.fromJson(it.bufferedReader().use(BufferedReader::readText))
        }

    //copy stops here
}

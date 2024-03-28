package be.kuleuven.privacybuddy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.gson.Gson
import be.kuleuven.privacybuddy.data.LocationData
import com.mapbox.maps.MapView
import java.util.Locale
import android.content.Intent
import android.widget.ImageView
import android.widget.ProgressBar
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

class LocSingleAccessActivity : BaseActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_specific_access)
        setupToolbar()

        val appNameTextView: TextView = findViewById(R.id.dataEntryApp)
        val timestampTextView: TextView = findViewById(R.id.dataEntryTime)
        val appLogoImageView: ImageView = findViewById(R.id.appLogoImageView)

        // Retrieve the JSON string from the intent
        val eventJsonString = intent.getStringExtra("jsonData") ?: return
        Log.d("LocSingleAccessActivity", "Received JSON: $eventJsonString")

        // Parse the JSON string into a LocationData object
        val locationData = Gson().fromJson(eventJsonString, LocationData::class.java)

        // Populate the UI with the data
        locationData?.let {
            appNameTextView.text = it.appName
            val appIconDrawable = getAppIconByName(it.appName)
            appLogoImageView.setImageDrawable(appIconDrawable)
            timestampTextView.text = it.timestamp

            it.accuracy?.let { accuracy ->
                setDataEntry(R.id.dataEntryAccuracy, "Accuracy:", formatNumber(accuracy))
            } ?: run {
                hideDataEntry(R.id.dataEntryAccuracy)
            }

            it.speed?.let { speed ->
                setDataEntry(R.id.dataEntrySpeed, "Speed:", formatNumber(speed))
            } ?: run {
                hideDataEntry(R.id.dataEntrySpeed)
            }

            it.bearing?.let { bearing ->
                setDataEntry(R.id.dataEntryBearing, "Bearing:", formatNumber(bearing))
            } ?: run {
                hideDataEntry(R.id.dataEntryBearing)
            }

            // Latitude and Longitude are always expected to be present but check for nullability to be safe
            setDataEntry(R.id.dataEntryLatitude, "Latitude:", formatNumber(it.latitude ?: 0.0))
            setDataEntry(R.id.dataEntryLongitude, "Longitude:", formatNumber(it.longitude ?: 0.0))

            it.altitude?.let { altitude ->
                setDataEntry(R.id.dataEntryAltitude, "Altitude:", formatNumber(altitude))
            } ?: run {
                hideDataEntry(R.id.dataEntryAltitude)
            }

        }

        mapView = findViewById(R.id.mapView)
        initializeMap(locationData.latitude ?: 0.0, locationData.longitude ?: 0.0)


    }

    private fun initializeMap(latitude: Double, longitude: Double) {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            mapView.getMapboxMap().setCamera(
                cameraOptions {
                    center(Point.fromLngLat(longitude, latitude))
                    zoom(14.0)
                }
            )
        }

        mapView.gestures.apply {
            pitchEnabled = false
            rotateEnabled = false
            scrollEnabled = false
        }
    }




    fun formatNumber(value: Double): String {
        return "%.7g".format(Locale.US, value)
    }

    private fun setDataEntry(viewId: Int, dataName: String, dataValue: String) {
        val dataEntryView = findViewById<View>(viewId)
        val nameTextView = dataEntryView.findViewById<TextView>(R.id.textViewDataName)
        val valueTextView = dataEntryView.findViewById<TextView>(R.id.textViewDataValue)
        nameTextView.text = dataName
        valueTextView.text = dataValue
    }

    private fun hideDataEntry(viewId: Int) {
        val dataEntryView = findViewById<View>(viewId)
        dataEntryView.visibility = View.GONE
    }

    override fun filterData(days: Int) {
        AppSettings.daysFilter = days
        //this page does not need to reload anything for this
    }

}

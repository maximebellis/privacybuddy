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
import android.content.res.Resources
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
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
        locationData.accuracy?.let { setupInfoButtons(it) }

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

    private fun setupInfoButtons(accuracy: Double) {
        val infoButtonLatitude = findViewById<View>(R.id.dataEntryLatitude).findViewById<ImageView>(R.id.imageViewInfo)
        val infoButtonLongitude = findViewById<View>(R.id.dataEntryLongitude).findViewById<ImageView>(R.id.imageViewInfo)
        val infoButtonAccuracy = findViewById<View>(R.id.dataEntryAccuracy).findViewById<ImageView>(R.id.imageViewInfo)
        val infoButtonSpeed = findViewById<View>(R.id.dataEntrySpeed).findViewById<ImageView>(R.id.imageViewInfo)
        val infoButtonBearing = findViewById<View>(R.id.dataEntryBearing).findViewById<ImageView>(R.id.imageViewInfo)
        val infoButtonAltitude = findViewById<View>(R.id.dataEntryAltitude).findViewById<ImageView>(R.id.imageViewInfo)

        infoButtonLatitude.setOnClickListener { view ->
            showInfoPopup(view, getString(R.string.info_latitude))
        }
        infoButtonLongitude.setOnClickListener { view ->
            showInfoPopup(view, getString(R.string.info_longitude))
        }
        val accuracyDescription = getAccuracyDescription(accuracy)
        infoButtonAccuracy.setOnClickListener { showInfoPopup(it, getString(R.string.info_accuracy, accuracy.toInt(), accuracyDescription)) }
        infoButtonSpeed.setOnClickListener { view ->
            showInfoPopup(view, getString(R.string.info_speed))
        }
        infoButtonBearing.setOnClickListener { view ->
            showInfoPopup(view, getString(R.string.info_bearing))
        }
        infoButtonAltitude.setOnClickListener { view ->
            showInfoPopup(view, getString(R.string.info_altitude))
        }
    }

    private fun getAccuracyDescription(accuracy: Double): String {
        return when {
            accuracy <= 5 -> "a very precise location, such as which part of a room you are in."
            accuracy <= 10 -> "a precise location, such as the specific area of a small building."
            accuracy <= 50 -> "a general area, such as the building you are in."
            else -> "a broad area, making it difficult to determine your exact location."
        }
    }


    private fun showInfoPopup(anchor: View, text: String) {
        val inflater = LayoutInflater.from(anchor.context)
        val popupView = inflater.inflate(R.layout.popup_info, null)
        val textViewPopupContent: TextView = popupView.findViewById(R.id.textViewPopupContent)
        textViewPopupContent.text = text

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Measure content view
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        // Determine anchor location on screen
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorLeft = location[0]
        val anchorTop = location[1]

        // Get screen size
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Calculate x and y position of the popup
        val xOff = if (anchorLeft + popupWidth > screenWidth) screenWidth - popupWidth else anchorLeft
        val yOff = if (anchorTop + popupHeight > screenHeight) screenHeight - popupHeight else anchorTop

        // Show the popup
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xOff, yOff)
    }



}

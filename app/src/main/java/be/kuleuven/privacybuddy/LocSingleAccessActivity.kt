package be.kuleuven.privacybuddy

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.data.LocationData
import be.kuleuven.privacybuddy.extension.getAppIconByName
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import java.util.Locale
import kotlin.math.*

class LocSingleAccessActivity : BaseActivity() {

    private lateinit var mapView: MapView

    // Define home coordinates
    private val homeLatitude = 50.85800
    private val homeLongitude = 4.78472
    private val homeAddressText = "Larestraat 3 Bierbeek"

    // Define work coordinates
    private val workLatitude = 50.87333
    private val workLongitude = 4.71381
    private val workAddressText = "Geldenaaksevest 2, Leuven"

    private val addressNotFoundText = "Address not found"
    private val maxDistance = 0.2 // Maximum distance in kilometers (200 meters)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_specific_access)
        setupToolbar()
        setupToolbarWithNestedScrollListener(R.id.nestedScrollView, R.id.locationAccessByTextView, getString(R.string.location_access_by))

        val locationData: LocationData? = intent.extras?.getParcelable("locationData")
        locationData?.let {
            setupDataEntries(it)
            setupInfoButtonEntries(it)
        }

        mapView = findViewById(R.id.mapView)
        initializeMap(locationData?.latitude ?: 0.0, locationData?.longitude ?: 0.0)

        val textViewAddress = findViewById<TextView>(R.id.textViewAdress)
        textViewAddress.text = when {
            locationData != null && locationData.latitude?.let { locationData.longitude?.let { it1 ->
                isWithinRange(it,
                    it1, homeLatitude, homeLongitude)
            } } == true -> homeAddressText
            locationData != null && locationData.latitude?.let { locationData.longitude?.let { it1 ->
                isWithinRange(it,
                    it1, workLatitude, workLongitude)
            } } == true -> workAddressText
            else -> addressNotFoundText
        }
    }

    private fun isWithinRange(latitude: Double, longitude: Double, targetLatitude: Double, targetLongitude: Double): Boolean {
        val earthRadius = 6371.0 // Radius of the earth in kilometers

        val dLat = Math.toRadians(latitude - targetLatitude)
        val dLon = Math.toRadians(longitude - targetLongitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(targetLatitude)) * cos(Math.toRadians(latitude)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = earthRadius * c

        return distance <= maxDistance
    }

    private fun setupDataEntries(data: LocationData) {
        findViewById<TextView>(R.id.dataEntryApp).text = data.appName
        findViewById<ImageView>(R.id.appLogoImageView).setImageDrawable(getAppIconByName(data.appName))
        findViewById<TextView>(R.id.dataEntryTime).text = data.timestamp

        setDataEntry(R.id.dataEntryLatitude, "Latitude:", formatNumberWithUnit(data.latitude, "°"))
        setDataEntry(R.id.dataEntryLongitude, "Longitude:", formatNumberWithUnit(data.longitude, "°"))
        setDataEntry(R.id.dataEntryAltitude, "Altitude:", formatNumberWithUnit(data.altitude, "m"))
        setDataEntry(R.id.dataEntrySpeed, "Speed:", formatNumberWithUnit(data.speed, "m/s"))
    }

    private fun setupInfoButtonEntries(data: LocationData) {
        setupAccuracyEntry(data.accuracy)
        setupBearingEntry(data.bearing)
    }

    private fun setupAccuracyEntry(accuracy: Double?) {
        accuracy?.let {
            val formattedAccuracy = formatNumberWithUnit(it, "m")
            val accuracyTextView = findViewById<TextView>(R.id.textViewDataValueAccuracy)
            accuracyTextView.text = "$formattedAccuracy"
            setupInfoButton(R.id.imageViewInfo, getString(R.string.info_accuracy, it.toInt(), getAccuracyDescription(it)))
        }
    }

    private fun setupBearingEntry(bearing: Double?) {
        bearing?.let {
            val formattedBearing = formatNumberWithUnit(it, "°")
            val BearingTextView = findViewById<TextView>(R.id.textViewDataValueBearing)
            BearingTextView.text = "$formattedBearing"
            setupInfoButton(R.id.imageViewInfoBearing, getString(R.string.info_bearing))
        }
    }

    private fun setupInfoButton(infoButtonId: Int, infoText: String) {
        val infoButton = findViewById<ImageView>(infoButtonId)
        infoButton.visibility = View.VISIBLE
        infoButton.setOnClickListener { showInfoPopup(it, infoText) }
    }

    private fun formatNumberWithUnit(value: Double?, unit: String): String {
        return if (value != null) String.format(Locale.US, "%.2f %s", value, unit) else "N/A"
    }

    private fun initializeMap(latitude: Double, longitude: Double) {
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            mapView.mapboxMap.setCamera(
                cameraOptions {
                    center(Point.fromLngLat(longitude, latitude))
                    zoom(14.0)
                }
            )

            val resizedBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_red_marker)
                .let { originalBitmap ->
                    val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                    Bitmap.createScaledBitmap(originalBitmap, 75, Math.round(75 / aspectRatio), false)
                }

            style.addImage("red", resizedBitmap)

            mapView.annotations.createPointAnnotationManager().create(
                PointAnnotationOptions()
                    .withIconImage("red")
                    .withIconAnchor(IconAnchor.BOTTOM)
                    .withPoint(Point.fromLngLat(longitude, latitude))
            )
        }

        disableMapGestures(mapView)
    }

    private fun setDataEntry(viewId: Int, dataName: String, dataValue: String) {
        val dataEntryView = findViewById<View>(viewId)
        val nameTextView = dataEntryView.findViewById<TextView>(R.id.textViewDataName)
        val valueTextView = dataEntryView.findViewById<TextView>(R.id.textViewDataValue)
        nameTextView.text = dataName
        valueTextView.text = dataValue
    }

    private fun getAccuracyDescription(accuracy: Double): String {
        return when {
            accuracy <= 5 -> "a very precise location, such as which part of a room you are in."
            accuracy <= 10 -> "a precise location, such as the specific area of a small building."
            accuracy <= 50 -> "a general area, such as the building you are in."
            accuracy <= 100 -> "an area covering several buildings, making it possible to estimate your location within a small cluster."
            else -> "a broad area, making it difficult to determine your exact location, but approximating it within a larger cluster of buildings."
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

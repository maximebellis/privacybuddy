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



class LocSingleAccessActivity : BaseActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_specific_access)
        setupToolbar()
        setupToolbarWithNestedScrollListener(R.id.nestedScrollView, R.id.locationAccessByTextView, getString(R.string.location_access_by))

        val locationData: LocationData? = intent.extras?.getParcelable("locationData")

        locationData?.accuracy?.let { setupInfoButtons(it) }
        locationData?.let {
            findViewById<TextView>(R.id.dataEntryApp).text = it.appName
            findViewById<ImageView>(R.id.appLogoImageView).setImageDrawable(getAppIconByName(it.appName))
            findViewById<TextView>(R.id.dataEntryTime).text = it.timestamp

            setDataEntry(R.id.dataEntryAccuracy, "Accuracy:", formatNumber(it.accuracy))
            setDataEntry(R.id.dataEntrySpeed, "Speed:", formatNumber(it.speed))
            setDataEntry(R.id.dataEntryBearing, "Bearing:", formatNumber(it.bearing))
            setDataEntry(R.id.dataEntryLatitude, "Latitude:", formatNumber(it.latitude ?: 0.0))
            setDataEntry(R.id.dataEntryLongitude, "Longitude:", formatNumber(it.longitude ?: 0.0))
            setDataEntry(R.id.dataEntryAltitude, "Altitude:", formatNumber(it.altitude))
        }

        mapView = findViewById(R.id.mapView)
        initializeMap(locationData?.latitude ?: 0.0, locationData?.longitude ?: 0.0)

        val textViewAddress = findViewById<TextView>(R.id.textViewAdress)
        textViewAddress.text = "No address found"
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




    private fun formatNumber(value: Double?): String {
        return if (value != null) "%.7g".format(Locale.US, value) else "N/A"
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
        daysFilter = days
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
        return if (accuracy <= 5) {
            "a very precise location, such as which part of a room you are in."
        } else if (accuracy <= 10) {
            "a precise location, such as the specific area of a small building."
        } else if (accuracy <= 50) {
            "a general area, such as the building you are in."
        } else if (accuracy <= 100) {
            "an area covering several buildings, making it possible to estimate your location within a small cluster."
        } else {
            "a broad area, making it difficult to determine your exact location, but approximating it within a larger cluster of buildings."
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

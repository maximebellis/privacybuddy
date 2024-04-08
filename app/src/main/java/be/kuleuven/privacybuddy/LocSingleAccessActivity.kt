package be.kuleuven.privacybuddy

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
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
import com.google.gson.Gson
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import java.util.Locale
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LocSingleAccessActivity : BaseActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_specific_access)
        setupToolbar()
        setupToolbarWithNestedScrollListener(R.id.nestedScrollView, R.id.locationAccessByTextView, getString(R.string.location_access_by))

        val locationData = intent.getStringExtra("jsonData")?.let {
            Log.d("LocSingleAccessActivity", "Received JSON: $it")
            Gson().fromJson(it, LocationData::class.java)
        } ?: return

        locationData.accuracy?.let { setupInfoButtons(it) }

        with(locationData) {
            findViewById<TextView>(R.id.dataEntryApp).text = appName
            findViewById<ImageView>(R.id.appLogoImageView).setImageDrawable(getAppIconByName(appName))
            findViewById<TextView>(R.id.dataEntryTime).text = timestamp

            setDataEntry(R.id.dataEntryAccuracy, "Accuracy:", formatNumber(accuracy))
            setDataEntry(R.id.dataEntrySpeed, "Speed:", formatNumber(speed))
            setDataEntry(R.id.dataEntryBearing, "Bearing:", formatNumber(bearing))
            setDataEntry(R.id.dataEntryLatitude, "Latitude:", formatNumber(latitude ?: 0.0))
            setDataEntry(R.id.dataEntryLongitude, "Longitude:", formatNumber(longitude ?: 0.0))
            setDataEntry(R.id.dataEntryAltitude, "Altitude:", formatNumber(altitude))
        }

        mapView = findViewById(R.id.mapView)
        initializeMap(locationData.latitude ?: 0.0, locationData.longitude ?: 0.0)


        val textViewAddress = findViewById<TextView>(R.id.textViewAdress)
        val mapboxGeocoding = MapboxGeocoding.builder()
            .accessToken(this.getString(R.string.mapbox_access_token))
            .query(Point.fromLngLat(locationData.latitude ?: 0.0, locationData.longitude ?: 0.0))
            .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
            .build()

        mapboxGeocoding.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                if (response.body() != null && response.body()!!.features().isNotEmpty()) {
                    val placeName = response.body()!!.features()[0].placeName()
                    textViewAddress.text = placeName
                } else {
                    textViewAddress.text = "No address found"
                    // Log the response body for debugging
                    Log.d("MapboxGeocoding", "Response body: ${response.body()}")
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                // Log the error for debugging
                Log.e("MapboxGeocoding", "Error: ", t)
            }
        })
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

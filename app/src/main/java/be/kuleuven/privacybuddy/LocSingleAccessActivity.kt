package be.kuleuven.privacybuddy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import be.kuleuven.privacybuddy.data.LocationData
import java.util.Locale

class LocSingleAccessActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_specific_access)
        setupToolbar()

        val appNameTextView: TextView = findViewById(R.id.dataEntryApp)
        val timestampTextView: TextView = findViewById(R.id.dataEntryTime)

        // Retrieve the JSON string from the intent
        val eventJsonString = intent.getStringExtra("jsonData") ?: return
        Log.d("LocSingleAccessActivity", "Received JSON: $eventJsonString")

        // Parse the JSON string into a LocationData object
        val locationData = Gson().fromJson(eventJsonString, LocationData::class.java)

        // Populate the UI with the data
        locationData?.let {
            appNameTextView.text = it.appName
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

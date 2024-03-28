package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class LocSingleAccessActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_specific_access)

        val appName = intent.getStringExtra("appName") ?: return
        val timestamp = intent.getStringExtra("timestamp") ?: return

        val feature = loadMatchingFeatureFromGeoJson(appName, timestamp)

        feature?.let {
            //setDataEntry(R.id.dataEntryAppName, "App Name", it.properties.appName)
            //setDataEntry(R.id.dataEntryUsageType, "Usage Type", it.properties.usageType)
            //setDataEntry(R.id.dataEntryInteractionType, "Interaction Type", it.properties.interactionType)
            //setDataEntry(R.id.dataEntryTimestamp, "Timestamp", it.properties.timestamp)
            setDataEntry(R.id.dataEntryAccuracy, "Accuracy", it.properties.accuracy.toString())
            setDataEntry(R.id.dataEntrySpeed, "Speed", it.properties.speed.toString())
            setDataEntry(R.id.dataEntryBearing, "Bearing", it.properties.bearing.toString())
            //setDataEntry(R.id.dataEntryScreenState, "Screen State", it.properties.screenState)

            // Assuming the coordinates are [longitude, latitude]
            setDataEntry(R.id.dataEntryLatitude, "Latitude", it.geometry.coordinates[1].toString())
            setDataEntry(R.id.dataEntryLongitude, "Longitude", it.geometry.coordinates[0].toString())
            // If altitude is needed and available
            if (it.geometry.coordinates.size > 2) {
                setDataEntry(R.id.dataEntryAltitude, "Altitude", it.geometry.coordinates[2].toString())
            }
        }
    }

    private fun loadMatchingFeatureFromGeoJson(appName: String?, timestamp: String?): Feature? {
        appName ?: return null
        timestamp ?: return null

        val geoJsonString = assets.open(AppState.selectedGeoJsonFile).use { inputStream ->
            InputStreamReader(inputStream).readText()
        }

        val gson = Gson()
        val geoJsonType = object : TypeToken<GeoJson>() {}.type
        val geoJson: GeoJson = gson.fromJson(geoJsonString, geoJsonType)

        return geoJson.features.find { feature ->
            feature.properties.appName == appName && feature.properties.timestamp == timestamp
        }
    }

    private fun setDataEntry(viewId: Int, dataName: String, dataValue: String) {
        val dataEntryView = findViewById<View>(viewId)
        dataEntryView.findViewById<TextView>(R.id.textViewDataName).text = dataName
        dataEntryView.findViewById<TextView>(R.id.textViewDataValue).text = dataValue
    }

    override fun filterData(days: Int) {
    }

    data class GeoJson(
        val type: String,
        val features: List<Feature>
    )

    data class Feature(
        val type: String,
        val geometry: Geometry,
        val properties: LocationData
    )

    data class Geometry(
        val type: String,
        val coordinates: List<Double>
    )
    data class LocationData(
        @SerializedName("appName") val appName: String,
        @SerializedName("usageType") val usageType: String,
        @SerializedName("interactionType") val interactionType: String,
        @SerializedName("timestamp") val timestamp: String,
        @SerializedName("accuracy") val accuracy: Double,
        @SerializedName("speed") val speed: Double,
        @SerializedName("bearing") val bearing: Double,
        @SerializedName("screenState") val screenState: String
    )
}

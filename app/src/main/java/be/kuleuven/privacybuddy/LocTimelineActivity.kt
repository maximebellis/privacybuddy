package be.kuleuven.privacybuddy

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


class LocTimelineActivity : BaseActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctp_timeline_location)

        coroutineScope.launch {
            val features = withContext(Dispatchers.IO) {
                loadGeoJsonFromAssets(null) // Load data in the background
            }

            // Display features one by one
            features.forEach { feature ->
                withContext(Dispatchers.Main) { // Switch back to Main thread to update UI
                    val view = LayoutInflater.from(this@LocTimelineActivity).inflate(R.layout.component_timeline_unit, null, false)
                    populateViewWithFeatureData(view, feature)
                    findViewById<LinearLayout>(R.id.linear_layout_timeline).addView(view)
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancels coroutines when the activity is destroyed
    }

    private fun populateViewWithFeatureData(view: View, feature: Feature) {
        // Extract properties from the feature
        val timestamp = feature.getStringProperty("timestamp")
        val appName = feature.getStringProperty("appName")
        val usageType = feature.getStringProperty("usageType")
        val interactionType = feature.getStringProperty("interactionType")

        // Populate the view
        view.findViewById<TextView>(R.id.textViewTime).text = formatTimestamp(timestamp)
        view.findViewById<TextView>(R.id.textViewAppName).text = appName
        view.findViewById<TextView>(R.id.textViewAccuracy).text = usageType
        view.findViewById<TextView>(R.id.textViewAccessType).text = interactionType
    }

    private fun loadGeoJsonFromAssets(selectedAppName: String?): List<Feature> {
        val features = mutableListOf<Feature>()
        val bufferSize = 1024
        try {
            assets.open("dummy_location_data.geojson").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream), bufferSize).use { reader ->
                    var line: String?
                    val stringBuilder = StringBuilder()
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    val jsonString = stringBuilder.toString()
                    val featureCollection = FeatureCollection.fromJson(jsonString)
                    if (selectedAppName != null) {
                        features.addAll(
                            featureCollection.features()?.filter {
                                it.getStringProperty("appName") == selectedAppName
                            } ?: emptyList()
                        )
                    } else {
                        features.addAll(featureCollection.features() ?: emptyList())
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return features
    }



    // Utility function to format the timestamp or other transformations needed
    private fun formatTimestamp(timestamp: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return try {
            val date = inputFormat.parse(timestamp)
            date?.let { outputFormat.format(it) } ?: "Unknown"
        } catch (e: Exception) {
            "Error"
        }
    }

    // Make sure to add getStringProperty extension method or use another way to access properties
    private fun Feature.getStringProperty(propertyName: String): String =
        this.properties()?.get(propertyName)?.asString ?: ""
}

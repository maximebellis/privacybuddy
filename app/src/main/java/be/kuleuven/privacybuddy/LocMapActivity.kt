package be.kuleuven.privacybuddy


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.*
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import java.io.InputStream

class LocMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctp_map_location)

        mapView = findViewById(R.id.mapView)

        loadMapStyle()
        //setupLocationButton()
    }

    private fun loadMapStyle() {
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            it.addSource(geoJsonSource(APP_USAGE_SOURCE_ID) {
                featureCollection(loadGeoJsonFromAssets())
                cluster(true)
                clusterMaxZoom(14)
                clusterRadius(50)
            })

            it.addLayer(circleLayer(CLUSTERS_LAYER_ID, APP_USAGE_SOURCE_ID) {
                circleColor("#2c4b6e")
                circleRadius(15.0)
            })

            it.addLayer(symbolLayer(CLUSTER_COUNT_LAYER_ID, APP_USAGE_SOURCE_ID) {
                textField("{point_count_abbreviated}")
                textSize(12.0)
                textColor("#ffffff")
                textIgnorePlacement(true)
                textAllowOverlap(true)
                textAnchor(TextAnchor.CENTER)
            })

            centerMapOnLocation()
        }





    }

    private fun loadGeoJsonFromAssets(): FeatureCollection {
        val inputStream: InputStream = assets.open("dummy_location_data.geojson")
        val originalFeatureCollection = FeatureCollection.fromJson(inputStream.bufferedReader().use { it.readText() })

        val filteredFeatures = originalFeatureCollection.features()?.filter {
            it.getStringProperty("appName") == "YouTube"
        }

        return FeatureCollection.fromFeatures(filteredFeatures ?: listOf())
    }


    private fun centerMapOnLocation() {
        mapView.mapboxMap.setCamera(
            cameraOptions {
                center(Point.fromLngLat(4.7012, 50.8789))
                zoom(12.0)
            }
        )
    }

    //private fun setupLocationButton() {
    //    findViewById<View>(R.id.buttonShowTimeline).setOnClickListener {
    //        startActivity(Intent(this, LocTimelineActivity::class.java))
    //    }
    //}

    companion object {
        private const val APP_USAGE_SOURCE_ID = "app-usage-source"
        private const val CLUSTERS_LAYER_ID = "clusters"
        private const val CLUSTER_COUNT_LAYER_ID = "cluster-count"
    }
}

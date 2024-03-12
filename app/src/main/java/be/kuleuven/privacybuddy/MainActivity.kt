package be.kuleuven.privacybuddy


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.style

class MainActivity : BaseActivity() {

    private lateinit var miniMapView: MapView
    private lateinit var mapboxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_main)

        miniMapView = findViewById(R.id.miniMapView)
        mapboxMap = miniMapView.mapboxMap

        mapboxMap.loadStyle(style(Style.MAPBOX_STREETS) {
            // Here you can add layers, sources, images, etc.
        }) {
            // Style has been loaded, do any additional setup here
        }
        miniMapView.mapboxMap.setCamera(cameraOptions {
            center(Point.fromLngLat(4.7012, 50.8789))
            zoom(12.0)
        })

        val widgetMapLocation = findViewById<CardView>(R.id.widgetMapLocation)
        val widgetLocation = findViewById<CardView>(R.id.widgetLocation)

        widgetMapLocation.setOnClickListener {
            val intent = Intent(this, LocMapActivity::class.java)
            startActivity(intent)
        }

        widgetLocation.setOnClickListener {
            val intent = Intent(this, LocTimelineActivity::class.java)
            startActivity(intent)
        }


        setAppIcons()
    }



    private fun setAppIcons() {
        val tikTokIcon = getAppIconByName("TikTok")
        val gmailIcon = getAppIconByName("Gmail")
        val youtubeIcon = getAppIconByName("YouTube")

        findViewById<ImageView>(R.id.imageViewTikTok).setImageDrawable(tikTokIcon)
        findViewById<ImageView>(R.id.imageViewGmail).setImageDrawable(gmailIcon)
        findViewById<ImageView>(R.id.imageViewAppLogo).setImageDrawable(youtubeIcon)
    }
}


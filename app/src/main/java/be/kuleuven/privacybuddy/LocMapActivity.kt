package be.kuleuven.privacybuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.extension.getAppIconByName
import com.mapbox.maps.MapView

class LocMapActivity : BaseActivity(){

    private lateinit var mapView: MapView
    private var selectedAppName: String? = null
    private lateinit var appIconView: ImageView
    private lateinit var appNameTextView: TextView

    private val chooseAppLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val newAppName = result.data?.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)
                if (newAppName != selectedAppName) {
                    selectedAppName = newAppName
                    updateChooseAppDisplay(selectedAppName)
                    setupMapView(mapView, selectedAppName)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_map_location)
        setupToolbar()
        updateMapText()

        mapView =   findViewById(R.id.mapView)
        appIconView = findViewById(R.id.imageViewMostLocationAccessesApp3)
        appNameTextView = findViewById(R.id.textViewSelectedApp)
        selectedAppName = intent.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)

        setupChooseAppButton()
        setupMapView(mapView, selectedAppName)
    }

    private fun updateChooseAppDisplay(appName: String?) {
        appNameTextView.text = appName ?: "All Apps"
        appIconView.visibility = if (appName == null) View.GONE else View.VISIBLE
        if (appName != null) {
            appIconView.setImageDrawable(applicationContext.getAppIconByName(appName))
        }
    }

    private fun setupChooseAppButton() {
        updateChooseAppDisplay(selectedAppName)
        findViewById<View>(R.id.buttonChooseApp).setOnClickListener {
            chooseAppLauncher.launch(Intent(this, ChooseAppActivity::class.java))
        }

/*
        val buttonShowTimeline = findViewById<CardView>(R.id.buttonShowTimeline)
        buttonShowTimeline.setOnClickListener {
            val intent = Intent(this, LocTimelineActivity::class.java)
            intent.putExtra(ChooseAppActivity.SELECTED_APP_NAME, selectedAppName)
            startActivity(intent)
        }
 */

    }

    override fun filterData(days: Int) {
        daysFilter = days
        updateMapText()
        setupMapView(mapView, selectedAppName)
    }

    private fun updateMapText() {
        val mapText = if (daysFilter > 1) {
            getString(R.string.dashboard_text, daysFilter)
        } else {
            getString(R.string.dashboard_text_single_day)
        }

        findViewById<TextView>(R.id.textViewMap).text = mapText
    }


}
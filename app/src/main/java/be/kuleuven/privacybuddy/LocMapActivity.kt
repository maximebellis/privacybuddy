package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.SpinnerAdapter
import be.kuleuven.privacybuddy.data.SpinnerItem
import be.kuleuven.privacybuddy.extension.getAppIconByName
import com.mapbox.maps.MapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocMapActivity : BaseActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var mapView: MapView
    private var selectedAppName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_map_location)
        setupToolbar()
        updateMapText()

        mapView = findViewById(R.id.mapView)
        setupMapView(mapView, selectedAppName)

        setupSpinner()
    }

    private fun setupSpinner() {
        val spinner: Spinner = findViewById(R.id.spinnerChooseApp)
        val apps = getUniqueAppNamesFromGeoJson(daysFilter).sorted().toMutableList()
        apps.add(0, "All apps")

        val spinnerItems = apps.map { appName ->
            val appIcon = getAppIconByName(appName)
            SpinnerItem(appIcon!!, appName)
        }

        val adapter = SpinnerAdapter(this, R.layout.spinner_item, spinnerItems)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedAppName = if (parent.getItemAtPosition(position) == "All apps") null else (parent.getItemAtPosition(position) as SpinnerItem).appName
                updateMapView(mapView, selectedAppName)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancel the coroutine when the activity is destroyed
    }

    override fun filterData(days: Int) {
        daysFilter = days
        updateMapText()
        setupMapView(mapView, selectedAppName)
    }

    private fun updateMapText() {
        findViewById<TextView>(R.id.textViewMap).text = if (daysFilter > 1) getString(R.string.dashboard_text, daysFilter) else getString(R.string.dashboard_text_single_day)
    }
}
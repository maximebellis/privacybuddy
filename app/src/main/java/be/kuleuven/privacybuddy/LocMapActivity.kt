package be.kuleuven.privacybuddy

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.SpinnerAdapter
import be.kuleuven.privacybuddy.data.SpinnerItem
import be.kuleuven.privacybuddy.extension.getAppIconByName

class LocMapActivity : BaseActivity() {

    private var selectedAppName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_map_location)
        setupToolbar()
        updateMapText()
        setupMapView(findViewById(R.id.mapView), selectedAppName)
        setupSpinner()
    }

    private fun setupSpinner() {
        val spinner: Spinner = findViewById(R.id.spinnerChooseApp)
        spinner.dropDownVerticalOffset = 100

        val apps = getUniqueAppNamesFromGeoJson(daysFilter).sorted().toMutableList()
        apps.add(0, "All apps")

        val spinnerItems = apps.map { appName ->
            SpinnerItem(getAppIconByName(appName)!!, appName)
        }

        spinner.adapter = SpinnerAdapter(this, R.layout.spinner_item, spinnerItems)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as SpinnerItem
                selectedAppName = if (selectedItem.appName == "All apps") null else selectedItem.appName
                updateMapView(findViewById(R.id.mapView), selectedAppName)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun filterData(days: Int) {
        daysFilter = days
        updateMapText()
        updateMapView(findViewById(R.id.mapView), selectedAppName)
    }

    private fun updateMapText() {
        findViewById<TextView>(R.id.textViewMap).text = if (daysFilter > 1) getString(R.string.dashboard_text, daysFilter) else getString(R.string.dashboard_text_single_day)
    }
}
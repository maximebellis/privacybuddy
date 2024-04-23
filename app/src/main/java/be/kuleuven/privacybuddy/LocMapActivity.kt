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
import be.kuleuven.privacybuddy.utils.LocationDataUtils

class LocMapActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_map_location)
        setupToolbar()
        updateMapText()
        setupMapView(findViewById(R.id.mapView), selectedAppName)
        setupSpinner()
    }

    override fun onSpinnerItemSelected() {
        updateMapView(findViewById(R.id.mapView), selectedAppName)
    }


    override fun filterData(days: Int) {
        super.filterData(days)
        updateMapText()
        updateMapView(findViewById(R.id.mapView), selectedAppName)
    }

    private fun updateMapText() {
        findViewById<TextView>(R.id.textViewMap).text = if (daysFilter > 1) getString(R.string.dashboard_text, daysFilter) else getString(R.string.dashboard_text_single_day)
    }
}
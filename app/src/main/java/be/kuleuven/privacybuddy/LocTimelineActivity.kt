package be.kuleuven.privacybuddy

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.BaseActivity.AppSettings.daysFilter
import be.kuleuven.privacybuddy.adapter.LocationEventAdapter
import be.kuleuven.privacybuddy.extension.getAppIconByName
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import be.kuleuven.privacybuddy.utils.LocationDataUtils.prepareTimelineItems
import kotlinx.coroutines.*
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.ProgressBar


class LocTimelineActivity : BaseActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewTimeline: TextView

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var recyclerView: RecyclerView
    private var selectedAppName: String? = null
    private lateinit var appNameTextView: TextView
    private lateinit var appIconView: ImageView
    private lateinit var adapter: LocationEventAdapter // Declare the adapter

    private val chooseAppLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newAppName = result.data?.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)
            if (newAppName != selectedAppName) {
                selectedAppName = newAppName
                updateChooseAppDisplay(selectedAppName)
                refreshEvents()
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_timeline_location)
        setupToolbar()

        progressBar = findViewById(R.id.progressBar)


        textViewTimeline = findViewById(R.id.textViewTimeline)


        recyclerView = findViewById(R.id.recyclerViewTimelineLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LocationEventAdapter(this) // Initialize the adapter
        recyclerView.adapter = adapter // Set the adapter

        appIconView = findViewById(R.id.imageViewMostLocationAccessesApp3)
        appNameTextView = findViewById(R.id.textViewSelectedApp)


        selectedAppName = intent.getStringExtra(ChooseAppActivity.SELECTED_APP_NAME)
        setupChooseAppButton()
        updateChooseAppDisplay(selectedAppName)
        refreshEvents()

        val managePermissionsButton: TextView = findViewById(R.id.managePermissionsButton)
        managePermissionsButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        refreshEvents()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    private fun refreshEvents() = coroutineScope.launch {
        // Show the ProgressBar
        progressBar.visibility = View.VISIBLE
        textViewTimeline.visibility = View.GONE
        recyclerView.visibility = View.GONE

        val events = withContext(Dispatchers.IO) {
            LocationDataUtils.loadGeoJsonFromAssets(selectedAppName, this@LocTimelineActivity, days = daysFilter)
        }
        val groupedEvents = withContext(Dispatchers.Default) {
            prepareTimelineItems(events)
        }
        withContext(Dispatchers.Main) {
            adapter.submitList(groupedEvents) // Use submitList to update data

            // Hide the ProgressBar
            progressBar.visibility = View.GONE
            textViewTimeline.visibility = View.VISIBLE
            recyclerView.visibility = View.VISIBLE

        }
        updateTimelineText()
    }


    private fun setupChooseAppButton() {
        findViewById<View>(R.id.buttonChooseApp).setOnClickListener {
            chooseAppLauncher.launch(Intent(this, ChooseAppActivity::class.java))
        }
    }

    private fun updateChooseAppDisplay(appName: String?) {
        appNameTextView.text = appName ?: "All Apps"
        appIconView.visibility = if (appName != null) View.VISIBLE else View.GONE
        if (appName != null) {
            appIconView.setImageDrawable(applicationContext.getAppIconByName(appName))
        }
    }

    override fun filterData(days: Int) {
        daysFilter = days // Update the days filter
        refreshEvents() // Refresh the events list with the new filter
    }

    private fun updateTimelineText() {
        val timelineTextId = if (daysFilter > 1) R.string.timeline_text else R.string.timeline_text_single_day
        findViewById<TextView>(R.id.textViewTimeline).text = getString(timelineTextId, daysFilter)
    }


}



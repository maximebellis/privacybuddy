package be.kuleuven.privacybuddy

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.adapter.TopAppsAdapter
import be.kuleuven.privacybuddy.utils.LocationDataUtils
import be.kuleuven.privacybuddy.data.AppAccessInfo


class LocTopAppsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var topAppsAdapter: TopAppsAdapter
    override fun filterData(days: Int) {
        TODO("Not yet implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_top_apps)

        recyclerView = findViewById(R.id.recyclerViewTopAppsLocation)
        recyclerView.layoutManager = LinearLayoutManager(this)
        updateTopAccessedApps()

        setupToolbar()


    }

    private fun updateTopAccessedApps() {
        val topApps = AppState.topAccessedAppsCache ?: emptyList<AppAccessInfo>()
        topAppsAdapter = TopAppsAdapter(this , topApps)
        recyclerView.adapter = topAppsAdapter
    }
}

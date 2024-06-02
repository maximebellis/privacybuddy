package be.kuleuven.privacybuddy

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.adapter.LiveDataAdapter
import be.kuleuven.privacybuddy.adapter.LiveTimelineItem
import be.kuleuven.privacybuddy.data.LiveDataItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LiveActivity : BaseActivity() {

    private lateinit var liveDataAdapter: LiveDataAdapter
    private val liveDataList = mutableListOf<LiveDataItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_live_data)
        setupToolbar()
        initUI()
        fetchLiveData()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val itemsToShow = listOf(R.id.action_one_day, R.id.action_seven_days, R.id.action_twenty_one_days)
        itemsToShow.forEach { id ->
            menu.findItem(id)?.isVisible = true
        }
        menu.findItem(R.id.action_live_data)?.isVisible = false
        return super.onPrepareOptionsMenu(menu)
    }

    private fun initUI() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewLiveData)
        recyclerView.layoutManager = LinearLayoutManager(this)
        liveDataAdapter = LiveDataAdapter(mutableListOf())
        recyclerView.adapter = liveDataAdapter
    }

    private fun fetchLiveData() {
        val uri: Uri = Uri.parse("content://be.kuleuven.contentprovider/data")
        Log.d("PermissionLogDebug", "URI: $uri")
        try {
            Log.d("PermissionLogDebug", "Trying to access the provider")
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            Log.d("PermissionLogDebug", "Cursor: $cursor")
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val packageNameIndex = cursor.getColumnIndex("packageName")
                    val permissionGroupIndex = cursor.getColumnIndex("permissionGroup")
                    val accessTimeIndex = cursor.getColumnIndex("accessTime")

                    val packageName = if (packageNameIndex != -1) cursor.getString(packageNameIndex) else null
                    val permissionGroup = if (permissionGroupIndex != -1) cursor.getString(permissionGroupIndex) else null
                    val accessTime = if (accessTimeIndex != -1) cursor.getString(accessTimeIndex) else null

                    if (packageName != null && permissionGroup != null && accessTime != null) {
                        val liveDataItem = LiveDataItem(packageName, permissionGroup, accessTime)
                        liveDataList.add(liveDataItem)
                        Log.d("LiveDataDebug", "Added item: $liveDataItem")
                    }
                } while (cursor.moveToNext())
                cursor.close()

                // Sort LiveDataItems from new to old
                val sortedLiveDataList = liveDataList.sortedByDescending { it.accessTime }

                // Convert List<LiveDataItem> to List<LiveTimelineItem>
                val timelineItems = mutableListOf<LiveTimelineItem>()
                var lastDateLabel: String? = null
                sortedLiveDataList.forEach { item ->
                    val dateLabel = getFormattedDate(item)
                    if (lastDateLabel == null || lastDateLabel != dateLabel) {
                        timelineItems.add(LiveTimelineItem.DateLabel(dateLabel))
                        lastDateLabel = dateLabel
                    }
                    timelineItems.add(LiveTimelineItem.LiveDataItemWrapper(item))
                }

                // Update the adapter's data source
                liveDataAdapter.timelineItems = timelineItems
                liveDataAdapter.notifyDataSetChanged()
                Log.d("LiveDataDebug", "Notified adapter about data set change")
            }
        } catch (e: Exception) {
            Log.d("PermissionLogDebug", "Exception: ${e.message}")
        }
    }

    private fun getFormattedDate(item: LiveDataItem): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val itemDate = LocalDateTime.parse(item.accessTime, formatter).toLocalDate()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (itemDate) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> itemDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }
}
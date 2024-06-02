package be.kuleuven.privacybuddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.R
import be.kuleuven.privacybuddy.data.LiveDataItem
import be.kuleuven.privacybuddy.extension.getAppIconByName
import be.kuleuven.privacybuddy.extension.getAppNameByPackageName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class LiveTimelineItem {
    data class DateLabel(val date: String) : LiveTimelineItem()
    data class LiveDataItemWrapper(val liveDataItem: LiveDataItem) : LiveTimelineItem()
}

class LiveDataAdapter(var timelineItems: List<LiveTimelineItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class DateLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateLabel: TextView = itemView.findViewById(R.id.dataLabelTextView)
    }

    class LiveDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val locationAccessTime: TextView = itemView.findViewById(R.id.locationAccessTime)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val appIcon: ImageView = itemView.findViewById(R.id.locationIcon)
    }

    override fun getItemViewType(position: Int): Int {
        return when (timelineItems[position]) {
            is LiveTimelineItem.DateLabel -> 0
            is LiveTimelineItem.LiveDataItemWrapper -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val view = inflater.inflate(R.layout.component_date_label, parent, false)
            DateLabelViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_live_data, parent, false)
            LiveDataViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val timelineItem = timelineItems[position]) {
            is LiveTimelineItem.DateLabel -> {
                (holder as DateLabelViewHolder).dateLabel.text = timelineItem.date
            }
            is LiveTimelineItem.LiveDataItemWrapper -> {
                val context = holder.itemView.context
                val item = timelineItem.liveDataItem

                (holder as LiveDataViewHolder).locationAccessTime.text = formatTime(item.accessTime)
                val appName = context.getAppNameByPackageName(item.packageName)
                holder.appName.text = appName
                holder.appIcon.setImageDrawable(context.getAppIconByName(appName))
            }
        }
    }

    private fun formatTime(accessTime: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val dateTime = LocalDateTime.parse(accessTime, formatter)
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }



    override fun getItemCount() = timelineItems.size
}
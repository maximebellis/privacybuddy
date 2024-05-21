package be.kuleuven.privacybuddy.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.LocSingleAccessActivity
import be.kuleuven.privacybuddy.R
import be.kuleuven.privacybuddy.data.LocationData
import be.kuleuven.privacybuddy.extension.getAppIconByName
import be.kuleuven.privacybuddy.utils.DateTimeUtils.formatDateLabel
import be.kuleuven.privacybuddy.utils.DateTimeUtils.formatTimestamp
import java.util.*
import be.kuleuven.privacybuddy.LocTimelineActivity

sealed interface TimelineItem {
    data class DateLabel(val date: Date) : TimelineItem
    data class EventItem(val event: LocationData) : TimelineItem
}

class LocationEventAdapter(private val context: Context, private val isWidget: Boolean = false) :
    ListAdapter<TimelineItem, LocationEventAdapter.TimelineViewHolder>(DIFF_CALLBACK) {

    abstract class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: TimelineItem)
    }

    inner class DateLabelViewHolder(itemView: View) : TimelineViewHolder(itemView) {
        private val dateLabel: TextView = itemView.findViewById(R.id.dataLabelTextView)

        override fun bind(item: TimelineItem) {
            dateLabel.text = formatDateLabel((item as TimelineItem.DateLabel).date)

            if (isWidget) {
                itemView.setOnClickListener {
                    val intent = Intent(context, LocTimelineActivity::class.java)
                    context.startActivity(intent)
                }
            }
        }
    }

    inner class EventViewHolder(itemView: View) : TimelineViewHolder(itemView) {
        private val timeView: TextView = itemView.findViewById(R.id.textViewTime)
        private val appNameView: TextView = itemView.findViewById(R.id.textViewAppName)
        private val usageTypeView: TextView = itemView.findViewById(R.id.textViewAccuracy)
        private val interactionTypeView: TextView = itemView.findViewById(R.id.textViewAccessType)
        private val appLogoView: ImageView = itemView.findViewById(R.id.imageViewMostLocationAccessesApp3)
        private val verticalLineView: View = itemView.findViewById(R.id.verticalLineView)

        override fun bind(item: TimelineItem) {
            val eventItem = item as TimelineItem.EventItem
            timeView.text = formatTimestamp(eventItem.event.timestamp)
            appNameView.text = eventItem.event.appName
            usageTypeView.text = eventItem.event.usageType
            interactionTypeView.text = eventItem.event.interactionType
            appLogoView.setImageDrawable(context.getAppIconByName(eventItem.event.appName))
            verticalLineView.visibility = if (bindingAdapterPosition == itemCount - 1) View.INVISIBLE else View.VISIBLE

            itemView.setOnClickListener {
                val intent = Intent(context, if (isWidget) LocTimelineActivity::class.java else LocSingleAccessActivity::class.java).apply {
                    if (!isWidget) putExtra("locationData", eventItem.event)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TimelineItem.DateLabel -> 0
        is TimelineItem.EventItem -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = when (viewType) {
            0 -> inflater.inflate(R.layout.component_date_label, parent, false)
            1 -> inflater.inflate(R.layout.component_timeline_entry, parent, false)
            else -> throw IllegalArgumentException("Invalid View Type")
        }
        return if (viewType == 0) DateLabelViewHolder(view) else EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TimelineItem>() {
            override fun areItemsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean = oldItem == newItem
            override fun areContentsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean = oldItem == newItem
        }
    }
}
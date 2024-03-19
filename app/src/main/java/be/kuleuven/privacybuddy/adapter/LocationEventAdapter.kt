package be.kuleuven.privacybuddy.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.R
import be.kuleuven.privacybuddy.data.LocationData
import be.kuleuven.privacybuddy.extension.getAppIconByName
import be.kuleuven.privacybuddy.utils.DateTimeUtils.formatDateLabel
import be.kuleuven.privacybuddy.utils.DateTimeUtils.formatTimestamp
import java.util.*

sealed interface TimelineItem {
    data class DateLabel(val date: Date) : TimelineItem
    data class EventItem(val event: LocationData) : TimelineItem

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TimelineItem>() {
            override fun areItemsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean =
                oldItem == newItem
        }
    }
}

class LocationEventAdapter(private val context: Context) :
    ListAdapter<TimelineItem, LocationEventAdapter.TimelineViewHolder>(TimelineItem.DIFF_CALLBACK) {


    companion object {
        private const val TYPE_DATE_LABEL = 0
        private const val TYPE_EVENT = 1
    }

    abstract class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: TimelineItem)
    }

    inner class DateLabelViewHolder(itemView: View) : TimelineViewHolder(itemView) {
        private val dateLabel: TextView = itemView.findViewById(R.id.dataLabelTextView)

        override fun bind(item: TimelineItem) {
            (item as? TimelineItem.DateLabel)?.let { label ->
                dateLabel.text = formatDateLabel(label.date)
            }
        }
    }

    inner class EventViewHolder(itemView: View) : TimelineViewHolder(itemView) {
        private val timeView: TextView = itemView.findViewById(R.id.textViewTime)
        private val appNameView: TextView = itemView.findViewById(R.id.textViewAppName)
        private val usageTypeView: TextView = itemView.findViewById(R.id.textViewAccuracy)
        private val interactionTypeView: TextView = itemView.findViewById(R.id.textViewAccessType)
        private val appLogoView: ImageView = itemView.findViewById(R.id.imageViewAppLogo)
        private val verticalLineView: View = itemView.findViewById(R.id.verticalLineView)

        override fun bind(item: TimelineItem) {
            (item as? TimelineItem.EventItem)?.let { eventItem ->
                timeView.text = formatTimestamp(eventItem.event.timestamp)
                appNameView.text = eventItem.event.appName
                usageTypeView.text = eventItem.event.usageType
                interactionTypeView.text = eventItem.event.interactionType
                appLogoView.setImageDrawable(context.getAppIconByName(eventItem.event.appName))
                verticalLineView.visibility = if (bindingAdapterPosition == itemCount - 1) View.INVISIBLE else View.VISIBLE
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TimelineItem.DateLabel -> TYPE_DATE_LABEL
        is TimelineItem.EventItem -> TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder = when (viewType) {
        TYPE_DATE_LABEL -> DateLabelViewHolder(LayoutInflater.from(context).inflate(R.layout.component_date_label, parent, false))
        TYPE_EVENT -> EventViewHolder(LayoutInflater.from(context).inflate(R.layout.component_timeline_unit, parent, false))
        else -> throw IllegalArgumentException("Invalid View Type")
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) = holder.bind(getItem(position))








}

package be.kuleuven.privacybuddy.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
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
import com.google.gson.GsonBuilder
import java.util.*
import androidx.core.text.HtmlCompat

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
        private val appLogoView: ImageView = itemView.findViewById(R.id.imageViewMostLocationAccessesApp3)
        private val verticalLineView: View = itemView.findViewById(R.id.verticalLineView)

        private fun convertEventToJsonString(event: LocationData): String {
            val gson = GsonBuilder().serializeNulls().create() // Configured to serialize null values
            return gson.toJson(event)
        }


        private val infoIconView: ImageView = itemView.findViewById(R.id.iconView)
        override fun bind(item: TimelineItem) {
            (item as? TimelineItem.EventItem)?.let { eventItem ->
                timeView.text = formatTimestamp(eventItem.event.timestamp)
                appNameView.text = eventItem.event.appName
                usageTypeView.text = eventItem.event.usageType
                interactionTypeView.text = eventItem.event.interactionType
                appLogoView.setImageDrawable(context.getAppIconByName(eventItem.event.appName))
                verticalLineView.visibility = if (bindingAdapterPosition == itemCount - 1) View.INVISIBLE else View.VISIBLE

                itemView.setOnClickListener {
                    val eventJsonString = convertEventToJsonString(eventItem.event)
                    val intent = Intent(context, LocSingleAccessActivity::class.java).apply {
                        putExtra("jsonData", eventJsonString)
                    }
                    context.startActivity(intent)
                }

                infoIconView.setOnClickListener { view ->
                    showInfoPopup(view, eventItem.event)
                }
            }
        }
    }

    private fun showInfoPopup(view: View, event: LocationData) {
        val inflater = LayoutInflater.from(view.context)
        val popupView = inflater.inflate(R.layout.popup_info, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val textViewPopupContent: TextView = popupView.findViewById(R.id.textViewPopupContent)

        val usageTypeDescription = when (event.usageType) {
            "precise" -> HtmlCompat.fromHtml(view.context.getString(R.string.precise_location), HtmlCompat.FROM_HTML_MODE_LEGACY)
            "approximate" -> context.getString(R.string.approximate)
            else -> ""
        }
        val interactionTypeDescription = when (event.interactionType) {
            "sanctioned" -> context.getString(R.string.sanctioned)
            "foreground" -> context.getString(R.string.foreground)
            "background" -> context.getString(R.string.background)
            "subliminal" -> context.getString(R.string.subliminal)
            else -> ""
        }


        textViewPopupContent.text = listOf(usageTypeDescription, interactionTypeDescription)
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")

        popupWindow.showAsDropDown(view)
    }


    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TimelineItem.DateLabel -> TYPE_DATE_LABEL
        is TimelineItem.EventItem -> TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder = when (viewType) {
        TYPE_DATE_LABEL -> DateLabelViewHolder(LayoutInflater.from(context).inflate(R.layout.component_date_label, parent, false))
        TYPE_EVENT -> EventViewHolder(LayoutInflater.from(context).inflate(R.layout.component_timeline_entry, parent, false))
        else -> throw IllegalArgumentException("Invalid View Type")
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) = holder.bind(getItem(position))








}

package be.kuleuven.privacybuddy.adapter;

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.LocationEvent
import be.kuleuven.privacybuddy.R
import be.kuleuven.privacybuddy.extension.getAppIconByName
import java.text.SimpleDateFormat
import java.util.Locale

class LocationEventAdapter(private val events: List<LocationEvent>, private val context: Context) :
    RecyclerView.Adapter<LocationEventAdapter.ViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.component_timeline_unit, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]

        val appIcon = context.getAppIconByName(event.appName)

        holder.textViewTime.text = formatTimestamp(event.timestamp)
        holder.textViewAppName.text = event.appName
        holder.textViewAccuracy.text = event.usageType
        holder.textViewAccessType.text = event.interactionType
        holder.imageViewAppLogo.setImageDrawable(appIcon)

    }

    private fun formatTimestamp(timestamp: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            val date = inputFormat.parse(timestamp) // Parse into Date object
            date?.let { timeFormat.format(it) } ?: "Unknown Time"
        } catch (e: Exception) {
            "Invalid Format"
        }
    }

    override fun getItemCount() = events.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        val textViewAppName: TextView = itemView.findViewById(R.id.textViewAppName)
        val textViewAccuracy: TextView = itemView.findViewById(R.id.textViewAccuracy)
        val textViewAccessType: TextView = itemView.findViewById(R.id.textViewAccessType)
        val imageViewAppLogo: ImageView = itemView.findViewById(R.id.imageViewAppLogo)
    }

}

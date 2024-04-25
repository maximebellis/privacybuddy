package be.kuleuven.privacybuddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.data.AppAccessStats  // Import AppAccessStats instead of AppAccessInfo
import be.kuleuven.privacybuddy.R
import android.content.Context
import be.kuleuven.privacybuddy.extension.getAppIconByName
import android.graphics.Color
import be.kuleuven.privacybuddy.utils.LocationDataUtils

enum class DisplayMode {
    ACCESS_COUNT,
    FREQUENCY,
    PRIVACY_SCORE
}


class TopAppsAdapter(private val context: Context, private val topApps: List<AppAccessStats>, private var displayMode: DisplayMode) :
    RecyclerView.Adapter<TopAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewAppIcon: ImageView = view.findViewById(R.id.imageViewAppIcon)
        val textViewAppName: TextView = view.findViewById(R.id.textViewAppName)
        val textViewAppAccesses: TextView = view.findViewById(R.id.textViewAppAccesses)
        val progressBarAppUsage: ProgressBar = view.findViewById(R.id.progressBarAppUsage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.component_top_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = topApps[position]
        holder.textViewAppName.text = app.appName
        holder.imageViewAppIcon.setImageDrawable(context.getAppIconByName(app.appName))

        val max = calculateMaxForMode()
        holder.progressBarAppUsage.max = max  // Ensure max is updated each time

        when (displayMode) {
            DisplayMode.ACCESS_COUNT -> {
                holder.textViewAppAccesses.text = "${app.totalAccesses} accesses"
                holder.progressBarAppUsage.progress = app.totalAccesses
            }
            DisplayMode.FREQUENCY -> {
                val frequency = if (app.days > 0) app.totalAccesses.toDouble() / app.days else 0.0
                holder.textViewAppAccesses.text = String.format("%.1f accesses per day", frequency)
                holder.progressBarAppUsage.progress = frequency.toInt()
            }
            DisplayMode.PRIVACY_SCORE -> {
                holder.textViewAppAccesses.text = String.format("Privacy Score: %.1f", app.privacyScore)
                holder.progressBarAppUsage.progress = app.privacyScore.toInt()
                holder.progressBarAppUsage.progressDrawable.setColorFilter(
                    LocationDataUtils.getPrivacyScoreColor(app.privacyScore), android.graphics.PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private fun calculateMaxForMode(): Int = when (displayMode) {
        DisplayMode.ACCESS_COUNT -> topApps.maxOfOrNull { it.totalAccesses } ?: 1
        DisplayMode.FREQUENCY -> topApps.maxOfOrNull { it.days.let { days -> if (days > 0) it.totalAccesses / days else 0 } } ?: 1
        DisplayMode.PRIVACY_SCORE -> 100  // Since privacy score is scaled between 0 and 100
    }



    override fun getItemCount() = topApps.size

}

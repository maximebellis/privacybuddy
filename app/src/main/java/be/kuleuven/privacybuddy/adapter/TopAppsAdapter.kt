package be.kuleuven.privacybuddy.adapter

import android.content.Context
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.LocMapActivity
import be.kuleuven.privacybuddy.LocTimelineActivity
import be.kuleuven.privacybuddy.R
import be.kuleuven.privacybuddy.data.AppAccessStats
import be.kuleuven.privacybuddy.extension.getAppIconByName
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
        holder.progressBarAppUsage.max = max

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

        holder.itemView.setOnClickListener { view ->
            showPopupMenu(view, app.appName)
        }
    }

    private fun showPopupMenu(view: View, appName: String) {
        val wrapper = ContextThemeWrapper(context, R.style.PopupMenuStyle)
        val popup = PopupMenu(wrapper, view)
        popup.menu.apply {
            add(Menu.NONE, Menu.NONE, 0, "Go to map for $appName")
            add(Menu.NONE, Menu.NONE, 1, "Go to timeline for $appName")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.order) {
                0 -> {  // Map
                    val intent = Intent(context, LocMapActivity::class.java).apply {
                        putExtra("APP_NAME", appName)
                    }
                    context.startActivity(intent)
                    true
                }
                1 -> {  // Timeline
                    val intent = Intent(context, LocTimelineActivity::class.java).apply {
                        putExtra("APP_NAME", appName)
                    }
                    context.startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }


    private fun calculateMaxForMode(): Int = when (displayMode) {
        DisplayMode.ACCESS_COUNT -> topApps.maxOfOrNull { it.totalAccesses } ?: 1
        DisplayMode.FREQUENCY -> topApps.maxOfOrNull { it.days.let { days -> if (days > 0) it.totalAccesses / days else 0 } } ?: 1
        DisplayMode.PRIVACY_SCORE -> 100  // Since privacy score is scaled between 0 and 100
    }



    override fun getItemCount() = topApps.size

}

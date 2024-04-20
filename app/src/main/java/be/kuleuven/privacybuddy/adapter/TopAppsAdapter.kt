package be.kuleuven.privacybuddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.data.AppAccessInfo
import be.kuleuven.privacybuddy.R
import android.content.Context
import be.kuleuven.privacybuddy.extension.getAppIconByName


class TopAppsAdapter(private val context: Context, private val topApps: List<AppAccessInfo>) :
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
        // Set your view components from app. Example:
        holder.textViewAppName.text = app.appName
        holder.textViewAppAccesses.text = "${app.accessCount} accesses"
        // Assume maximum access count for scaling progress bars consistently
        val maxAccessCount = topApps.maxOfOrNull { it.accessCount } ?: 1
        holder.progressBarAppUsage.max = maxAccessCount
        holder.progressBarAppUsage.progress = app.accessCount
        holder.imageViewAppIcon.setImageDrawable(context.getAppIconByName(app.appName))
    }

    override fun getItemCount() = topApps.size
}

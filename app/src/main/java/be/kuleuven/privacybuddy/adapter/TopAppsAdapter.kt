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

class TopAppsAdapter(private val context: Context, private val topApps: List<AppAccessStats>) :
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
        holder.textViewAppAccesses.text = "${app.totalAccesses} accesses"  // Updated to use totalAccesses
        val maxAccessCount = topApps.maxOfOrNull { it.totalAccesses } ?: 1  // Use totalAccesses
        holder.progressBarAppUsage.max = maxAccessCount
        holder.progressBarAppUsage.progress = app.totalAccesses
        holder.imageViewAppIcon.setImageDrawable(context.getAppIconByName(app.appName))
    }

    override fun getItemCount() = topApps.size
}

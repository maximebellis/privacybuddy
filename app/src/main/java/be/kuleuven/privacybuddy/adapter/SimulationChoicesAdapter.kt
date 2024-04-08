package be.kuleuven.privacybuddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import be.kuleuven.privacybuddy.R

class SimulationChoicesAdapter(private val titles: List<String>, private val descriptions: List<String>) :
    RecyclerView.Adapter<SimulationChoicesAdapter.ChoiceViewHolder>() {

    inner class ChoiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textView_choice_title)
        val description: TextView = view.findViewById(R.id.textView_choice_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.component_simulation_viewpager_choice, parent, false)
        return ChoiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChoiceViewHolder, position: Int) {
        holder.title.text = titles[position]
        holder.description.text = descriptions[position]
    }

    override fun getItemCount(): Int = titles.size
}

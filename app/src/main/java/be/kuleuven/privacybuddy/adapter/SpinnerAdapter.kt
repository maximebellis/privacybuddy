package be.kuleuven.privacybuddy.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import be.kuleuven.privacybuddy.R
import be.kuleuven.privacybuddy.data.SpinnerItem

class SpinnerAdapter(context: Context, resource: Int, objects: List<SpinnerItem>) :
    ArrayAdapter<SpinnerItem>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        val spinnerItem = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)

        val appIconImageView = view.findViewById<ImageView>(R.id.appIcon)
        val appNameTextView = view.findViewById<TextView>(R.id.appName)

        if (spinnerItem?.appName == "All apps") {
            appIconImageView.visibility = View.GONE
        } else {
            appIconImageView.visibility = View.VISIBLE
            appIconImageView.setImageDrawable(spinnerItem?.appIcon)
        }

        appNameTextView.text = spinnerItem?.appName

        return view
    }
}
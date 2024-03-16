package be.kuleuven.privacybuddy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import be.kuleuven.privacybuddy.extension.getAppIconByName
import org.json.JSONObject


class ChooseAppActivity : BaseActivity() {

    companion object {
        const val SELECTED_APP_NAME = "selected_app_name"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctp_choose_app)
        setupToolbar()
        setupToolbarWithScrollListener(R.id.nestedScrollView, R.id.chooseAppTitleTextView, getString(R.string.choose_app_title))

        val linearLayout = findViewById<LinearLayout>(R.id.linearLayoutApps)

        // Add "All Apps" option first
        linearLayout.addView(createAppView("All Apps").apply {
            setOnClickListener { finishWithResult(null) } // Return to previous activity with "All Apps"
        })

        // Retrieve and sort app names alphabetically
        val appNames = getUniqueAppNamesFromGeoJson("dummy_location_data.geojson").sorted()

        // Then add all other apps in alphabetical order
        appNames.forEach { appName ->
            linearLayout.addView(createAppView(appName).apply {
                setOnClickListener { finishWithResult(appName)  }
            })
        }
    }

    private fun createAppView(appName: String): View {
        val view = LayoutInflater.from(this).inflate(R.layout.component_app_choice, null, false)
        val appNameTextView: TextView = view.findViewById(R.id.textViewAppName)
        val appLogoImageView: ImageView = view.findViewById(R.id.imageViewAppLogo)

        appNameTextView.text = appName
        val appIcon = getAppIconByName(appName)

        if (appIcon != null) {
            appLogoImageView.visibility = View.VISIBLE
            appLogoImageView.setImageDrawable(appIcon)
            // Since the icon is visible, ensure text is aligned to start of icon or its original position
            (appNameTextView.layoutParams as RelativeLayout.LayoutParams).apply {
                addRule(RelativeLayout.START_OF, R.id.imageViewAppLogo)
                addRule(RelativeLayout.ALIGN_PARENT_END, 0) // Remove end alignment
            }
        } else {
            appLogoImageView.visibility = View.GONE
            // Align text to the end if there is no icon
            (appNameTextView.layoutParams as RelativeLayout.LayoutParams).apply {
                addRule(RelativeLayout.START_OF, 0) // Remove start alignment to the icon
                addRule(RelativeLayout.ALIGN_PARENT_END) // Align to the end of parent
            }
        }

        val verticalMargin = view.resources.getDimensionPixelSize(R.dimen.vertical_margin_choose_app)
        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
            it.setMargins(0, verticalMargin, 0, 0)
        }

        return view
    }

    private fun finishWithResult(selectedAppName: String?) {
        val intent = Intent().apply {
            putExtra(SELECTED_APP_NAME, selectedAppName)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun getUniqueAppNamesFromGeoJson(geoJsonFileName: String): List<String> = runCatching {
        assets.open(geoJsonFileName).bufferedReader().use { it.readText() }
            .let { JSONObject(it).getJSONArray("features") }
            .let { features ->
                (0 until features.length()).mapNotNull { index ->
                    features.getJSONObject(index).getJSONObject("properties").getString("appName")
                }.distinct()
            }
    }.getOrElse { emptyList() }
}

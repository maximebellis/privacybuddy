package be.kuleuven.privacybuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONObject

class ChooseAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ctp_choose_app)

        val linearLayout = findViewById<LinearLayout>(R.id.linearLayoutApps)

        // Add "All Apps" option first
        linearLayout.addView(createAppView("All Apps").apply {
            setOnClickListener { launchMapActivity(null) } // null indicates "All Apps"
        })

        // Retrieve and sort app names alphabetically
        val appNames = getUniqueAppNamesFromGeoJson("dummy_location_data.geojson").sorted()

        // Then add all other apps in alphabetical order
        appNames.forEach { appName ->
            val appIcon = getAppIconByName(appName)
            linearLayout.addView(createAppView(appName, appIcon).apply {
                setOnClickListener { launchMapActivity(appName) }
            })
        }
    }



    private fun createAppView(appName: String, appIcon: Drawable? = null): View {
        val view = LayoutInflater.from(this).inflate(R.layout.component_app_choice, null, false)
        val appNameTextView: TextView = view.findViewById(R.id.textViewAppName)
        val appLogoImageView: ImageView = view.findViewById(R.id.imageViewAppLogo)

        appNameTextView.text = appName

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


    private fun launchMapActivity(appName: String?) {
        val intent = Intent(this, LocMapActivity::class.java).apply {
            // If appName is null, it means "All apps" was selected
            appName?.let { putExtra(LocMapActivity.SELECTED_APP_NAME, it) }
        }
        startActivity(intent)
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

    private fun getAppIconByName(appName: String): Drawable? =
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED) {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA).find {
                packageManager.getApplicationLabel(it).toString() == appName
            }?.loadIcon(packageManager)
        } else null
}

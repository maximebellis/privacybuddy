package be.kuleuven.privacybuddy
import be.kuleuven.privacybuddy.adapter.SimulationChoicesAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import be.kuleuven.privacybuddy.AppState.clearTopAccessedAppsCache
import be.kuleuven.privacybuddy.utils.LocationDataUtils


class SimulationActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var buttonPrevious: ImageButton
    private lateinit var buttonNext: ImageButton

    private lateinit var adapter: SimulationChoicesAdapter

    val titles = listOf("Datastream 1",
        "Datastream 2",
        "Datastream 3",
        "Dummy data 1",
        "Dummy data 2",
        "Student/tutor simudata",
        "Dummy data 4",
        "Dummy data 5",
        "Dummy data 6",
        "Dummy data 7",
        )


    override fun filterData(days: Int) {
        TODO("Not yet implemented")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_simulation)

        val descriptions = listOf(
            getString(R.string.description_1),
            getString(R.string.description_2),
            getString(R.string.description_3),
            "dummydata 1 around Leuven",
            "dummydata 2 around Leuven",
            "Simulated data made with chatgpt for a student/tutor that teaches and studies in and around Leuven",
            getString(R.string.description_4),
            getString(R.string.description_5),
            getString(R.string.description_6),
            getString(R.string.description_7),
        )

        val buttonChoose: Button = findViewById(R.id.button_choose)


        buttonChoose.setOnClickListener {
            val selectedPageIndex = viewPager.currentItem
            AppState.selectedGeoJsonFile = when (selectedPageIndex) {
                0 -> "real_data_w_simu_features_1.geojson"
                1 -> "real_data_w_simu_features_2.geojson"
                2 -> "real_data_w_simu_features_3.geojson"
                3 -> "dummy_location_1.geojson"
                4 -> "dummy_location_3.geojson"
                5 -> "simu_data_tutor.geojson"
                else -> "dummy_location_1.geojson"
            }

            clearTopAccessedAppsCache()

            LocationDataUtils.buildAppAccessStatsFromGeoJson(this)
            LocationDataUtils.cacheAllLocationData(this)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }



        viewPager = findViewById(R.id.viewPager_choices)
        buttonPrevious = findViewById(R.id.button_previous)
        buttonNext = findViewById(R.id.button_next)


        adapter = SimulationChoicesAdapter(titles, descriptions)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(pageChangeCallback)

        buttonPrevious.setOnClickListener {
            viewPager.currentItem -= 1
        }

        buttonNext.setOnClickListener {
            viewPager.currentItem += 1
        }
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            buttonPrevious.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            buttonNext.visibility = if (position == titles.size - 1) View.INVISIBLE else View.VISIBLE
        }
    }
}

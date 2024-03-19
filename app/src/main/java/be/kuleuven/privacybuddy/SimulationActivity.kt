package be.kuleuven.privacybuddy
import SimulationChoicesAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import be.kuleuven.privacybuddy.BaseActivity
import be.kuleuven.privacybuddy.R



class SimulationActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var buttonPrevious: ImageButton
    private lateinit var buttonNext: ImageButton

    private lateinit var adapter: SimulationChoicesAdapter

    val titles = listOf("The Chef", "The Student", "The Engineer")
    val descriptions = listOf("Description 1 Description 1 Description 1 Description 1 Description 1 Description 1 Description 1 Description 1 Description 1 Description 1" +
            "Description 1 Description 1 Description 1 Description 1 Description 1 Description 1 Description 1 Description 1 Description 1 Description 1"
            , "Description 2", "Description 3")

    override fun filterData(days: Int) {
        TODO("Not yet implemented")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simulation_page)

        val buttonChoose: Button = findViewById(R.id.button_choose)


        buttonChoose.setOnClickListener {
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
            viewPager.currentItem = viewPager.currentItem - 1
        }

        buttonNext.setOnClickListener {
            viewPager.currentItem = viewPager.currentItem + 1
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

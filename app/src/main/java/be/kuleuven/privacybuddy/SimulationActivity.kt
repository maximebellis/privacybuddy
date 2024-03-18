package be.kuleuven.privacybuddy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class SimulationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simulation_page)

        // Find the button by its ID
        val chooseButton = findViewById<Button>(R.id.button_choose)

        // Set an OnClickListener
        chooseButton.setOnClickListener {
            // Create an Intent to start MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            finish()
        }
    }
}

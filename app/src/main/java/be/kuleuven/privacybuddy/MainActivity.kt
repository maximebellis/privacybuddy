package be.kuleuven.privacybuddy


import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_main)

        val widgetMapLocation: View = findViewById(R.id.widgetMapLocation)
        widgetMapLocation.setOnClickListener {
            val intent = Intent(this, LocMapActivity::class.java)
            startActivity(intent)
        }
    }
}


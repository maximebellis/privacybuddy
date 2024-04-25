package be.kuleuven.privacybuddy

import android.os.Bundle

class LocInfoActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_info)
        setupToolbar()

    }
}
package com.example.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        // below line is to change
        // the title of our action bar.
        supportActionBar?.setTitle("Settings")

        // below line is used to check if
        // frame layout is empty or not.
        // below line is used to check if
        // frame layout is empty or not.
        if (findViewById<View?>(R.id.idFrameLayout) != null) {
            if (savedInstanceState != null) {
                return
            }
            // below line is to inflate our fragment.
            fragmentManager.beginTransaction().add(R.id.idFrameLayout, SettingsFragment()).commit()
        }
    }
}
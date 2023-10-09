package com.example.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Switch
import android.widget.Toast

class SettingActivity : AppCompatActivity() {
    private var isFeatureEnabled = false // Default: feature disabled



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        val switchSetting: Switch = findViewById(R.id.security_switch)
        switchSetting.isChecked = isFeatureEnabled // Switch ka initial state set karo

        switchSetting.setOnCheckedChangeListener { _, isChecked ->
            isFeatureEnabled = isChecked // Switch ka state variable mein store karo
            // Feature enable ya disable karne ke functions yahan call karo
            if (isChecked) {
                isFeatureEnabled = true

                Toast.makeText(this, "switch on", Toast.LENGTH_SHORT).show()
            } else {
                isFeatureEnabled=false
                Toast.makeText(this, "switch of", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
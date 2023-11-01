package com.example.calculator

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.Toast
import android.widget.Toolbar
import com.example.calculator.databinding.ActivityLockerBinding
import com.example.calculator.databinding.ActivityPhotosBinding
import com.example.calculator.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    private var isFeatureEnabled = false // Default: feature disabled
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivitySettingBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.toolbar.setNavigationOnClickListener(View.OnClickListener {
            val main = Intent(applicationContext, MainActivity::class.java)
            startActivity(main)
        })


        sharedPreferences = getSharedPreferences("SwitchState", Context.MODE_PRIVATE)
        val switchButton = findViewById<Switch>(R.id.security_switch)
        switchButton.isChecked = sharedPreferences.getBoolean("SWITCH_STATE", false)

        switchButton.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("SWITCH_STATE", isChecked)
            editor.apply()

            if (isChecked) {
                Toast.makeText(this, "Security Mode On", Toast.LENGTH_SHORT).show()
                // Switch button on hai
            } else {
                Toast.makeText(this, "Security Mode Off", Toast.LENGTH_SHORT).show()
                // Switch button off hai
            }
        }


    }

}
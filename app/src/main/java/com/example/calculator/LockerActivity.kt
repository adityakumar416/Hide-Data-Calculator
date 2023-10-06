package com.example.calculator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.calculator.databinding.ActivityLockerBinding
import com.example.calculator.photos.PhotosActivity
import com.example.calculator.videos.VideosActivity

class LockerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLockerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.photos.setOnClickListener {
            val intent = Intent(this, PhotosActivity::class.java)
            startActivity(intent)
        }
        binding.videos.setOnClickListener {
            val intent = Intent(this, VideosActivity::class.java)
            startActivity(intent)
        }

    }
}
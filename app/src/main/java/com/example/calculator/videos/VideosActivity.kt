package com.example.calculator.videos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculator.LockerActivity
import com.example.calculator.MainViewModel
import com.example.calculator.R
import com.example.calculator.databinding.ActivityVideosBinding
import com.example.calculator.videoPlayer.ExoPlayerActivity

class VideosActivity : AppCompatActivity(), VideoItemClickListener  {
    private lateinit var binding:ActivityVideosBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var videoAdapter: VideoAdapter
    private var viewModel:MainViewModel? = null

    @SuppressLint("MissingInflatedId", "WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        videoAdapter = VideoAdapter(mutableListOf(),this,this) // Initialize with an empty list initially
        recyclerView.adapter = videoAdapter


        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        binding.progressBar.visibility = View.VISIBLE
        binding.mainLayout.visibility = View.GONE


        viewModel?.videoUploadStatus?.observe(this) { status ->

            if (status) {
                Toast.makeText(this, "Video Uploaded Successfully", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Failed to Upload Video", Toast.LENGTH_SHORT).show()
            }

        }


        viewModel?.videoList?.observe(this) { videos ->
            binding.progressBar.visibility = View.GONE
            binding.mainLayout.visibility = View.VISIBLE
            videoAdapter.updateData(videos)

        }
        viewModel?.fetchVideoItemsFromFirebase()

        binding.toolbar.setNavigationOnClickListener(View.OnClickListener {
            val main = Intent(applicationContext, LockerActivity::class.java)
            startActivity(main)
        })


        binding.addVideo.setOnClickListener {
            // Open the gallery to select a video
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "video/*"
            startActivityForResult(intent, PICK_VIDEO_REQUEST)
        }
        
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { videoUri ->
                viewModel?.uploadVideoToFirebase(videoUri,this)
            }
        }
    }


    private fun checkEmptyFolderVisibility() {
        if (binding.progressBar.visibility == View.GONE && videoAdapter.itemCount == 0) {
            // Show empty folder image
            binding.emptyFolderImageView.visibility = View.VISIBLE
        } else {
            // Hide empty folder image and show videos
            binding.emptyFolderImageView.visibility = View.GONE
        }
    }


    companion object {
        private const val PICK_VIDEO_REQUEST = 1
    }

    override fun onVideoItemClicked(videoUrl: String) {
        val intent = Intent(this, ExoPlayerActivity::class.java)
        intent.putExtra("VIDEO_URL", videoUrl)
        startActivity(intent)
    }


}
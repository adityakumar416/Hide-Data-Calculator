package com.example.calculator.photos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculator.LockerActivity
import com.example.calculator.MainViewModel
import com.example.calculator.R
import com.example.calculator.databinding.ActivityPhotosBinding

class PhotosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotosBinding
    private lateinit var uri: Uri
    private lateinit var imageList: ArrayList<ImageModel>
    private var viewModel: MainViewModel? = null
    private var isGridView = true // Shared preference to store view mode
    private val PICK_IMAGES_REQUEST = 71


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // binding.iconView.text = "List View"

        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        binding.progressBar.visibility = View.VISIBLE
        binding.mainLayout.visibility = View.GONE

        binding.addImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGES_REQUEST)
        }

        binding.toolbar.setNavigationOnClickListener(View.OnClickListener {
            val main = Intent(applicationContext, LockerActivity::class.java)
            startActivity(main)
        })

        imageList = arrayListOf()

        viewModel?.uploadStatus?.observe(this, Observer {
            if (it) {
                Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show()
                viewModel?.fetchImages()
            }
            else{
                Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show()
            }
        })

        viewModel?.imageList?.observe(this, Observer {
            binding.progressBar.visibility = View.GONE
            binding.mainLayout.visibility = View.VISIBLE
            imageList.clear()
            imageList.addAll(it)

            if (imageList.isEmpty()) {
                // Show empty folder image
                binding.emptyFolderImageView.visibility = View.VISIBLE
                binding.recyclerview.visibility = View.GONE
            } else {
                // Hide empty folder image and show images
                binding.emptyFolderImageView.visibility = View.GONE
                binding.recyclerview.visibility = View.VISIBLE
                binding.recyclerview.adapter?.notifyDataSetChanged()
            }
        })
        viewModel?.fetchImages()


        val sharedPref = getSharedPreferences("view_mode", MODE_PRIVATE)
        isGridView = sharedPref.getBoolean("is_grid_view", true)

        if (isGridView) {
            binding.recyclerview.layoutManager = GridLayoutManager(this, 4)
            binding.recyclerview.adapter = ShowGridViewImageAdapter(imageList, this@PhotosActivity)
        } else {
            binding.recyclerview.layoutManager =
                LinearLayoutManager(this, RecyclerView.VERTICAL, false)
            binding.recyclerview.adapter = ShowImageAdapter(imageList, this@PhotosActivity)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    viewModel?.uploadImage(uri,this)
                }
            } ?: run {
                data?.data?.let { uri ->
                    viewModel?.uploadImage(uri,this)
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.photos_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.view -> {

                isGridView = !isGridView

                if (isGridView) {
                    binding.recyclerview.layoutManager = GridLayoutManager(this, 4)
                    binding.recyclerview.adapter =
                        ShowGridViewImageAdapter(imageList, this@PhotosActivity)
                    item.setTitle("List View")
                } else {
                    binding.recyclerview.layoutManager =
                        LinearLayoutManager(this, RecyclerView.VERTICAL, false)
                    binding.recyclerview.adapter = ShowImageAdapter(imageList, this@PhotosActivity)
                    item.setTitle("Grid View")
                }

                val sharedPref = getSharedPreferences("view_mode", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean("is_grid_view", isGridView)
                    apply()
                }

                true
            }
            /* R.id.delete_all ->{
                 Toast.makeText(this@PhotosActivity,"Delete all clicked", Toast.LENGTH_SHORT).show();
                 return true
             }*/

            else -> super.onOptionsItemSelected(item)
        }
    }


}
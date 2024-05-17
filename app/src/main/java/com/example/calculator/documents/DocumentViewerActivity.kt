package com.example.calculator.documents

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.calculator.R
import com.example.calculator.databinding.ActivityDocumentViewerBinding
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle

class DocumentViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentViewerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val pdfUrl = intent.getStringExtra("pdf_url")

        if (pdfUrl != null) {
            val uri = Uri.parse(pdfUrl)
            // Load PDF using PDFView library
            binding.pdfView.fromUri(uri)
                .defaultPage(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .onPageError { page, _ ->
                    // Handle page error
                }
                .onTap { false }
                .scrollHandle(DefaultScrollHandle(this))
                .spacing(10) // in dp
                .load()
        } else {
            // Handle the case when pdfUrl is null
            // Maybe show an error message or finish the activity
            finish()
        }


    }
}
package com.example.calculator.documents

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculator.LockerActivity
import com.example.calculator.MainViewModel
import com.example.calculator.R
import com.example.calculator.databinding.ActivityDocumentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class DocumentActivity : AppCompatActivity(), DeleteInterface {

    private val PICK_PDF_REQUEST = 1
    private lateinit var selectedDocumentUri: Uri
    private lateinit var documents: MutableList<Document>
    private lateinit var storageReference: StorageReference
    private lateinit var documentAdapter: DocumentAdapter
    private var viewModel: MainViewModel? = null
    private lateinit var binding: ActivityDocumentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            val main = Intent(applicationContext, LockerActivity::class.java)
            startActivity(main)
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.mainLayout.visibility = View.GONE

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        documents = mutableListOf()
        documentAdapter = DocumentAdapter(this, documents, this)

        val documentRecyclerView: RecyclerView = findViewById(R.id.documentRecyclerView)
        documentRecyclerView.adapter = documentAdapter
        documentRecyclerView.layoutManager = LinearLayoutManager(this)

        viewModel?.documents?.observe(this, Observer { newDocuments ->
            binding.progressBar.visibility = View.GONE
            binding.mainLayout.visibility = View.VISIBLE
            documents.clear()
            documents.addAll(newDocuments)
            documentAdapter.notifyDataSetChanged()
        })
        viewModel?.fetchDocuments()


        viewModel?.documentsUploadStatus?.observe(this, Observer { success ->

            if (success) {
                Toast.makeText(this, "Document uploaded successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Document upload failed", Toast.LENGTH_SHORT).show()
            }

        })

        val selectDocumentButton: TextView = findViewById(R.id.selectDocumentButton)
        selectDocumentButton.setOnClickListener {
            openFileChooser()
        }

        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        storageReference = FirebaseStorage.getInstance().getReference(uid).child("documents")
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "application/pdf" // You can also use "*/*" to allow all file types
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_PDF_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedDocumentUri = data.data!!
            val documentName = getOriginalFileName(selectedDocumentUri)

            viewModel?.uploadDocumentToFirebaseStorage(documentName, selectedDocumentUri, this)
            // val documentName = "Document_" + System.currentTimeMillis() + ".pdf"
            // uploadDocumentToFirebaseStorage(documentName, selectedDocumentUri)
        }
    }

    @SuppressLint("Range")
    private fun getOriginalFileName(uri: Uri?): String {
        val cursor = contentResolver.query(uri!!, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                cursor.close()
                return displayName
            }
        }
        return "video_${System.currentTimeMillis()}.mp4"
    }


    override fun onDeleteDocument(document: Document) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        val firebaseStorage = FirebaseStorage.getInstance().getReference(uid).child("documents/")

        MaterialAlertDialogBuilder(this).setTitle("Delete Document")
            .setMessage("Do you want to delete this Document ?")
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton("Yes") { dialog, _ ->
                firebaseStorage.storage.getReferenceFromUrl(document.downloadUrl.toString())
                    .delete().addOnSuccessListener {
                        Toast.makeText(
                            this@DocumentActivity, "Document deleted", Toast.LENGTH_SHORT
                        ).show()
                        firebaseStorage.child(document.id).delete()
                        viewModel?.fetchDocuments()
                    }.addOnFailureListener {
                        Toast.makeText(
                            this@DocumentActivity, "Failed to delete document", Toast.LENGTH_SHORT
                        ).show()
                    }
            }.show()
    }


}

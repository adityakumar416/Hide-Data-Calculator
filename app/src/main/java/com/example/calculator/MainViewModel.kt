package com.example.calculator

import android.app.Activity
import android.app.Application
import android.app.ProgressDialog
import android.content.ContentValues
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.calculator.documents.Document
import com.example.calculator.documents.DocumentActivity
import com.example.calculator.photos.ImageModel
import com.example.calculator.videos.VideoModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class MainViewModel(application: Application) : AndroidViewModel(application) {

     val uploadStatus: MutableLiveData<Boolean> by lazy {
         MutableLiveData<Boolean>()
    }
    val imageList: MutableLiveData<List<ImageModel>> by lazy {
        MutableLiveData<List<ImageModel>>()
    }

    val videoUploadStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    val videoList: MutableLiveData<List<VideoModel>> by lazy {
        MutableLiveData<List<VideoModel>>()
    }





    fun uploadVideoToFirebase(videoUri: Uri,activity: Activity) {
        // Get the Firebase Storage reference
        val storage = Firebase.storage
        val storageRef = storage.reference

        val originalFileName = getFileName(videoUri)
        // Create a reference to the video file in Firebase Storage
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        val videoRef = FirebaseStorage.getInstance().getReference(uid).child("videos/$originalFileName")


        val processDialog = ProgressDialog(activity)
        processDialog.setMessage("Video Uploading")
        processDialog.setCancelable(false)
        processDialog.show()
        // Show progress bar during upload

        // Upload the video to Firebase Storage
        videoRef.putFile(videoUri)
            .addOnSuccessListener { taskSnapshot ->

                processDialog.dismiss()
                fetchVideoItemsFromFirebase()
                videoUploadStatus.value = true

            }
            .addOnFailureListener { exception ->
                processDialog.dismiss()
                videoUploadStatus.value = false

            }
            .addOnProgressListener { taskSnapshot -> //displaying the upload progress
                val progress =
                    100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                processDialog.setMessage("Uploaded " + progress.toInt() + "%...")
            }

    }


    fun fetchVideoItemsFromFirebase() {
        val storage = Firebase.storage

        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val storageRef = FirebaseStorage.getInstance().getReference(uid).child("videos/")

        val videoItems = mutableListOf<VideoModel>()

        storageRef.listAll().addOnSuccessListener { result ->
            for (item in result.items) {
                item.downloadUrl.addOnSuccessListener { uri ->
                    val videoUrl = uri.toString()
                    val videoName = item.name

                    val videoItem = VideoModel(videoUrl, videoName)
                    videoItems.add(videoItem)

                    videoList.value = videoItems
                    // Update the adapter with the new video item
                }
            }
        }.addOnFailureListener { exception ->
            // Handle errors during fetch operation
        }

    }


    fun uploadImage(uri: Uri, activity: Activity) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        if (uri != null) {
            val originalFileName = getFileName(uri)
            val firebaseStorage =
                FirebaseStorage.getInstance().getReference(uid).child("images/$originalFileName")
            val databaseRef =
                FirebaseDatabase.getInstance().getReference(uid).child("images/")

            val storageRef = firebaseStorage.child(
                System.currentTimeMillis().toString() + "." + getFileExtension(uri)
            )

            val processDialog = ProgressDialog(activity)
            processDialog.setMessage("Photo Uploading")
            processDialog.setCancelable(false)
            processDialog.show()

            storageRef.putFile(uri)
                .addOnSuccessListener {

                    Log.i(ContentValues.TAG, "onSuccess Main: $it")
                    processDialog.dismiss()



                    val urlTask: Task<Uri> = it.storage.downloadUrl
                    while (!urlTask.isSuccessful);
                    val downloadUrl: Uri = urlTask.result
                    Log.i(ContentValues.TAG, "onSuccess: $downloadUrl")

                    val imageModel =
                        ImageModel(databaseRef.push().key, originalFileName, downloadUrl.toString())
                    val uploadId = imageModel.imageId

                    if (uploadId != null) {
                        databaseRef.child(uploadId).setValue(imageModel)
                    }
                    uploadStatus.value = true

                }

                .addOnFailureListener {
                    uploadStatus.value = false
                    processDialog.dismiss()

                }
                .addOnProgressListener { taskSnapshot -> //displaying the upload progress
                    val progress =
                        100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                    processDialog.setMessage("Uploaded " + progress.toInt() + "%...")
                }
        }

    }


    private fun getFileName(uri: Uri): String {
        var result = ""
        if (uri.scheme == "content") {
            val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        return result
    }

    private fun getFileExtension(uri: Uri): String? {
        val cR = getApplication<Application>().contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(cR.getType(uri))
    }


     fun fetchImages() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val databaseReference = FirebaseDatabase.getInstance().getReference(uid).child("images/")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val images = mutableListOf<ImageModel>()
                for (dataSnapshot in snapshot.children) {
                    val image: ImageModel? = dataSnapshot.getValue(ImageModel::class.java)
                    image?.let {
                        images.add(it)
                    }
                }
                imageList.value = images
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }



    val documents: MutableLiveData<MutableList<Document>> by lazy {
        MutableLiveData<MutableList<Document>>()
    }

    val documentsUploadStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    private val storage = Firebase.storage
    private val uid = FirebaseAuth.getInstance().currentUser!!.uid
    private val storageRef = FirebaseStorage.getInstance().getReference(uid).child("documents")

    fun fetchDocuments() {
        storageRef.listAll().addOnSuccessListener { listResult ->
            val documentList = mutableListOf<Document>()
            listResult.items.forEach { item ->
                item.downloadUrl.addOnSuccessListener { uri ->
                    val document = Document(item.name, item.name, uri, uri)
                    documentList.add(document)
                    documents.value = documentList
                }
            }
        }.addOnFailureListener { e ->
            // Handle failure to fetch documents
        }
    }

    fun uploadDocumentToFirebaseStorage(
        documentName: String,
        documentUri: Uri,
        activity: Activity
    ) {
        val fileRef = storageRef.child(documentName)
        val uploadTask = fileRef.putFile(documentUri)

        val processDialog = ProgressDialog(activity)
        processDialog.setMessage("Document Uploading")
        processDialog.setCancelable(false)
        processDialog.show()

        uploadTask.addOnSuccessListener { taskSnapshot ->
            fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val downloadUrl = downloadUri
                val documentId = fileRef.name // You can use the document name as the ID
                val document = Document(documentId, documentName, documentUri, downloadUrl)
                fetchDocuments() // Update the list of documents after upload
                documentsUploadStatus.value = true // Notify upload success
                processDialog.dismiss()
            }.addOnFailureListener {
                documentsUploadStatus.value = false // Notify upload failure
                // Handle any errors that occurred during getting download URL
                processDialog.dismiss()
            }
        }.addOnFailureListener {
            // Handle upload failures
            documentsUploadStatus.value = false // Notify upload failure
            processDialog.dismiss()
        }.addOnProgressListener { taskSnapshot ->
            // Displaying the upload progress
            val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
            processDialog.setMessage("Uploaded " + progress.toInt() + "%...")
        }
    }




}

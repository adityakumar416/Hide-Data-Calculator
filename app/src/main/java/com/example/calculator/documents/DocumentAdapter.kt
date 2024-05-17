package com.example.calculator.documents


import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.calculator.R

class DocumentAdapter(
    private val context: Context,
    private val documents: List<Document>,
    private val deleteInterface: DeleteInterface
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val documentName: TextView = itemView.findViewById(R.id.documentName)
        val documentItem: CardView = itemView.findViewById(R.id.document_item)
        val documentUrl: TextView = itemView.findViewById(R.id.documentUrl)
        val downloadPdf: LinearLayout = itemView.findViewById(R.id.downloadPdf)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }


    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documents[position]
        holder.documentName.text = document.name
        // holder.documentUrl.text = document.downloadUrl


        holder.downloadPdf.setOnClickListener {
            downloadPdf(document.downloadUrl, document.name)
        }

        holder.itemView.setOnClickListener {
            openDocument(document.uri)
        }

        holder.documentItem.setOnLongClickListener(View.OnLongClickListener {
            deleteInterface.onDeleteDocument(document)
            true
        })


    }
    private fun downloadPdf(url: Uri, fileName: String) {
        val request = DownloadManager.Request(url)
            .setTitle(fileName)
            .setDescription("Downloading PDF...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$fileName.pdf")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        Toast.makeText(context, "Downloading PDF...", Toast.LENGTH_SHORT).show()
    }

    override fun getItemCount(): Int {
        return documents.size
    }

    private fun openDocument(documentUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            documentUri, "application/pdf"
        ) // Change the MIME type if opening different types of documents
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permission to the app
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context, "No application available to open the document", Toast.LENGTH_SHORT
            ).show()
        }
    }
}

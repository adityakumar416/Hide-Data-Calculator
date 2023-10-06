package com.example.calculator.videos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.calculator.R
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView


interface VideoItemClickListener {
    fun onVideoItemClicked(videoUrl: String)
}

class VideoAdapter(private val videoItems: MutableList<VideoModel>, private val clickListener: VideoItemClickListener) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var exoPlayer: SimpleExoPlayer? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoItem = videoItems[position]
        holder.bind(videoItem)
    }

    override fun getItemCount(): Int {
        return videoItems.size
    }

    fun updateData(newVideoItems: List<VideoModel>) {
        videoItems.clear()
        videoItems.addAll(newVideoItems)
        notifyDataSetChanged()
    }


    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoThumbnailImageView: ImageView = itemView.findViewById(R.id.videoThumbnailImageView)
        private val videoNameTextView: TextView = itemView.findViewById(R.id.videoNameTextView)
        private val playerView: PlayerView = itemView.findViewById(R.id.playerView)

        fun bind(videoItem: VideoModel) {
            // Load video thumbnail using Glide
            Glide.with(itemView.context)
                .load(videoItem.videoUrl)
                .into(videoThumbnailImageView)

            videoNameTextView.text = videoItem.videoName
            itemView.setOnClickListener {
                clickListener.onVideoItemClicked(videoItem.videoUrl)
            }

           /* val player = SimpleExoPlayer.Builder(itemView.context).build()
            playerView.player = player

            val mediaItem = MediaItem.fromUri(videoItem.videoUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true*/

        }
    }
}

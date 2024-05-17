package com.educate.theteachingapp.videoPlayer.utils

import android.net.Uri

class PlaylistManager {

    private val queue = mutableListOf<Uri>()
    private var currentItem: Uri? = null


    fun hasNext(): Boolean {
        return currentIndex() + 1 < size()
    }

    fun size() = queue.size

    fun currentIndex(): Int = queue.indexOfFirst { it == currentItem }.takeIf { it >= 0 } ?: 0

    fun isNotEmpty() = queue.isNotEmpty()

    fun isEmpty() = queue.isEmpty()

    fun getCurrent(): Uri? = currentItem

    override fun toString(): String = buildString {
        append("########## playlist ##########\n")
        queue.forEach { append(it.toString() + "\n") }
        append("##############################")
    }
}

package com.educate.theteachingapp.videoPlayer.extensions

import android.content.Context
import android.widget.ImageButton
import androidx.core.content.ContextCompat

fun ImageButton.setImageDrawable(context: Context, id: Int) {
    setImageDrawable(ContextCompat.getDrawable(context, id))
}
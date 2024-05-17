package com.example.calculator.photos

import android.content.Context
import android.graphics.PorterDuff
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.example.calculator.R

object DrawableHelper {

    fun circularProgressDrawable(context:Context): CircularProgressDrawable {

        val circularProgressDrawable = CircularProgressDrawable(context)
        circularProgressDrawable.strokeWidth = 10f
        circularProgressDrawable.centerRadius = 60f
        circularProgressDrawable.setColorFilter(
            ContextCompat.getColor(
                context,
                R.color.btnBackground2
            ), PorterDuff.Mode.SRC_IN
        )
        circularProgressDrawable.start()
        return circularProgressDrawable
    }
}
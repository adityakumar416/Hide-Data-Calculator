package com.educate.theteachingapp.videoPlayer.extensions

import com.educate.theteachingapp.videoPlayer.enums.FastSeek
import com.educate.theteachingapp.videoPlayer.model.PlayerPreferences

fun PlayerPreferences.shouldFastSeek(duration: Long): Boolean {
    return when (fastSeek) {
        FastSeek.ENABLE -> true
        FastSeek.DISABLE -> false
        FastSeek.AUTO -> duration >= minDurationForFastSeek
    }
}

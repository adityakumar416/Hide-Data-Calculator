package com.educate.theteachingapp.videoPlayer.model

import com.educate.theteachingapp.videoPlayer.enums.FastSeek

data class PlayerPreferences(
    val fastSeek: FastSeek = FastSeek.AUTO,
    val minDurationForFastSeek: Long = 120000L,
    )

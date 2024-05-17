package com.educate.theteachingapp.videoPlayer.utils

import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer

class VolumeManager(private val audioManager: AudioManager) {

    val currentStreamVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    val maxStreamVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    var currentVolume: Float
        private set

    var loudnessEnhancer: LoudnessEnhancer? = null
        set(value) {
            if (currentVolume > maxStreamVolume) {
                try {
                    value?.enabled = true
                    value?.setTargetGain(currentLoudnessGain.toInt())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            field = value
        }

    init {
        currentVolume = currentStreamVolume.toFloat()
    }

    val maxVolume: Float
        get() = maxStreamVolume.toFloat() * (loudnessEnhancer?.let { 2 } ?: 1)

    val currentLoudnessGain: Float
        get() = (currentVolume - maxStreamVolume) * (MAX_VOLUME_BOOST / maxStreamVolume)

    val volumePercentage: Int
        get() = (currentVolume / maxStreamVolume.toFloat() * 100).toInt()

    fun setVolume(volume: Float, showVolumePanel: Boolean = false) {
        currentVolume = volume.coerceIn(0f, maxVolume)

        if (currentVolume <= maxStreamVolume) {
            loudnessEnhancer?.enabled = false
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                currentVolume.toInt(),
                if (showVolumePanel && audioManager.isWiredHeadsetOn) AudioManager.FLAG_SHOW_UI else 0
            )
        } else {
            try {
                loudnessEnhancer?.enabled = true
                loudnessEnhancer?.setTargetGain(currentLoudnessGain.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun adjustVolumeBySteps(steps: Int, showVolumePanel: Boolean = false) {
        val newVolume = currentVolume + steps
        setVolume(newVolume, showVolumePanel)
    }

    fun increaseVolume(showVolumePanel: Boolean = false) {
        setVolume(currentVolume + 1, showVolumePanel)
    }

    fun decreaseVolume(showVolumePanel: Boolean = false) {
        setVolume(currentVolume - 1, showVolumePanel)
    }

    companion object {
        const val MAX_VOLUME_BOOST = 2000
    }
}
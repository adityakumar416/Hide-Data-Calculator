package com.example.calculator.videoPlayer.utils

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.calculator.videoPlayer.ExoPlayerActivity
import com.educate.theteachingapp.videoPlayer.enums.DoubleTapGesture
import com.educate.theteachingapp.videoPlayer.model.PlayerPreferences
import com.educate.theteachingapp.videoPlayer.extensions.dpToPx
import com.educate.theteachingapp.videoPlayer.extensions.shouldFastSeek
import com.example.calculator.videoPlayer.extensions.seekBack
import com.example.calculator.videoPlayer.extensions.seekForward
import com.educate.theteachingapp.videoPlayer.extensions.togglePlayPause
import com.educate.theteachingapp.videoPlayer.utils.BrightnessManager
import com.educate.theteachingapp.videoPlayer.utils.Utils
import com.educate.theteachingapp.videoPlayer.utils.VolumeManager
import com.example.calculator.R
import kotlin.math.abs

@UnstableApi
@SuppressLint("ClickableViewAccessibility")
class PlayerGestureHelper(
    private val activity: ExoPlayerActivity,
    private val volumeManager: VolumeManager,
    private val brightnessManager: BrightnessManager,
    private val playerPreferences: PlayerPreferences
) {

    private val playerView: PlayerView
        get() = activity.binding.playerView

    private var exoContentFrameLayout: AspectRatioFrameLayout =
        playerView.findViewById(R.id.exo_content_frame)

    private var currentGestureAction: GestureAction? = null
    private var seekStart = 0L
    private var position = 0L
    private var seekChange = 0L
    private var pointerCount = 1
    private var isPlayingOnSeekStart: Boolean = false
    private var currentPlaybackSpeed: Float? = null
    val longPressControlsSpeed: Float = 2.0f
    val doubleTapGesture: DoubleTapGesture = DoubleTapGesture.BOTH
    val seekIncrement: Int = 10


    private val tapGestureDetector =
        GestureDetector(playerView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                with(playerView) {
                    if (!isControllerFullyVisible) showController() else hideController()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (activity.isControlsLocked) return

                if (playerView.player?.isPlaying == false) return
                if (currentGestureAction == null) {
                    currentGestureAction = GestureAction.FAST_PLAYBACK
                    currentPlaybackSpeed = playerView.player?.playbackParameters?.speed
                }
                if (currentGestureAction != GestureAction.FAST_PLAYBACK) return
                if (pointerCount >= 3) return

                // Check if zoom gesture is in progress
                if (zoomGestureDetector.isInProgress) return

                playerView.hideController()
                activity.showTopInfo(
                    activity.getString(
                        R.string.fast_playback_speed, longPressControlsSpeed
                    )
                )
                playerView.player?.setPlaybackSpeed(longPressControlsSpeed)
            }

            override fun onDoubleTap(event: MotionEvent): Boolean {
                if (activity.isControlsLocked) return false

                playerView.player?.run {
                    when (doubleTapGesture) {
                        DoubleTapGesture.FAST_FORWARD_AND_REWIND -> {
                            val viewCenterX = playerView.measuredWidth / 2

                            if (event.x.toInt() < viewCenterX) {
                                val newPosition = currentPosition - seekIncrement.toMillis
                                seekBack(
                                    newPosition.coerceAtLeast(0),
                                    playerPreferences.shouldFastSeek(duration)
                                )
                            } else {
                                val newPosition = currentPosition + seekIncrement.toMillis
                                seekForward(
                                    newPosition.coerceAtMost(duration),
                                    playerPreferences.shouldFastSeek(duration)
                                )
                            }
                        }

                        DoubleTapGesture.BOTH -> {
                            val eventPositionX = event.x / playerView.measuredWidth

                            if (eventPositionX < 0.35) {
                                val newPosition = currentPosition - seekIncrement.toMillis
                                seekBack(newPosition.coerceAtLeast(0), playerPreferences.shouldFastSeek(duration))
                            } else if (eventPositionX > 0.65) {
                                val newPosition = currentPosition + seekIncrement.toMillis
                                seekForward(newPosition.coerceAtMost(duration), playerPreferences.shouldFastSeek(duration))
                            } else {
                                playerView.togglePlayPause()
                            }
                        }

                        DoubleTapGesture.PLAY_PAUSE -> playerView.togglePlayPause()

                        DoubleTapGesture.NONE -> return false
                    }
                } ?: return false
                return true
            }

        })

    private val seekGestureDetector =
        GestureDetector(playerView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                firstEvent: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (inExclusionArea(firstEvent)) return false
                if (activity.isControlsLocked) return false
                if (!activity.isFileLoaded) return false
                if (abs(distanceX / distanceY) < 2) return false

                if (currentGestureAction == null) {
                    seekChange = 0L
                    seekStart = playerView.player?.currentPosition ?: 0L
                    playerView.controllerAutoShow = playerView.isControllerFullyVisible
                    if (playerView.player?.isPlaying == true) {
                        playerView.player?.pause()
                        isPlayingOnSeekStart = true
                    }
                    currentGestureAction = GestureAction.SEEK
                }
                if (currentGestureAction != GestureAction.SEEK) return false

                val distanceDiff = abs(Utils.pxToDp(distanceX) / 4).coerceIn(0.5f, 10f)
                val change = (distanceDiff * SEEK_STEP_MS).toLong()
                playerView.player?.seekTo(seekStart + change)

                return false
            }
        })

    private val volumeAndBrightnessGestureDetector =
        GestureDetector(playerView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                firstEvent: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (inExclusionArea(firstEvent)) return false
                if (activity.isControlsLocked) return false
                if (abs(distanceY / distanceX) < 2) return false

                if (currentGestureAction == null) {
                    currentGestureAction = GestureAction.SWIPE
                }
                if (currentGestureAction != GestureAction.SWIPE) return false

                val viewCenterX = playerView.measuredWidth / 2
                val distanceFull = playerView.measuredHeight * FULL_SWIPE_RANGE_SCREEN_RATIO
                val ratioChange = distanceY / distanceFull

                if (firstEvent.x.toInt() > viewCenterX) {
                    val change = ratioChange * volumeManager.maxStreamVolume
                    volumeManager.setVolume(volumeManager.currentVolume + change)
                    activity.showVolumeGestureLayout()
                } else {
                    val change = ratioChange * brightnessManager.maxBrightness
                    brightnessManager.setBrightness(brightnessManager.currentBrightness + change)
                    activity.showBrightnessGestureLayout()
                }
                return true
            }
        })


    private val zoomGestureDetector = ScaleGestureDetector(playerView.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private val MIN_SCALE_FACTOR = 0.25f
            private val MAX_SCALE_FACTOR = 4.0f

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (activity.isControlsLocked) return false

                val scaleFactor = detector.scaleFactor
                val currentScaleX = exoContentFrameLayout.scaleX
                val currentScaleY = exoContentFrameLayout.scaleY

                val newScaleX = currentScaleX * scaleFactor
                val newScaleY = currentScaleY * scaleFactor

                // Limit the scale factor within the defined range
                val clampedScaleX = newScaleX.coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)
                val clampedScaleY = newScaleY.coerceIn(MIN_SCALE_FACTOR, MAX_SCALE_FACTOR)

                // Apply the new scale factors
                exoContentFrameLayout.scaleX = clampedScaleX
                exoContentFrameLayout.scaleY = clampedScaleY

                // Show the zoom level information
                val zoomPercentage = (clampedScaleX * 100).toInt()
                activity.showPlayerInfo("$zoomPercentage%")

                return true
            }
        })

    private fun releaseGestures() {
        activity.hideVolumeGestureLayout()
        activity.hideBrightnessGestureLayout()
        activity.hidePlayerInfo(0L)
        activity.hideTopInfo()

        currentPlaybackSpeed?.let {
            playerView.player?.setPlaybackSpeed(it)
            currentPlaybackSpeed = null
        }

        playerView.controllerAutoShow = true
        if (isPlayingOnSeekStart) playerView.player?.play()
        isPlayingOnSeekStart = false
        currentGestureAction = null
    }

    private fun inExclusionArea(firstEvent: MotionEvent): Boolean {
        val gestureExclusionBorder = playerView.context.dpToPx(GESTURE_EXCLUSION_AREA)

        return firstEvent.y < gestureExclusionBorder || firstEvent.y > playerView.height - gestureExclusionBorder || firstEvent.x < gestureExclusionBorder || firstEvent.x > playerView.width - gestureExclusionBorder
    }

    init {
        playerView.setOnTouchListener { _, motionEvent ->
            pointerCount = motionEvent.pointerCount
            when (motionEvent.pointerCount) {
                1 -> {
                    tapGestureDetector.onTouchEvent(motionEvent)
                    volumeAndBrightnessGestureDetector.onTouchEvent(motionEvent)
                    seekGestureDetector.onTouchEvent(motionEvent)
                }

                2 -> {
                    zoomGestureDetector.onTouchEvent(motionEvent)
                }
            }

            if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.pointerCount >= 3) {
                releaseGestures()
            }
            true
        }
    }

    companion object {
        const val FULL_SWIPE_RANGE_SCREEN_RATIO = 0.66f
        const val GESTURE_EXCLUSION_AREA = 20f
        const val SEEK_STEP_MS = 1000L
    }
}

inline val Int.toMillis get() = this * 1000

enum class GestureAction {
    SWIPE, SEEK, ZOOM, FAST_PLAYBACK
}

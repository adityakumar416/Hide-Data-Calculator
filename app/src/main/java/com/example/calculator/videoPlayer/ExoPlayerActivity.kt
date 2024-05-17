package com.example.calculator.videoPlayer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import com.educate.theteachingapp.videoPlayer.dialogs.PlaybackSpeedControlsDialogFragment
import com.example.calculator.videoPlayer.dialogs.TrackSelectionDialogFragment
import com.example.calculator.videoPlayer.dialogs.VideoZoomOptionsDialogFragment
import com.example.calculator.videoPlayer.dialogs.nameRes
import com.educate.theteachingapp.videoPlayer.enums.VideoZoom
import com.educate.theteachingapp.videoPlayer.extensions.getFilenameFromUri
import com.educate.theteachingapp.videoPlayer.extensions.next
import com.example.calculator.videoPlayer.extensions.seekBack
import com.example.calculator.videoPlayer.extensions.seekForward
import com.educate.theteachingapp.videoPlayer.extensions.setImageDrawable
import com.educate.theteachingapp.videoPlayer.extensions.shouldFastSeek
import com.example.calculator.videoPlayer.extensions.switchTrack
import com.educate.theteachingapp.videoPlayer.extensions.toggleSystemBars
import com.educate.theteachingapp.videoPlayer.model.PlayerPreferences
import com.educate.theteachingapp.videoPlayer.utils.BrightnessManager
import com.example.calculator.videoPlayer.utils.PlayerGestureHelper
import com.educate.theteachingapp.videoPlayer.utils.PlaylistManager
import com.educate.theteachingapp.videoPlayer.utils.Utils
import com.educate.theteachingapp.videoPlayer.utils.VolumeManager
import com.example.calculator.videoPlayer.utils.toMillis
import com.example.calculator.R
import com.example.calculator.databinding.ActivityExoPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@UnstableApi
class ExoPlayerActivity : AppCompatActivity() {
    lateinit var binding: ActivityExoPlayerBinding
    private lateinit var playerView: PlayerView
    private var textureView: TextureView? = null
    private var playWhenReady = true
    private lateinit var player: Player

    private lateinit var audioTrackButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var exoContentFrameLayout: AspectRatioFrameLayout
    private lateinit var lockControlsButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var playbackSpeedButton: ImageButton
    private lateinit var playerLockControls: FrameLayout
    private lateinit var playerUnlockControls: FrameLayout
    private lateinit var playerCenterControls: LinearLayout
    private lateinit var prevButton: ImageButton
    private lateinit var screenRotateButton: ImageButton
    private lateinit var seekBar: TimeBar

    // private lateinit var subtitleTrackButton: ImageButton
    private lateinit var unlockControlsButton: ImageButton
    private lateinit var videoTitleTextView: TextView
    private lateinit var videoZoomButton: ImageButton
    private lateinit var videoFlipButton: CardView

    private lateinit var playerPreferences: PlayerPreferences
    private lateinit var playerGestureHelper: PlayerGestureHelper
    private lateinit var playlistManager: PlaylistManager
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var volumeManager: VolumeManager
    private lateinit var brightnessManager: BrightnessManager

    private var currentZoomMode = VideoZoom.BEST_FIT
    var isFileLoaded = false
    var isControlsLocked = false
    private var isFrameRendered = false
    private var isPlayingOnScrubStart: Boolean = false
    private var previousScrubPosition = 0L
    private var scrubStartPosition: Long = -1L
    private var currentVideoSize: VideoSize? = null
    private var hideVolumeIndicatorJob: Job? = null
    private var hideBrightnessIndicatorJob: Job? = null
    private var hideInfoLayoutJob: Job? = null
    val seekIncrement: Int = 10
    private var isVideoFlipped = false

    private val shouldFastSeek: Boolean
        get() = playerPreferences.shouldFastSeek(player.duration)

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityExoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeView()

        playerPreferences = PlayerPreferences()
        playlistManager = PlaylistManager()
        trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()

        playerView = binding.playerView
        playerView.player = player


        seekBar.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                if (player.isPlaying) {
                    isPlayingOnScrubStart = true
                    player.pause()
                }
                isFrameRendered = true
                scrubStartPosition = player.currentPosition
                previousScrubPosition = player.currentPosition
                scrub(position)
                showPlayerInfo(
                    info = Utils.formatDurationMillis(position),
                    subInfo = "[${Utils.formatDurationMillisSign(position - scrubStartPosition)}]"
                )
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                scrub(position)
                showPlayerInfo(
                    info = Utils.formatDurationMillis(position),
                    subInfo = "[${Utils.formatDurationMillisSign(position - scrubStartPosition)}]"
                )
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                hidePlayerInfo(0L)
                scrubStartPosition = -1L
                if (isPlayingOnScrubStart) {
                    player.play()
                }
            }
        })

        volumeManager =
            VolumeManager(audioManager = getSystemService(AUDIO_SERVICE) as AudioManager)
        brightnessManager = BrightnessManager(activity = this)
        playerGestureHelper = PlayerGestureHelper(
            activity = this,
            volumeManager = volumeManager,
            brightnessManager = brightnessManager,
            playerPreferences = playerPreferences
        )

    }


    private fun initializeView() {
        audioTrackButton = binding.playerView.findViewById(R.id.btn_audio_track)
        backButton = binding.playerView.findViewById(R.id.back_button)
        exoContentFrameLayout = binding.playerView.findViewById(R.id.exo_content_frame)
        lockControlsButton = binding.playerView.findViewById(R.id.btn_lock_controls)
        nextButton = binding.playerView.findViewById(R.id.btn_play_next)
        prevButton = binding.playerView.findViewById(R.id.btn_play_prev)
        playbackSpeedButton = binding.playerView.findViewById(R.id.btn_playback_speed)
        playerLockControls = binding.playerView.findViewById(R.id.player_lock_controls)
        playerUnlockControls = binding.playerView.findViewById(R.id.player_unlock_controls)
        playerCenterControls = binding.playerView.findViewById(R.id.player_center_controls)
        screenRotateButton = binding.playerView.findViewById(R.id.screen_rotate)
        seekBar = binding.playerView.findViewById(R.id.exo_progress)
        //  subtitleTrackButton = binding.playerView.findViewById(R.id.btn_subtitle_track)
        unlockControlsButton = binding.playerView.findViewById(R.id.btn_unlock_controls)

        videoTitleTextView = binding.playerView.findViewById(R.id.video_name)
        videoZoomButton = binding.playerView.findViewById(R.id.btn_video_zoom)
        videoTitleTextView = binding.playerView.findViewById(R.id.video_name)
        videoFlipButton = binding.playerView.findViewById(R.id.btn_flip)
    }

    override fun onStart() {
        super.onStart()

        if (player.playbackState == Player.STATE_READY) {
            player.play()
            return
        }


        var videoUrl = intent.getStringExtra("VIDEO_URL")

        if (videoUrl != null) {
            initializePlayerView()
            playVideo(Uri.parse(videoUrl))
        } else {
            showToast("No video to play")
            finish()
        }

    }


    private fun playVideo(uri: Uri) = lifecycleScope.launch(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                textureView = TextureView(this@ExoPlayerActivity).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                player.setVideoTextureView(textureView)
                exoContentFrameLayout.addView(textureView, 0)

                videoTitleTextView.text = getFilenameFromUri(uri)

                val mediaStream = MediaItem.Builder().setUri(uri).setMediaId(uri.toString()).build()

                player.setMediaItem(mediaStream)
                player.playWhenReady = playWhenReady
                player.prepare()
            }
        } catch (e: Exception) {
            // Check if the exception is due to a network connection error
            if (e is IOException) {
                // Network connection error
                showToast("Network connection error. Please check your internet connection and try again.")
            } else {
                // Other errors
                showToast("Error playing video: ${e.message}")
            }
            Log.e("PlayerActivity", "Error playing video", e)
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@ExoPlayerActivity, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        player.pause()
    }


    fun showPlayerInfo(info: String, subInfo: String? = null) {
        hideInfoLayoutJob?.cancel()
        with(binding) {
            infoLayout.visibility = View.VISIBLE
            infoText.text = info
            infoSubtext.visibility = View.GONE.takeIf { subInfo == null } ?: View.VISIBLE
            infoSubtext.text = subInfo
        }
    }

    fun hidePlayerInfo(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.infoLayout.visibility != View.VISIBLE) return
        hideInfoLayoutJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.infoLayout.visibility = View.GONE
        }
    }


    @OptIn(UnstableApi::class)
    private fun initializePlayerView() {
        binding.playerView.apply {
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            player = this@ExoPlayerActivity.player
            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                toggleSystemBars(showBars = visibility == View.VISIBLE && !isControlsLocked)
            })
        }

        audioTrackButton.setOnClickListener {

            trackSelector.currentMappedTrackInfo ?: return@setOnClickListener

            TrackSelectionDialogFragment(type = C.TRACK_TYPE_AUDIO,
                tracks = player.currentTracks,
                onTrackSelected = { trackIndex ->
                    player.switchTrack(C.TRACK_TYPE_AUDIO, trackIndex)
                }).show(supportFragmentManager, "AudioTrackSelectionDialog")

        }

        /*      subtitleTrackButton.setOnClickListener {
                  trackSelector.currentMappedTrackInfo ?: return@setOnClickListener
                  TrackSelectionDialogFragment(type = C.TRACK_TYPE_TEXT,
                      tracks = player.currentTracks,
                      onTrackSelected = { trackIndex ->
                          player.switchTrack(C.TRACK_TYPE_TEXT, trackIndex)
                      }).show(supportFragmentManager, "SubtitleTrackSelectionDialog")

              }*/

        nextButton.setOnClickListener {
            val newPosition = player.currentPosition + seekIncrement.toMillis
            player.seekForward(
                newPosition.coerceAtMost(player.duration),
                playerPreferences.shouldFastSeek(player.duration)
            )
        }

        prevButton.setOnClickListener {
            val newPosition = player.currentPosition - seekIncrement.toMillis
            player.seekBack(
                newPosition.coerceAtLeast(0), playerPreferences.shouldFastSeek(player.duration)
            )
        }


        playbackSpeedButton.setOnClickListener {
            PlaybackSpeedControlsDialogFragment(currentSpeed = player.playbackParameters.speed,
                onChange = { player.setPlaybackSpeed(it) }).show(
                supportFragmentManager, "PlaybackSpeedSelectionDialog"
            )
        }

        screenRotateButton.setOnClickListener {
            requestedOrientation = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        lockControlsButton.setOnClickListener {
            playerUnlockControls.visibility = View.INVISIBLE
            playerLockControls.visibility = View.VISIBLE
            isControlsLocked = true
            toggleSystemBars(showBars = false)
        }

        unlockControlsButton.setOnClickListener {
            playerLockControls.visibility = View.INVISIBLE
            playerUnlockControls.visibility = View.VISIBLE
            isControlsLocked = false
            binding.playerView.showController()
            toggleSystemBars(showBars = true)
        }

        videoZoomButton.setOnClickListener {
            applyVideoZoom(showInfo = true)
        }

        videoZoomButton.setOnLongClickListener {
            VideoZoomOptionsDialogFragment(currentVideoZoom = currentZoomMode,
                onVideoZoomOptionSelected = { applyVideoZoom(showInfo = true) }).show(
                supportFragmentManager, "VideoZoomOptionsDialog"
            )
            true
        }

        videoFlipButton.setOnClickListener {
            textureView?.scaleX = if (isVideoFlipped) 1f else -1f
            isVideoFlipped = !isVideoFlipped
        }

        backButton.setOnClickListener { finish() }
    }


    @OptIn(UnstableApi::class)
    private fun applyVideoZoom(showInfo: Boolean) {
        // Increment the zoom mode to the next one
        currentZoomMode = currentZoomMode.next()
        resetExoContentFrameWidthAndHeight()

        when (currentZoomMode) {
            VideoZoom.BEST_FIT -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                videoZoomButton.setImageDrawable(this, R.drawable.ic_fit_screen)
            }

            VideoZoom.STRETCH -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                videoZoomButton.setImageDrawable(this, R.drawable.ic_aspect_ratio)
            }

            VideoZoom.CROP -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                videoZoomButton.setImageDrawable(this, R.drawable.ic_crop_landscape)
            }

            VideoZoom.HUNDRED_PERCENT -> {
                currentVideoSize?.let {
                    exoContentFrameLayout.layoutParams.width = it.width
                    exoContentFrameLayout.layoutParams.height = it.height
                    exoContentFrameLayout.requestLayout()
                }
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                videoZoomButton.setImageDrawable(this, R.drawable.ic_width_wide)
            }
        }

        if (showInfo) {
            lifecycleScope.launch {
                binding.infoLayout.visibility = View.VISIBLE
                binding.infoText.text = getString(currentZoomMode.nameRes())
                delay(HIDE_DELAY_MILLIS)
                binding.infoLayout.visibility = View.GONE
            }
        }
    }

    private fun resetExoContentFrameWidthAndHeight() {
        exoContentFrameLayout.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        exoContentFrameLayout.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        exoContentFrameLayout.scaleX = 1.0f
        exoContentFrameLayout.scaleY = 1.0f
        exoContentFrameLayout.requestLayout()
    }

    fun showVolumeGestureLayout() {
        hideVolumeIndicatorJob?.cancel()
        with(binding) {
            volumeGestureLayout.visibility = View.VISIBLE
            volumeProgressBar.max = volumeManager.maxVolume.times(100).toInt()
            volumeProgressBar.progress = volumeManager.currentVolume.times(100).toInt()
            volumeProgressText.text = volumeManager.volumePercentage.toString()
        }
    }

    fun showBrightnessGestureLayout() {
        hideBrightnessIndicatorJob?.cancel()
        with(binding) {
            brightnessGestureLayout.visibility = View.VISIBLE
            brightnessProgressBar.max = brightnessManager.maxBrightness.times(100).toInt()
            brightnessProgressBar.progress = brightnessManager.currentBrightness.times(100).toInt()
            brightnessProgressText.text = brightnessManager.brightnessPercentage.toString()
        }
    }

    fun hideVolumeGestureLayout(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.volumeGestureLayout.visibility != View.VISIBLE) return
        hideVolumeIndicatorJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.volumeGestureLayout.visibility = View.GONE
        }
    }

    fun hideBrightnessGestureLayout(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
        if (binding.brightnessGestureLayout.visibility != View.VISIBLE) return
        hideBrightnessIndicatorJob = lifecycleScope.launch {
            delay(delayTimeMillis)
            binding.brightnessGestureLayout.visibility = View.GONE
        }
    }


    fun hideTopInfo() {
        binding.topInfoLayout.visibility = View.GONE
    }

    fun showTopInfo(info: String) {
        with(binding) {
            topInfoLayout.visibility = View.VISIBLE
            topInfoText.text = info
        }
    }

    @OptIn(UnstableApi::class)
    private fun scrub(position: Long) {
        if (isFrameRendered) {
            isFrameRendered = false
            if (position > previousScrubPosition) {
                player.seekForward(position, shouldFastSeek)
            } else {
                player.seekBack(position, shouldFastSeek)
            }
            previousScrubPosition = position
        }
    }

    companion object {
        const val HIDE_DELAY_MILLIS = 1000L
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

}

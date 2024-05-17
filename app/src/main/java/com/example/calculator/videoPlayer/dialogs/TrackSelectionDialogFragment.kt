package com.example.calculator.videoPlayer.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.educate.theteachingapp.videoPlayer.extensions.getName
import com.example.calculator.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@UnstableApi
class TrackSelectionDialogFragment(
    private val type: Int,
    private val tracks: Tracks,
    private val onTrackSelected: (trackIndex: Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activityContext = activity ?: throw IllegalStateException("Activity cannot be null")

        return when (type) {
            C.TRACK_TYPE_AUDIO -> createAudioTrackSelectionDialog(activityContext)
            C.TRACK_TYPE_TEXT -> createSubtitleTrackSelectionDialog(activityContext)
            else -> throw IllegalArgumentException(
                "Track type not supported. Track type must be either TRACK_TYPE_AUDIO or TRACK_TYPE_TEXT"
            )
        }
    }

    private fun createAudioTrackSelectionDialog(activityContext: Context): Dialog {
        val audioTracks = tracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }

        val trackNames = audioTracks.mapIndexed { index, trackGroup ->
            trackGroup.mediaTrackGroup.getName(type, index)
        }.toTypedArray()

        val selectedTrackIndex =
            audioTracks.indexOfFirst { it.isSelected }.takeIf { it != -1 } ?: audioTracks.size

        return MaterialAlertDialogBuilder(activityContext).apply {
            setTitle(activityContext.getString(R.string.select_audio_track))
            if (trackNames.isNotEmpty()) {
                setSingleChoiceItems(
                    arrayOf(*trackNames, activityContext.getString(R.string.disable)),
                    selectedTrackIndex
                ) { dialog, trackIndex ->
                    onTrackSelected(trackIndex.takeIf { it < trackNames.size } ?: -1)
                    dialog.dismiss()
                }
            } else {
                setMessage(activityContext.getString(R.string.no_audio_tracks_found))
            }
        }.create()
    }

    private fun createSubtitleTrackSelectionDialog(activityContext: Context): Dialog {
        val subtitleTracks = tracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }

        val trackNames = subtitleTracks.mapIndexed { index, trackGroup ->
            trackGroup.mediaTrackGroup.getName(type, index)
        }.toTypedArray()

        val selectedTrackIndex =
            subtitleTracks.indexOfFirst { it.isSelected }.takeIf { it != -1 } ?: subtitleTracks.size

        return MaterialAlertDialogBuilder(activityContext).apply {
            setTitle(activityContext.getString(R.string.select_subtitle_track))
            if (trackNames.isNotEmpty()) {
                setSingleChoiceItems(
                    arrayOf(*trackNames, activityContext.getString(R.string.disable)),
                    selectedTrackIndex
                ) { dialog, trackIndex ->
                    onTrackSelected(trackIndex.takeIf { it < trackNames.size } ?: -1)
                    dialog.dismiss()
                }
            } else {
                setMessage(activityContext.getString(R.string.no_subtitle_tracks_found))
            }
            setPositiveButton(activityContext.getString(R.string.close)) { dialog, _ ->
                dialog.dismiss()
                // Perform any action needed on selecting subtitle track
            }
        }.create()
    }
}

package com.deepak.flow.core.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.deepak.flow.R

/**
 * Bundled rest-complete chime for foreground alerts.
 *
 * Uses [AudioAttributes.USAGE_NOTIFICATION_EVENT] so Android picks the active notification
 * output route (speaker, wired headset, or Bluetooth) without privileged routing.
 */
object GymRestCompleteSound {
    fun soundUri(context: Context): Uri =
        Uri.parse(soundResourceUriString(context.packageName, R.raw.gym_rest_complete))

    internal fun soundResourceUriString(packageName: String, rawResId: Int): String =
        "android.resource://$packageName/$rawResId"

    fun playForeground(context: Context) {
        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            player.setDataSource(context.applicationContext, soundUri(context))
            player.setOnPreparedListener {
                it.start()
            }
            player.setOnCompletionListener { mediaPlayer ->
                mediaPlayer.release()
            }
            player.setOnErrorListener { mediaPlayer, _, _ ->
                mediaPlayer.release()
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            // Silent devices, missing resource, or audio focus denied.
        }
    }
}

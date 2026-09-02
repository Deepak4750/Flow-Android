package com.deepak.flow.core.gym

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Rest-complete: PEP PEP PEEEP PEEEP repeated twice (~4-5s total).
 * Fires once per natural rest completion; never throws.
 */
fun vibrateRestComplete(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java) ?: return
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return

        val segments = GymRestVibrationPattern.vibratorSegments()
        val amplitude = GymRestVibrationPattern.AMPLITUDE

        val timings = LongArray(segments.size + 1)
        val amplitudes = IntArray(segments.size + 1)
        timings[0] = 0
        amplitudes[0] = 0
        var index = 1
        var onPulse = true
        for (duration in segments) {
            timings[index] = duration
            amplitudes[index] = if (onPulse) amplitude else 0
            onPulse = !onPulse
            index++
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    } catch (_: Exception) {
        // Device/settings may deny vibration.
    }
}

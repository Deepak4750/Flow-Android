package com.deepak.flow.core.gym

/**
 * Shared PEP-PEP-PEEEP rest-complete pattern for direct [android.os.Vibrator] use and
 * notification channels.
 */
object GymRestVibrationPattern {
    private const val SHORT_ON = 150L
    private const val LONG_ON = 480L
    private const val PAUSE = 180L
    const val AMPLITUDE = 120

    private val cycleSegments = listOf(
        SHORT_ON, PAUSE,
        SHORT_ON, PAUSE,
        LONG_ON, PAUSE,
        LONG_ON, PAUSE,
    )

    fun vibratorSegments(): List<Long> = cycleSegments + cycleSegments

    /**
     * Off/on/off/on pattern for [android.app.NotificationChannel.setVibrationPattern].
     * Leading zero is the delay before the first pulse.
     */
    fun notificationChannelPattern(): LongArray {
        val segments = vibratorSegments()
        val pattern = LongArray(segments.size + 1)
        pattern[0] = 0L
        segments.forEachIndexed { index, duration ->
            pattern[index + 1] = duration
        }
        return pattern
    }
}

package com.deepak.flow.core.gym

import java.util.Locale
import java.util.regex.Pattern

object GymLogic {
    private val urlPattern: Pattern = Pattern.compile(
        """(?i)\b((?:https?://|www\.)[^\s<>\[\](){}"']+)""",
    )

    fun encodeTrackingFields(fields: Set<TrackingField>): String =
        fields.sortedBy { it.ordinal }.joinToString(",") { it.name }

    fun decodeTrackingFields(raw: String?): Set<TrackingField> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(',')
            .mapNotNull { token ->
                runCatching { TrackingField.valueOf(token.trim()) }.getOrNull()
            }
            .toSet()
    }

    fun hasMeaningfulMeasurement(
        fields: Set<TrackingField>,
        measurements: GymSetMeasurements,
    ): Boolean {
        if (fields.isEmpty()) return false
        return fields.any { field ->
            when (field) {
                TrackingField.WEIGHT -> (measurements.weight ?: 0.0) > 0.0
                TrackingField.REPS -> (measurements.reps ?: 0) > 0
                TrackingField.DURATION -> (measurements.durationSeconds ?: 0) > 0
                TrackingField.DISTANCE -> (measurements.distance ?: 0.0) > 0.0
                TrackingField.SPEED -> (measurements.speed ?: 0.0) > 0.0
                TrackingField.INCLINE -> measurements.incline != null
                TrackingField.RESISTANCE -> (measurements.resistance ?: 0.0) > 0.0
                TrackingField.ROUNDS -> (measurements.rounds ?: 0) > 0
            }
        }
    }

    fun elapsedSeconds(startedAtEpochMilli: Long, nowEpochMilli: Long): Long =
        ((nowEpochMilli - startedAtEpochMilli).coerceAtLeast(0L) / 1000L)

    fun formatStopwatch(startedAtEpochMilli: Long, nowEpochMilli: Long): String {
        val total = elapsedSeconds(startedAtEpochMilli, nowEpochMilli)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatCountdown(remainingSeconds: Int): String {
        val safe = remainingSeconds.coerceAtLeast(0)
        val minutes = safe / 60
        val seconds = safe % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun remainingRestSeconds(restEndsAtEpochMilli: Long?, nowEpochMilli: Long): Int {
        if (restEndsAtEpochMilli == null) return 0
        return ((restEndsAtEpochMilli - nowEpochMilli) / 1000L).toInt().coerceAtLeast(0)
    }

    /**
     * Volume only when weight and reps are both present and positive.
     * Duration-only (and similar) sets contribute nothing.
     * Failed sets still count: the weight was lifted.
     */
    fun setVolumeKg(set: GymWorkoutSet, sessionUnit: WeightUnit): Double? {
        if (!set.saved) return null
        val weight = set.measurements.weight?.takeIf { it > 0.0 } ?: return null
        val reps = set.measurements.reps?.takeIf { it > 0 } ?: return null
        val unit = set.measurements.weightUnit ?: sessionUnit
        val kg = when (unit) {
            WeightUnit.KG -> weight
            WeightUnit.LB -> weight * LB_TO_KG
        }
        return kg * reps
    }

    /**
     * Compact line for rest "UP NEXT" prep, e.g. "12.5 × 12".
     * Prefers weight × reps when both exist; otherwise falls back to formatSetSummary.
     */
    fun formatCompactSetLine(
        set: GymWorkoutSet,
        fields: Set<TrackingField>,
        sessionUnit: WeightUnit,
    ): String {
        val weight = set.measurements.weight
        val reps = set.measurements.reps
        if (TrackingField.WEIGHT in fields &&
            TrackingField.REPS in fields &&
            weight != null &&
            reps != null
        ) {
            val body = "${formatNumber(weight)} × $reps"
            return if (set.failure) "$body · fail" else body
        }
        return formatSetSummary(set, fields, sessionUnit)
    }

    fun sessionVolumeKg(session: GymWorkoutSession): Double? {
        var total = 0.0
        var any = false
        session.exercises.forEach { exercise ->
            exercise.sets.forEach { set ->
                val volume = setVolumeKg(set, session.weightUnit)
                if (volume != null) {
                    total += volume
                    any = true
                }
            }
        }
        return if (any) total else null
    }

    fun summarize(session: GymWorkoutSession, nowEpochMilli: Long = System.currentTimeMillis()): GymWorkoutSummary {
        val end = session.endedAtEpochMilli ?: nowEpochMilli
        val savedSets = session.exercises.flatMap { it.sets }.filter { it.saved }
        val exercisesWithSets = session.exercises.count { exercise ->
            exercise.sets.any { it.saved }
        }
        return GymWorkoutSummary(
            durationSeconds = elapsedSeconds(session.startedAtEpochMilli, end),
            exerciseCount = exercisesWithSets,
            setCount = savedSets.size,
            volumeKg = sessionVolumeKg(session),
        )
    }

    fun formatSetSummary(
        set: GymWorkoutSet,
        fields: Set<TrackingField>,
        sessionUnit: WeightUnit,
    ): String {
        val parts = mutableListOf<String>()
        fields.forEach { field ->
            when (field) {
                TrackingField.WEIGHT -> {
                    val weight = set.measurements.weight
                    if (weight != null) {
                        val unit = set.measurements.weightUnit ?: sessionUnit
                        parts += "${formatNumber(weight)} ${unit.label}"
                    }
                }
                TrackingField.REPS -> set.measurements.reps?.let { parts += "${it} reps" }
                TrackingField.DURATION -> set.measurements.durationSeconds?.let {
                    parts += formatDurationShort(it)
                }
                TrackingField.DISTANCE -> set.measurements.distance?.let {
                    parts += "${formatNumber(it)} km"
                }
                TrackingField.SPEED -> set.measurements.speed?.let {
                    parts += "${formatNumber(it)} km/h"
                }
                TrackingField.INCLINE -> set.measurements.incline?.let {
                    parts += "incl ${formatNumber(it)}"
                }
                TrackingField.RESISTANCE -> set.measurements.resistance?.let {
                    parts += "res ${formatNumber(it)}"
                }
                TrackingField.ROUNDS -> set.measurements.rounds?.let { parts += "${it} rnd" }
            }
        }
        val body = parts.joinToString(" × ").ifBlank { "—" }
        return if (set.failure) "$body · fail" else body
    }

    fun formatDurationShort(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = safe / 60
        val seconds = safe % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun formatVolumeKg(volumeKg: Double?): String? {
        if (volumeKg == null) return null
        return "${formatNumber(volumeKg)} kg"
    }

    fun formatSummaryDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    data class NoteLink(
        val start: Int,
        val end: Int,
        val url: String,
    )

    fun findNoteLinks(note: String): List<NoteLink> {
        if (note.isBlank()) return emptyList()
        val matcher = urlPattern.matcher(note)
        val links = mutableListOf<NoteLink>()
        while (matcher.find()) {
            val raw = matcher.group(1) ?: continue
            val url = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
            links += NoteLink(start = matcher.start(1), end = matcher.end(1), url = url)
        }
        return links
    }

    fun nextSetNumber(existing: List<GymWorkoutSet>): Int =
        (existing.maxOfOrNull { it.setNumber } ?: 0) + 1

    fun copyMeasurementsForNewSet(previous: GymWorkoutSet?): GymSetMeasurements {
        if (previous == null) return GymSetMeasurements()
        return previous.measurements.copy()
    }

    private const val LB_TO_KG = 0.45359237
}


package com.deepak.flow.core.gym

import java.util.Locale
import java.util.regex.Pattern

object GymLogic {
    private val urlPattern: Pattern = Pattern.compile(
        """(?i)\b((?:https?://|www\.)[^\s<>\[\](){}"']+)""",
    )

    fun encodeTrackingFields(fields: Set<TrackingField>): String =
        fields.sortedBy { it.ordinal }.joinToString(",") { it.name }

    fun workoutDisplayTitle(
        title: String,
        type: GymWorkoutType = GymWorkoutType.FREE,
    ): String {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) return trimmed
        return if (type == GymWorkoutType.ROUTINE) "Workout" else "Free Workout"
    }

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

    /**
     * True when every selected tracking field has a valid value.
     * Unselected fields are ignored.
     */
    fun allSelectedFieldsFilled(
        fields: Set<TrackingField>,
        measurements: GymSetMeasurements,
    ): Boolean {
        if (fields.isEmpty()) return false
        return fields.all { field ->
            when (field) {
                TrackingField.WEIGHT -> (measurements.weight ?: 0.0) > 0.0
                TrackingField.REPS -> (measurements.reps ?: 0) >= 1
                TrackingField.DURATION -> (measurements.durationSeconds ?: 0) > 0
                TrackingField.DISTANCE -> (measurements.distance ?: 0.0) > 0.0
                TrackingField.SPEED -> (measurements.speed ?: 0.0) > 0.0
                TrackingField.INCLINE -> measurements.incline != null
                TrackingField.RESISTANCE -> (measurements.resistance ?: 0.0) > 0.0
                TrackingField.ROUNDS -> (measurements.rounds ?: 0) >= 1
            }
        }
    }

    /** Gym-friendly 0.5 step; keyboard may still enter any decimal. */
    fun stepWeightValue(raw: String, up: Boolean): String {
        val current = raw.toDoubleOrNull() ?: 0.0
        val delta = if (up) 0.5 else -0.5
        val next = ((kotlin.math.round((current + delta) * 2.0)) / 2.0).coerceAtLeast(0.0)
        return if (next == 0.0) "" else formatNumber(next)
    }

    fun stepWholeValue(raw: String, up: Boolean, minimum: Int = 1): String {
        val current = raw.toIntOrNull() ?: if (up) (minimum - 1) else minimum
        val next = (current + if (up) 1 else -1).coerceAtLeast(minimum)
        return next.toString()
    }

    fun elapsedSeconds(startedAtEpochMilli: Long, nowEpochMilli: Long): Long =
        ((nowEpochMilli - startedAtEpochMilli).coerceAtLeast(0L) / 1000L)

    fun formatStopwatch(startedAtEpochMilli: Long, nowEpochMilli: Long): String {
        val total = elapsedSeconds(startedAtEpochMilli, nowEpochMilli)
        return formatElapsedHms(total)
    }

    /** Exercise elapsed as MM:SS (minutes may exceed 59). */
    fun formatExerciseElapsed(startedAtEpochMilli: Long, nowEpochMilli: Long): String {
        val total = elapsedSeconds(startedAtEpochMilli, nowEpochMilli)
        val minutes = total / 60
        val seconds = total % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun formatElapsedHms(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Content lines for the active-workout notification.
     * Times are derived from timestamps; callers supply [nowEpochMilli].
     */
    fun activeWorkoutNotificationBody(
        exerciseName: String?,
        workoutStartedAtEpochMilli: Long,
        exerciseStartedAtEpochMilli: Long?,
        nowEpochMilli: Long,
    ): String {
        val workoutLine = "Workout ${formatStopwatch(workoutStartedAtEpochMilli, nowEpochMilli)}"
        if (exerciseName.isNullOrBlank() || exerciseStartedAtEpochMilli == null) {
            return workoutLine
        }
        val exerciseLine = "Exercise ${formatExerciseElapsed(exerciseStartedAtEpochMilli, nowEpochMilli)}"
        return "$exerciseName\n$workoutLine\n$exerciseLine"
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
        val kg = convertWeight(weight, from = unit, to = WeightUnit.KG)
        return kg * reps
    }

    /**
     * Compact set values for rest / lists, e.g. "12.5 kg × 8 reps".
     * Prefers weight × reps when both exist; otherwise falls back to formatSetSummary.
     * Failure is not appended; callers can show it separately.
     * Converts from the set's recorded unit into [displayUnit].
     */
    fun formatCompactSetLine(
        set: GymWorkoutSet,
        fields: Set<TrackingField>,
        displayUnit: WeightUnit,
    ): String {
        val weight = set.measurements.weight
        val reps = set.measurements.reps
        if (TrackingField.WEIGHT in fields &&
            TrackingField.REPS in fields &&
            weight != null &&
            reps != null
        ) {
            val storedUnit = set.measurements.weightUnit ?: displayUnit
            return "${formatWeight(weight, storedUnit, displayUnit)} ${displayUnit.label.lowercase()} × $reps reps"
        }
        return formatSetValues(set, fields, displayUnit)
    }

    /** Recorded values only, without a failure suffix. */
    fun formatSetValues(
        set: GymWorkoutSet,
        fields: Set<TrackingField>,
        displayUnit: WeightUnit,
    ): String {
        val withFail = formatSetSummary(set, fields, displayUnit)
        return withFail.removeSuffix(" · fail")
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
        displayUnit: WeightUnit,
    ): String {
        val parts = mutableListOf<String>()
        fields.forEach { field ->
            when (field) {
                TrackingField.WEIGHT -> {
                    val weight = set.measurements.weight
                    if (weight != null) {
                        val storedUnit = set.measurements.weightUnit ?: displayUnit
                        parts += "${formatWeight(weight, storedUnit, displayUnit)} ${displayUnit.label.lowercase()}"
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

    /**
     * Convert a recorded weight into another unit.
     * Does not round; callers format for display separately.
     */
    fun convertWeight(value: Double, from: WeightUnit, to: WeightUnit): Double {
        if (from == to) return value
        return when {
            from == WeightUnit.KG && to == WeightUnit.LB -> value * KG_TO_LB
            from == WeightUnit.LB && to == WeightUnit.KG -> value / KG_TO_LB
            else -> value
        }
    }

    /** Display weight converted from [storedUnit] into [displayUnit], with UI rounding only. */
    fun formatWeight(
        weight: Double,
        storedUnit: WeightUnit,
        displayUnit: WeightUnit,
    ): String = formatNumber(convertWeight(weight, from = storedUnit, to = displayUnit))

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

    fun lastSavedSet(sets: List<GymWorkoutSet>): GymWorkoutSet? =
        sets.filter { it.saved }.maxByOrNull { it.setNumber }

    /**
     * Seed for the next set: the user's last completed values in this occurrence,
     * else the previous occurrence's final completed set.
     */
    fun seedMeasurementsForNextSet(
        currentSets: List<GymWorkoutSet>,
        previousOccurrenceLastSet: GymSetMeasurements?,
    ): GymSetMeasurements {
        val lastCompleted = lastSavedSet(currentSets)
        if (lastCompleted != null) return lastCompleted.measurements.copy()
        return previousOccurrenceLastSet?.copy() ?: GymSetMeasurements()
    }

    fun matchPreviousExercise(
        previousExercises: List<GymWorkoutExercise>,
        routineExerciseId: Long?,
        exerciseStableKey: String?,
        name: String,
    ): GymWorkoutExercise? {
        if (!exerciseStableKey.isNullOrBlank()) {
            previousExercises.firstOrNull {
                it.exerciseStableKey == exerciseStableKey
            }?.let { return it }
        }
        if (routineExerciseId != null) {
            previousExercises.firstOrNull { it.routineExerciseId == routineExerciseId }?.let {
                return it
            }
        }
        val key = name.trim().lowercase(Locale.US)
        if (key.isEmpty()) return null
        return previousExercises.firstOrNull { it.name.trim().lowercase(Locale.US) == key }
    }

    fun formatDayHeading(dayIndex: Int, title: String, isRestDay: Boolean): String {
        val number = "Day ${dayIndex + 1}"
        val trimmed = title.trim()
        return when {
            isRestDay && trimmed.isEmpty() -> "$number — Rest Day"
            isRestDay -> "$number — $trimmed"
            trimmed.isEmpty() -> number
            else -> "$number — $trimmed"
        }
    }

    fun dayHeadingPrefix(dayIndex: Int): String = "Day ${dayIndex + 1} — "

    fun dayHeadingSuffixLabel(title: String, isRestDay: Boolean): String {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) return trimmed
        return if (isRestDay) "Rest Day" else "Day title"
    }

    fun cycleCompletesAfterDay(currentDayIndex: Int, dayCount: Int): Boolean =
        dayCount > 0 && currentDayIndex == dayCount - 1

    fun formatRoundsCompleted(count: Int): String = when (count) {
        1 -> "1 round completed"
        else -> "$count rounds completed"
    }

    fun isPlannedSet(plannedSetCount: Int): Boolean = plannedSetCount > 0

    fun isLastPlannedSetNumber(setNumber: Int, plannedSetCount: Int): Boolean =
        plannedSetCount > 0 && setNumber >= plannedSetCount

    fun shouldAutoOpenNextPlannedSet(savedCount: Int, plannedSetCount: Int): Boolean =
        plannedSetCount > 0 && savedCount < plannedSetCount

    fun nextDayIndex(currentDayIndex: Int, dayCount: Int): Int {
        if (dayCount <= 0) return 0
        return (currentDayIndex + 1).mod(dayCount)
    }

    /** 1 kg = 2.20462262185 lb. lb→kg uses division by this factor. */
    const val KG_TO_LB = 2.20462262185

    /** Completed workouts stay editable for exactly 24 hours after endedAtEpochMilli. */
    const val POST_WORKOUT_EDIT_WINDOW_MS = 24L * 60L * 60L * 1000L

    /**
     * True while [nowEpochMilli] is strictly before completedAt + 24h.
     * Requires a real completion timestamp; null means not editable.
     */
    fun isWithinPostWorkoutEditWindow(
        endedAtEpochMilli: Long?,
        nowEpochMilli: Long,
    ): Boolean {
        if (endedAtEpochMilli == null) return false
        return nowEpochMilli < endedAtEpochMilli + POST_WORKOUT_EDIT_WINDOW_MS
    }

    /** True when [measurements] has a recorded value for [field]. */
    fun hasValueForField(measurements: GymSetMeasurements, field: TrackingField): Boolean =
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

    /**
     * Fields being removed that already have saved values on any set.
     * Callers should warn before clearing those values.
     */
    fun fieldsLosingRecordedValues(
        currentFields: Set<TrackingField>,
        nextFields: Set<TrackingField>,
        sets: List<GymWorkoutSet>,
    ): Set<TrackingField> {
        val removed = currentFields - nextFields
        if (removed.isEmpty()) return emptySet()
        return removed.filter { field ->
            sets.any { set -> set.saved && hasValueForField(set.measurements, field) }
        }.toSet()
    }

    /** Clears values for removed tracking fields; leaves others untouched. */
    fun clearMeasurementsForFields(
        measurements: GymSetMeasurements,
        fieldsToClear: Set<TrackingField>,
    ): GymSetMeasurements {
        if (fieldsToClear.isEmpty()) return measurements
        var next = measurements
        fieldsToClear.forEach { field ->
            next = when (field) {
                TrackingField.WEIGHT -> next.copy(weight = null, weightUnit = null)
                TrackingField.REPS -> next.copy(reps = null)
                TrackingField.DURATION -> next.copy(durationSeconds = null)
                TrackingField.DISTANCE -> next.copy(distance = null)
                TrackingField.SPEED -> next.copy(speed = null)
                TrackingField.INCLINE -> next.copy(incline = null)
                TrackingField.RESISTANCE -> next.copy(resistance = null)
                TrackingField.ROUNDS -> next.copy(rounds = null)
            }
        }
        return next
    }

    fun <T> reorderListByMove(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex == toIndex) return items
        if (fromIndex !in items.indices || toIndex !in items.indices) return items
        val mutable = items.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        return mutable
    }

    fun reorderListByKey(keys: List<String>, movedKey: String, toIndex: Int): List<String> {
        val fromIndex = keys.indexOf(movedKey)
        if (fromIndex < 0) return keys
        return reorderListByMove(keys, fromIndex, toIndex)
    }

    fun reorderDaysByKeys(
        days: List<GymRoutineDay>,
        orderedKeys: List<String>,
    ): List<GymRoutineDay>? {
        if (orderedKeys.size != days.size) return null
        val byKey = days.associateBy { it.localKey }
        if (orderedKeys.any { it !in byKey }) return null
        return orderedKeys.mapIndexed { index, key ->
            byKey.getValue(key).copy(dayIndex = index)
        }
    }
}


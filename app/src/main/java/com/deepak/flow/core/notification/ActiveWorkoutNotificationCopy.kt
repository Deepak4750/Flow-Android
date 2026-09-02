package com.deepak.flow.core.notification

import com.deepak.flow.core.gym.GymLogic
import com.deepak.flow.core.gym.GymRestKind
import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutExercisePolicy
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutSet
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.gym.TrackingField
import com.deepak.flow.core.gym.WeightUnit
import com.deepak.flow.core.widget.WidgetLaunch

/**
 * Glanceable copy for the ongoing active-workout notification.
 * Derived from persisted session state only.
 */
data class ActiveWorkoutNotificationSnapshot(
    val contentTitle: String,
    val contentText: String,
    val subText: String?,
    val expandedText: String,
    val fingerprint: String,
    val isResting: Boolean,
    val workoutId: Long,
    val destination: String,
)

object ActiveWorkoutNotificationCopy {
    const val APP_LABEL = "Flow"
    const val HEADLINE = "Workout in progress"

    fun fromSession(
        session: GymWorkoutSession,
        nowEpochMilli: Long,
        appLabel: String = APP_LABEL,
    ): ActiveWorkoutNotificationSnapshot {
        val destination = when (session.type) {
            GymWorkoutType.ROUTINE -> WidgetLaunch.DEST_GYM_ROUTINE_WORKOUT
            else -> WidgetLaunch.DEST_GYM_FREE_WORKOUT
        }
        val exercise = currentExercise(session)
        val exerciseName = exercise?.name?.trim().orEmpty()
        val resting = GymLogic.remainingRestSeconds(session.restEndsAtEpochMilli, nowEpochMilli) > 0

        return if (resting) {
            restSnapshot(
                session = session,
                exercise = exercise,
                exerciseName = exerciseName,
                nowEpochMilli = nowEpochMilli,
                appLabel = appLabel,
                destination = destination,
            )
        } else {
            activeSnapshot(
                exercise = exercise,
                exerciseName = exerciseName,
                weightUnit = session.weightUnit,
                appLabel = appLabel,
                workoutId = session.id,
                destination = destination,
            )
        }
    }

    private fun restSnapshot(
        session: GymWorkoutSession,
        exercise: GymWorkoutExercise?,
        exerciseName: String,
        nowEpochMilli: Long,
        appLabel: String,
        destination: String,
    ): ActiveWorkoutNotificationSnapshot {
        val remaining = GymLogic.formatCountdown(
            GymLogic.remainingRestSeconds(session.restEndsAtEpochMilli, nowEpochMilli),
        )
        val restLine = "Rest · $remaining"
        val nextLine = nextExerciseLine(session, exercise)
        val expanded = buildExpandedBody(
            headline = HEADLINE,
            exerciseName = exerciseName,
            detailLine = restLine,
            extraLine = nextLine,
        )
        return ActiveWorkoutNotificationSnapshot(
            contentTitle = appLabel,
            contentText = restLine,
            subText = exerciseName.takeIf { it.isNotBlank() },
            expandedText = expanded,
            fingerprint = restFingerprint(
                exerciseName = exerciseName,
                restLine = restLine,
                nextLine = nextLine,
                destination = destination,
            ),
            isResting = true,
            workoutId = session.id,
            destination = destination,
        )
    }

    private fun activeSnapshot(
        exercise: GymWorkoutExercise?,
        exerciseName: String,
        weightUnit: WeightUnit,
        appLabel: String,
        workoutId: Long,
        destination: String,
    ): ActiveWorkoutNotificationSnapshot {
        val setLine = exercise?.let { setProgressLine(it, weightUnit) }
        val expanded = buildExpandedBody(
            headline = HEADLINE,
            exerciseName = exerciseName,
            detailLine = setLine,
            extraLine = null,
        )
        val collapsedText = when {
            exerciseName.isNotBlank() -> exerciseName
            else -> HEADLINE
        }
        return ActiveWorkoutNotificationSnapshot(
            contentTitle = appLabel,
            contentText = collapsedText,
            subText = setLine?.takeIf { exerciseName.isNotBlank() },
            expandedText = expanded,
            fingerprint = activeFingerprint(
                exerciseName = exerciseName,
                setLine = setLine,
                destination = destination,
            ),
            isResting = false,
            workoutId = workoutId,
            destination = destination,
        )
    }

    fun setProgressLine(exercise: GymWorkoutExercise, weightUnit: WeightUnit): String? {
        val saved = exercise.sets.filter { it.saved }
        val lastSaved = saved.maxByOrNull { it.setNumber }
        val planned = exercise.plannedSetCount
        val setLabel = when {
            lastSaved != null && planned > 0 -> "Set ${lastSaved.setNumber}/$planned"
            lastSaved != null -> "Set ${lastSaved.setNumber}"
            planned > 0 -> "Set 1/$planned"
            saved.isEmpty() -> null
            else -> "Set 1"
        } ?: return null
        val values = lastSaved?.let { formatNotificationSetValues(it, exercise.trackingFields, weightUnit) }
        return if (values != null) "$setLabel · $values" else setLabel
    }

    fun nextExerciseLine(session: GymWorkoutSession, current: GymWorkoutExercise?): String? {
        if (session.restKind != GymRestKind.EXERCISE) return null
        val currentId = current?.id ?: return null
        val nextName = GymWorkoutExercisePolicy.unresolvedExercises(session.exercises)
            .firstOrNull { it.id != currentId }
            ?.name
            ?.trim()
            .orEmpty()
        if (nextName.isBlank()) return null
        return "Next: $nextName"
    }

    fun formatNotificationSetValues(
        set: GymWorkoutSet,
        fields: Set<TrackingField>,
        displayUnit: WeightUnit,
    ): String? {
        val weight = set.measurements.weight
        val reps = set.measurements.reps
        if (TrackingField.WEIGHT in fields &&
            TrackingField.REPS in fields &&
            weight != null &&
            reps != null
        ) {
            val storedUnit = set.measurements.weightUnit ?: displayUnit
            return "${GymLogic.formatWeight(weight, storedUnit, displayUnit)} ${displayUnit.label.lowercase()} × $reps"
        }
        return GymLogic.formatSetValues(set, fields, displayUnit).takeIf { it.isNotBlank() }
    }

    private fun currentExercise(session: GymWorkoutSession): GymWorkoutExercise? {
        if (session.exercises.isEmpty()) return null
        val index = session.currentExerciseIndex.coerceIn(0, session.exercises.lastIndex)
        return session.exercises[index]
    }

    private fun buildExpandedBody(
        headline: String,
        exerciseName: String,
        detailLine: String?,
        extraLine: String?,
    ): String = buildString {
        append(headline)
        if (exerciseName.isNotBlank()) {
            append('\n')
            append(exerciseName)
        }
        if (!detailLine.isNullOrBlank()) {
            append('\n')
            append(detailLine)
        }
        if (!extraLine.isNullOrBlank()) {
            append('\n')
            append(extraLine)
        }
    }

    private fun restFingerprint(
        exerciseName: String,
        restLine: String,
        nextLine: String?,
        destination: String,
    ): String = listOf("rest", exerciseName, restLine, nextLine.orEmpty(), destination).joinToString("|")

    private fun activeFingerprint(
        exerciseName: String,
        setLine: String?,
        destination: String,
    ): String = listOf("active", exerciseName, setLine.orEmpty(), destination).joinToString("|")
}

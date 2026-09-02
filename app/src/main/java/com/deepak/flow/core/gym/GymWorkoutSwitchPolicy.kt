package com.deepak.flow.core.gym

/**
 * Rules for when the user may switch away from the active exercise.
 */
object GymWorkoutSwitchPolicy {
    /**
     * True when the active exercise still has required work remaining before switching away.
     */
    fun hasUnfinishedRequiredWork(
        exercise: GymWorkoutExercise?,
        trackingFields: Set<TrackingField>,
        setDraft: GymSetDraftSnapshot,
        setEditorVisible: Boolean,
        awaitingNextAction: Boolean,
        composingExercise: Boolean,
        isResting: Boolean,
    ): Boolean {
        if (isResting) return true
        if (exercise == null) return false
        if (exercise.skipped || exercise.completedAtEpochMilli != null) return false
        if (composingExercise) return true

        val draftHasUserData = setEditorVisible &&
            GymSetDraftPolicy.hasUserEnteredData(
                trackingFields = trackingFields,
                weight = setDraft.weight,
                reps = setDraft.reps,
                durationMinutes = setDraft.durationMinutes,
                durationSeconds = setDraft.durationSeconds,
                distance = setDraft.distance,
                speed = setDraft.speed,
                incline = setDraft.incline,
                resistance = setDraft.resistance,
                rounds = setDraft.rounds,
            )
        if (draftHasUserData) return true

        val savedSets = exercise.sets.filter { it.saved }
        val hasSavedWork = savedSets.isNotEmpty()

        if (!hasSavedWork) return false

        if (exercise.plannedSetCount > 0) {
            for (setNumber in 1..exercise.plannedSetCount) {
                val recorded = savedSets.any { it.setNumber == setNumber }
                if (!recorded) return true
            }
            return false
        }

        if (awaitingNextAction) return false
        return false
    }

    fun canSwitchToTarget(
        activeExercise: GymWorkoutExercise?,
        targetExercise: GymWorkoutExercise,
        trackingFields: Set<TrackingField>,
        setDraft: GymSetDraftSnapshot,
        setEditorVisible: Boolean,
        awaitingNextAction: Boolean,
        composingExercise: Boolean,
        isResting: Boolean,
    ): Boolean {
        if (isResting) return false
        if (!GymWorkoutExercisePolicy.isExerciseSelectable(targetExercise)) return false
        if (activeExercise?.id == targetExercise.id) return true
        return !hasUnfinishedRequiredWork(
            exercise = activeExercise,
            trackingFields = trackingFields,
            setDraft = setDraft,
            setEditorVisible = setEditorVisible,
            awaitingNextAction = awaitingNextAction,
            composingExercise = composingExercise,
            isResting = isResting,
        )
    }

    fun shouldShowSwitchAction(
        isFocused: Boolean,
        isSelectable: Boolean,
        hasFocusedExerciseInSession: Boolean,
        isResting: Boolean,
    ): Boolean = !isFocused && isSelectable && hasFocusedExerciseInSession && !isResting
}

/**
 * Editor draft values used by [GymWorkoutSwitchPolicy] without depending on UI-layer types.
 */
data class GymSetDraftSnapshot(
    val setNumber: Int = 1,
    val weight: String = "",
    val reps: String = "",
    val durationMinutes: String = "",
    val durationSeconds: String = "",
    val distance: String = "",
    val speed: String = "",
    val incline: String = "",
    val resistance: String = "",
    val rounds: String = "",
)

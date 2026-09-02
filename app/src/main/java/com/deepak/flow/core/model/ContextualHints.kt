package com.deepak.flow.core.model

/**
 * Legacy persisted gesture-hint flags. Kept for Room migration compatibility only.
 */
data class ContextualHints(
    val routineSwipeDeleteHintShown: Boolean = ROUTINE_SWIPE_DELETE_HINT_SHOWN_DEFAULT,
    val builderDaySwipeDeleteHintShown: Boolean = BUILDER_DAY_SWIPE_DELETE_HINT_SHOWN_DEFAULT,
) {
    companion object {
        const val ROUTINE_SWIPE_DELETE_HINT_SHOWN_DEFAULT = true
        const val BUILDER_DAY_SWIPE_DELETE_HINT_SHOWN_DEFAULT = true
    }
}

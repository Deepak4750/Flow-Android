package com.deepak.flow.core.gym

/**
 * Pure helpers for gym rest duration text fields (Set Rest / Exercise Rest).
 * [commit] runs when the field loses focus or the user submits.
 */
object GymRestSecondsInput {

    fun filterDigits(raw: String): String = raw.filter { it.isDigit() }.take(4)

    /** True when the user may leave Settings with this field value (not empty, at least [min]). */
    fun isValidForSettingsLeave(raw: String, min: Int, max: Int): Boolean =
        GymRestSettingsLeaveGuard.isValidRestSeconds(raw, min, max)

    fun commit(raw: String, min: Int, max: Int): Int {
        if (raw.isEmpty()) return min
        val parsed = raw.toIntOrNull() ?: return min
        return parsed.coerceIn(min, max)
    }
}

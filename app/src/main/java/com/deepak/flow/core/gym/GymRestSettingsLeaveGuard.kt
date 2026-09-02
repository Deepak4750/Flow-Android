package com.deepak.flow.core.gym

enum class GymRestSettingsField {
    SET_REST,
    EXERCISE_REST,
}

data class GymRestSettingsLeaveValidation(
    val canLeave: Boolean,
    val invalidField: GymRestSettingsField? = null,
)

/**
 * Validates gym rest timer fields before leaving Settings.
 * Empty or values below [minSeconds] block navigation; no silent clamping.
 */
object GymRestSettingsLeaveGuard {

    fun isValidRestSeconds(raw: String, minSeconds: Int, maxSeconds: Int): Boolean {
        if (raw.isEmpty()) return false
        val parsed = raw.toIntOrNull() ?: return false
        return parsed in minSeconds..maxSeconds
    }

    fun validate(
        setRestRaw: String,
        exerciseRestRaw: String,
        minSeconds: Int = GymLimits.SET_REST_MIN_SECONDS,
        maxSeconds: Int = GymLimits.SET_REST_MAX_SECONDS,
    ): GymRestSettingsLeaveValidation {
        if (!isValidRestSeconds(setRestRaw, minSeconds, maxSeconds)) {
            return GymRestSettingsLeaveValidation(
                canLeave = false,
                invalidField = GymRestSettingsField.SET_REST,
            )
        }
        if (!isValidRestSeconds(exerciseRestRaw, minSeconds, maxSeconds)) {
            return GymRestSettingsLeaveValidation(
                canLeave = false,
                invalidField = GymRestSettingsField.EXERCISE_REST,
            )
        }
        return GymRestSettingsLeaveValidation(canLeave = true)
    }
}

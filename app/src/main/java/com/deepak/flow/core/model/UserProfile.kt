package com.deepak.flow.core.model

import kotlin.math.roundToInt

data class UserProfile(
    val displayName: String? = null,
    val nickname: String? = null,
    val onboardingCompleted: Boolean = false,
    val snoozeEnabled: Boolean = SnoozeSettings.DEFAULT_ENABLED,
    val snoozeIntervalMinutes: Int = SnoozeSettings.DEFAULT_INTERVAL_MINUTES,
    val remindersEnabled: Boolean = DEFAULT_REMINDERS_ENABLED,
    val waterEnabled: Boolean = DEFAULT_WATER_ENABLED,
    val gymEnabled: Boolean = DEFAULT_GYM_ENABLED,
    val gymWeightUnit: String = DEFAULT_GYM_WEIGHT_UNIT,
    val gymSetRestSeconds: Int = DEFAULT_GYM_SET_REST_SECONDS,
    val gymExerciseRestSeconds: Int = DEFAULT_GYM_EXERCISE_REST_SECONDS,
    val waterGoalMl: Int? = null,
    val waterBottleStyleIndex: Int? = null,
    val waterIntakeMl: Int = 0,
    val waterIntakeEpochDay: Long? = null,
    val waterAddLog: String = "",
    val waterCustomQuickAddsMl: String = "",
    val waterRemindersEnabled: Boolean = WaterReminderSettings.DEFAULT_ENABLED,
    val waterReminderIntervalMinutes: Int = WaterReminderSettings.DEFAULT_INTERVAL_MINUTES,
    val waterActiveHoursEnabled: Boolean = WaterReminderSettings.DEFAULT_ACTIVE_HOURS_ENABLED,
    val waterActiveHoursStartMinutes: Int = WaterReminderSettings.DEFAULT_ACTIVE_START_MINUTES,
    val waterActiveHoursEndMinutes: Int = WaterReminderSettings.DEFAULT_ACTIVE_END_MINUTES,
    val keepDataOnUninstall: Boolean = DEFAULT_KEEP_DATA_ON_UNINSTALL,
    val activeGymRoutineId: Long? = null,
) {
    companion object {
        const val DEFAULT_REMINDERS_ENABLED = true
        const val DEFAULT_WATER_ENABLED = false
        const val DEFAULT_GYM_ENABLED = true
        const val DEFAULT_GYM_WEIGHT_UNIT = "KG"
        const val DEFAULT_GYM_SET_REST_SECONDS = 90
        const val DEFAULT_GYM_EXERCISE_REST_SECONDS = 120
        const val DEFAULT_KEEP_DATA_ON_UNINSTALL = true
        const val MIN_WATER_GOAL_ML = 250
        const val MAX_WATER_GOAL_ML = 7000
        const val MAX_WATER_INTAKE_ML = 7000
        const val BOTTLE_STYLE_COUNT = 3
        const val MIN_CUSTOM_WATER_ML = 10
        const val MAX_CUSTOM_WATER_ML = 999
        const val MAX_CUSTOM_WATER_QUICK_ADDS = 3
    }
}

fun UserProfile?.remindersFeatureEnabled(): Boolean =
    this?.remindersEnabled ?: UserProfile.DEFAULT_REMINDERS_ENABLED

fun UserProfile?.waterFeatureEnabled(): Boolean =
    this?.waterEnabled ?: UserProfile.DEFAULT_WATER_ENABLED

fun UserProfile?.gymFeatureEnabled(): Boolean =
    this?.gymEnabled ?: UserProfile.DEFAULT_GYM_ENABLED

fun parseWaterGoalMl(raw: String): Int? {
    val cleaned = raw.trim()
        .removeSuffix("L")
        .removeSuffix("l")
        .trim()
    if (cleaned.isEmpty() || cleaned == ".") return null
    val liters = cleaned.toDoubleOrNull() ?: return null
    val millilitres = (liters * 1000.0).roundToInt()
    return millilitres.takeIf {
        it in UserProfile.MIN_WATER_GOAL_ML..UserProfile.MAX_WATER_GOAL_ML
    }
}

fun filterWaterGoalInput(raw: String): String {
    val builder = StringBuilder()
    var seenDot = false
    var decimals = 0
    for (char in raw) {
        when {
            char.isDigit() -> {
                if (seenDot) {
                    if (decimals >= 2) continue
                    decimals++
                }
                builder.append(char)
            }
            (char == '.' || char == ',') && !seenDot -> {
                seenDot = true
                builder.append('.')
            }
        }
    }
    return builder.toString()
}

fun formatWaterLiters(millilitres: Int): String {
    val amount = millilitres.coerceAtLeast(0)
    val text = when {
        amount % 1000 == 0 -> (amount / 1000).toString()
        amount % 100 == 0 -> String.format(java.util.Locale.US, "%.1f", amount / 1000.0)
        else -> String.format(java.util.Locale.US, "%.2f", amount / 1000.0)
    }
    return "$text L"
}

fun formatWaterGoalInput(millilitres: Int): String {
    val amount = millilitres.coerceAtLeast(0)
    return when {
        amount % 1000 == 0 -> (amount / 1000).toString()
        amount % 100 == 0 -> String.format(java.util.Locale.US, "%.1f", amount / 1000.0)
        else -> String.format(java.util.Locale.US, "%.2f", amount / 1000.0)
    }
}

fun parseWaterBottleStyleIndex(index: Int): Int? =
    index.takeIf { it in 0 until UserProfile.BOTTLE_STYLE_COUNT }

fun UserProfile.todayWaterIntakeMl(todayEpochDay: Long): Int {
    if (waterIntakeEpochDay != todayEpochDay) return 0
    return waterIntakeMl.coerceIn(0, UserProfile.MAX_WATER_INTAKE_ML)
}

data class WaterIntakeWrite(
    val millilitres: Int,
    val addLog: List<Int>,
)

fun encodeWaterAddLog(amounts: List<Int>): String =
    amounts.filter { it > 0 }.joinToString(",")

fun decodeWaterAddLog(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(',').mapNotNull { token ->
        token.trim().toIntOrNull()?.takeIf { it > 0 }
    }
}

fun encodeWaterCustomQuickAdds(amounts: List<Int>): String =
    amounts
        .mapNotNull { parseCustomWaterMl(it.toString()) }
        .distinct()
        .sorted()
        .take(UserProfile.MAX_CUSTOM_WATER_QUICK_ADDS)
        .joinToString(",")

fun decodeWaterCustomQuickAdds(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(',')
        .mapNotNull { token -> parseCustomWaterMl(token.trim()) }
        .distinct()
        .sorted()
        .take(UserProfile.MAX_CUSTOM_WATER_QUICK_ADDS)
}

fun parseCustomWaterMl(raw: String): Int? {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return null
    val millilitres = digits.toIntOrNull() ?: return null
    return millilitres.takeIf {
        it in UserProfile.MIN_CUSTOM_WATER_ML..UserProfile.MAX_CUSTOM_WATER_ML
    }
}

fun filterCustomWaterInput(raw: String): String =
    raw.filter { it.isDigit() }.take(3)

/** Built-in quick adds plus saved custom amounts, sorted by size. */
val WaterBuiltinQuickAddsMl = listOf(250, 500, 1000)

fun waterQuickAddAmountsMl(customAmounts: List<Int>): List<Int> {
    return (WaterBuiltinQuickAddsMl + customAmounts.mapNotNull { parseCustomWaterMl(it.toString()) }
        .take(UserProfile.MAX_CUSTOM_WATER_QUICK_ADDS))
        .distinct()
        .sorted()
}

fun isExistingWaterQuickAddMl(amountMl: Int, customAmounts: List<Int>): Boolean =
    amountMl in waterQuickAddAmountsMl(customAmounts)

fun UserProfile.waterCustomQuickAdds(): List<Int> =
    decodeWaterCustomQuickAdds(waterCustomQuickAddsMl)

fun UserProfile.canSaveWaterCustomQuickAdd(amountMl: Int): Boolean {
    val parsed = parseCustomWaterMl(amountMl.toString()) ?: return false
    if (isExistingWaterQuickAddMl(parsed, waterCustomQuickAdds())) return false
    return waterCustomQuickAdds().size < UserProfile.MAX_CUSTOM_WATER_QUICK_ADDS
}

fun UserProfile.withWaterCustomQuickAdd(amount: Int): List<Int>? {
    val parsed = parseCustomWaterMl(amount.toString()) ?: return null
    val current = waterCustomQuickAdds()
    if (isExistingWaterQuickAddMl(parsed, current)) return null
    if (current.size >= UserProfile.MAX_CUSTOM_WATER_QUICK_ADDS) return null
    return (current + parsed).sorted()
}

fun UserProfile.withoutWaterCustomQuickAdd(amount: Int): List<Int> =
    waterCustomQuickAdds().filterNot { it == amount }

fun UserProfile.todayWaterAddLog(todayEpochDay: Long): List<Int> {
    if (waterIntakeEpochDay != todayEpochDay) return emptyList()
    return decodeWaterAddLog(waterAddLog)
}

fun UserProfile.canUndoWater(todayEpochDay: Long): Boolean =
    todayWaterIntakeMl(todayEpochDay) > 0 && todayWaterAddLog(todayEpochDay).isNotEmpty()

fun UserProfile.withWaterAdd(amount: Int, todayEpochDay: Long): WaterIntakeWrite? {
    if (amount <= 0) return null
    val current = todayWaterIntakeMl(todayEpochDay)
    val next = (current + amount).coerceAtMost(UserProfile.MAX_WATER_INTAKE_ML)
    val added = next - current
    if (added <= 0) return null
    return WaterIntakeWrite(
        millilitres = next,
        addLog = todayWaterAddLog(todayEpochDay) + added,
    )
}

fun UserProfile.waterDrinkRemindersOn(): Boolean =
    waterEnabled && waterRemindersEnabled

/** Water drink reminders always use active hours - not optional. */
fun UserProfile.waterActiveHoursOrNull(): ActiveHours =
    ActiveHours(
        startTime = WaterReminderSettings.localTimeFromMinutes(waterActiveHoursStartMinutes),
        endTime = WaterReminderSettings.localTimeFromMinutes(waterActiveHoursEndMinutes),
    )

fun UserProfile.withWaterUndo(todayEpochDay: Long): WaterIntakeWrite? {
    val log = todayWaterAddLog(todayEpochDay)
    if (log.isEmpty()) return null
    val current = todayWaterIntakeMl(todayEpochDay)
    val next = (current - log.last()).coerceAtLeast(0)
    return WaterIntakeWrite(
        millilitres = next,
        addLog = log.dropLast(1),
    )
}

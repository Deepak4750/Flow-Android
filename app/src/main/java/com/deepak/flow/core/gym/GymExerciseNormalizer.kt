package com.deepak.flow.core.gym

import java.util.Locale

object GymExerciseNormalizer {
    fun normalize(raw: String): String =
        raw.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.US)

    fun normalizeKey(raw: String): String = normalize(raw)
}

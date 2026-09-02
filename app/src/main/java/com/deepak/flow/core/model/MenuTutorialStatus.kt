package com.deepak.flow.core.model

/**
 * Legacy persisted menu-tutorial state. Kept for Room migration compatibility only.
 */
enum class MenuTutorialStatus {
    NOT_NEEDED,
    PENDING,
    COMPLETED,
    ;

    companion object {
        val DEFAULT = COMPLETED

        fun fromStored(raw: String?): MenuTutorialStatus =
            entries.find { it.name == raw } ?: DEFAULT
    }
}

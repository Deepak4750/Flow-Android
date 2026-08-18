package com.deepak.flow.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Category(val displayName: String) {
    @SerialName("health")
    HEALTH("Health"),

    @SerialName("fitness")
    FITNESS("Fitness"),

    @SerialName("study")
    STUDY("Study"),

    @SerialName("work")
    WORK("Work"),

    @SerialName("personal")
    PERSONAL("Personal"),

    @SerialName("custom")
    CUSTOM("Custom"),
}

fun Reminder.categoryLabel(): String =
    if (category == Category.CUSTOM && !customCategoryName.isNullOrBlank()) {
        customCategoryName
    } else {
        category.displayName
    }

data class SavedCustomCategory(
    val name: String,
    val accentColorIndex: Int?,
)

fun List<Reminder>.savedCustomCategories(): List<SavedCustomCategory> {
    val latestByName = linkedMapOf<String, Pair<Long, Int?>>()
    for (reminder in this) {
        if (reminder.category != Category.CUSTOM) continue
        val name = reminder.customCategoryName?.trim()?.takeIf { it.isNotEmpty() } ?: continue
        val previous = latestByName[name]
        if (previous == null || reminder.id >= previous.first) {
            latestByName[name] = reminder.id to reminder.accentColorIndex
        }
    }
    return latestByName
        .map { (name, idAndAccent) -> SavedCustomCategory(name, idAndAccent.second) }
        .sortedBy { it.name }
}

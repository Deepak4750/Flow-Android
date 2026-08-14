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

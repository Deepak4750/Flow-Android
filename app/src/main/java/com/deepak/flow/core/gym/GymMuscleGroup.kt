package com.deepak.flow.core.gym

enum class GymMuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CORE("Core"),
    TRAPS("Traps"),
    FULL_BODY("Full Body"),
    ;

    companion object {
        fun fromStored(value: String?): GymMuscleGroup? =
            entries.firstOrNull { it.name == value || it.displayName.equals(value, ignoreCase = true) }
    }
}

package com.deepak.flow.core.gym

enum class GymEquipment(val displayName: String) {
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    CABLE("Cable"),
    MACHINE("Machine"),
    SMITH_MACHINE("Smith Machine"),
    KETTLEBELL("Kettlebell"),
    BODYWEIGHT("Bodyweight"),
    RESISTANCE_BAND("Resistance Band"),
    EZ_BAR("EZ Bar"),
    TRAP_BAR("Trap Bar"),
    PLATE("Plate"),
    LANDMINE("Landmine"),
    SUSPENSION("Suspension"),
    OTHER("Other"),
    ;

    companion object {
        fun fromStored(value: String?): GymEquipment? =
            entries.firstOrNull { it.name == value || it.displayName.equals(value, ignoreCase = true) }
    }
}

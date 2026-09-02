package com.deepak.flow.core.gym

object GymExerciseMetadataCodec {
    private const val SEPARATOR = ","

    fun encodeMuscles(muscles: List<GymMuscleGroup>): String? =
        muscles.takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR) { it.name }

    fun decodeMuscles(raw: String?): List<GymMuscleGroup> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(SEPARATOR)
            .mapNotNull { token -> GymMuscleGroup.fromStored(token.trim()) }
    }

    fun encodeEquipment(equipment: GymEquipment?): String? = equipment?.name

    fun decodeEquipment(raw: String?): GymEquipment? = GymEquipment.fromStored(raw)
}

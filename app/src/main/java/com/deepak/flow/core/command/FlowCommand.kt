package com.deepak.flow.core.command

import com.deepak.flow.core.gym.GymEquipment
import com.deepak.flow.core.gym.GymMuscleGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Structured command model for future on-device natural language interpretation.
 * Commands are validated, then routed to existing deterministic Flow business logic.
 * The LLM must never manipulate Room directly.
 */
@Serializable
sealed class FlowCommand {
    abstract val type: String

    // --- Tasks ---
    @Serializable
    data class CreateTask(
        val name: String,
        val frequency: String? = null,
        val anchor: String? = null,
        val time: String? = null,
    ) : FlowCommand() {
        override val type: String = TYPE

        companion object {
            const val TYPE = "CREATE_TASK"
        }
    }

    @Serializable
    data class EditTask(val taskId: Long, val patch: Map<String, String> = emptyMap()) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "EDIT_TASK" }
    }

    @Serializable
    data class DeleteTask(val taskId: Long) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "DELETE_TASK" }
    }

    @Serializable
    data class CompleteTask(val taskId: Long) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "COMPLETE_TASK" }
    }

    // --- Water ---
    @Serializable
    data class LogWater(val amountMl: Int) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "LOG_WATER" }
    }

    @Serializable
    data class UndoWater(val amountMl: Int? = null) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "UNDO_WATER" }
    }

    @Serializable
    data class GetWaterStatus(val dateEpochDay: Long? = null) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "GET_WATER_STATUS" }
    }

    // --- Gym ---
    @Serializable
    data class CreateRoutine(val name: String, val days: List<GeneratedRoutineDay> = emptyList()) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "CREATE_ROUTINE" }
    }

    @Serializable
    data class EditRoutine(val routineId: Long, val patch: Map<String, String> = emptyMap()) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "EDIT_ROUTINE" }
    }

    @Serializable
    data class AddExercise(
        val routineId: Long? = null,
        val dayIndex: Int? = null,
        val exerciseId: String,
        val setCount: Int = 3,
    ) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "ADD_EXERCISE" }
    }

    @Serializable
    data class RemoveExercise(val routineId: Long, val exerciseStableKey: String) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "REMOVE_EXERCISE" }
    }

    @Serializable
    data class SaveRoutine(val routineId: Long) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "SAVE_ROUTINE" }
    }

    @Serializable
    data class StartWorkout(val routineId: Long? = null, val dayIndex: Int? = null) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "START_WORKOUT" }
    }

    // --- History ---
    @Serializable
    data class GetPreviousPerformance(val exerciseId: String) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "GET_PREVIOUS_PERFORMANCE" }
    }

    @Serializable
    data class GetExerciseHistory(val exerciseId: String, val limit: Int = 10) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "GET_EXERCISE_HISTORY" }
    }

    // --- Preferences ---
    @Serializable
    data class SetPreference(val key: String, val value: String) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "SET_PREFERENCE" }
    }

    @Serializable
    data class GetPreference(val key: String) : FlowCommand() {
        override val type: String = TYPE
        companion object { const val TYPE = "GET_PREFERENCE" }
    }
}

@Serializable
data class GeneratedRoutineDay(
    val name: String,
    val exercises: List<GeneratedRoutineExercise> = emptyList(),
)

@Serializable
data class GeneratedRoutineExercise(
    val exerciseId: String,
    val setCount: Int = 3,
    val trackingFields: List<String> = listOf("WEIGHT", "REPS"),
)

/**
 * Validates parsed commands before execution. Returns null when invalid.
 */
object FlowCommandValidator {
    fun validate(command: FlowCommand): FlowCommandValidationResult {
        return when (command) {
            is FlowCommand.CreateTask -> {
                if (command.name.isBlank()) {
                    FlowCommandValidationResult.Invalid("Task name is required.")
                } else {
                    FlowCommandValidationResult.Valid(command)
                }
            }
            is FlowCommand.LogWater -> {
                if (command.amountMl <= 0) {
                    FlowCommandValidationResult.Invalid("Water amount must be positive.")
                } else {
                    FlowCommandValidationResult.Valid(command)
                }
            }
            is FlowCommand.AddExercise -> {
                if (command.exerciseId.isBlank()) {
                    FlowCommandValidationResult.Invalid("Exercise ID is required.")
                } else if (!command.exerciseId.startsWith("builtin:") && !command.exerciseId.startsWith("custom:")) {
                    FlowCommandValidationResult.Invalid("Exercise ID must be a catalogue or custom ID.")
                } else {
                    FlowCommandValidationResult.Valid(command)
                }
            }
            is FlowCommand.GetPreviousPerformance -> {
                if (command.exerciseId.isBlank()) {
                    FlowCommandValidationResult.Invalid("Exercise ID is required.")
                } else {
                    FlowCommandValidationResult.Valid(command)
                }
            }
            is FlowCommand.GetExerciseHistory -> {
                if (command.exerciseId.isBlank()) {
                    FlowCommandValidationResult.Invalid("Exercise ID is required.")
                } else {
                    FlowCommandValidationResult.Valid(command)
                }
            }
            is FlowCommand.CreateRoutine -> {
                val invalidExercise = command.days
                    .flatMap { it.exercises }
                    .firstOrNull { ex ->
                        ex.exerciseId.isBlank() ||
                            (!ex.exerciseId.startsWith("builtin:") && !ex.exerciseId.startsWith("custom:"))
                    }
                if (command.name.isBlank()) {
                    FlowCommandValidationResult.Invalid("Routine name is required.")
                } else if (invalidExercise != null) {
                    FlowCommandValidationResult.Invalid("Generated routines must use valid catalogue exercise IDs.")
                } else {
                    FlowCommandValidationResult.Valid(command)
                }
            }
            is FlowCommand.SetPreference -> {
                if (command.key.isBlank()) {
                    FlowCommandValidationResult.Invalid("Preference key is required.")
                } else {
                    FlowCommandValidationResult.Valid(command)
                }
            }
            else -> FlowCommandValidationResult.Valid(command)
        }
    }
}

sealed class FlowCommandValidationResult {
    data class Valid(val command: FlowCommand) : FlowCommandValidationResult()
    data class Invalid(val reason: String) : FlowCommandValidationResult()
}

object FlowCommandJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(command: FlowCommand): String = when (command) {
        is FlowCommand.CreateTask -> json.encodeToString(command)
        is FlowCommand.EditTask -> json.encodeToString(command)
        is FlowCommand.DeleteTask -> json.encodeToString(command)
        is FlowCommand.CompleteTask -> json.encodeToString(command)
        is FlowCommand.LogWater -> json.encodeToString(command)
        is FlowCommand.UndoWater -> json.encodeToString(command)
        is FlowCommand.GetWaterStatus -> json.encodeToString(command)
        is FlowCommand.CreateRoutine -> json.encodeToString(command)
        is FlowCommand.EditRoutine -> json.encodeToString(command)
        is FlowCommand.AddExercise -> json.encodeToString(command)
        is FlowCommand.RemoveExercise -> json.encodeToString(command)
        is FlowCommand.SaveRoutine -> json.encodeToString(command)
        is FlowCommand.StartWorkout -> json.encodeToString(command)
        is FlowCommand.GetPreviousPerformance -> json.encodeToString(command)
        is FlowCommand.GetExerciseHistory -> json.encodeToString(command)
        is FlowCommand.SetPreference -> json.encodeToString(command)
        is FlowCommand.GetPreference -> json.encodeToString(command)
    }

    fun decode(raw: String): FlowCommand? {
        val element = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: return null
        val type = element.jsonObject["type"]?.jsonPrimitive?.contentOrNull ?: return null
        return runCatching {
            when (type) {
                FlowCommand.CreateTask.TYPE -> json.decodeFromString<FlowCommand.CreateTask>(raw)
                FlowCommand.EditTask.TYPE -> json.decodeFromString<FlowCommand.EditTask>(raw)
                FlowCommand.DeleteTask.TYPE -> json.decodeFromString<FlowCommand.DeleteTask>(raw)
                FlowCommand.CompleteTask.TYPE -> json.decodeFromString<FlowCommand.CompleteTask>(raw)
                FlowCommand.LogWater.TYPE -> json.decodeFromString<FlowCommand.LogWater>(raw)
                FlowCommand.UndoWater.TYPE -> json.decodeFromString<FlowCommand.UndoWater>(raw)
                FlowCommand.GetWaterStatus.TYPE -> json.decodeFromString<FlowCommand.GetWaterStatus>(raw)
                FlowCommand.CreateRoutine.TYPE -> json.decodeFromString<FlowCommand.CreateRoutine>(raw)
                FlowCommand.EditRoutine.TYPE -> json.decodeFromString<FlowCommand.EditRoutine>(raw)
                FlowCommand.AddExercise.TYPE -> json.decodeFromString<FlowCommand.AddExercise>(raw)
                FlowCommand.RemoveExercise.TYPE -> json.decodeFromString<FlowCommand.RemoveExercise>(raw)
                FlowCommand.SaveRoutine.TYPE -> json.decodeFromString<FlowCommand.SaveRoutine>(raw)
                FlowCommand.StartWorkout.TYPE -> json.decodeFromString<FlowCommand.StartWorkout>(raw)
                FlowCommand.GetPreviousPerformance.TYPE -> json.decodeFromString<FlowCommand.GetPreviousPerformance>(raw)
                FlowCommand.GetExerciseHistory.TYPE -> json.decodeFromString<FlowCommand.GetExerciseHistory>(raw)
                FlowCommand.SetPreference.TYPE -> json.decodeFromString<FlowCommand.SetPreference>(raw)
                FlowCommand.GetPreference.TYPE -> json.decodeFromString<FlowCommand.GetPreference>(raw)
                else -> null
            }
        }.getOrNull()
    }
}

/** Future LLM preference keys (structured, not chat history). */
object FlowPreferenceKeys {
    const val DINNER_TIME = "DINNER_TIME"
    const val WAKE_TIME = "WAKE_TIME"
    const val BED_TIME = "BED_TIME"
}

/** Helper for future routine generation from catalogue metadata. */
object FlowCommandExerciseCatalog {
    fun resolveExerciseIdsForMuscles(
        muscles: Set<GymMuscleGroup>,
        equipment: Set<GymEquipment> = emptySet(),
        limit: Int = 20,
    ): List<String> {
        return com.deepak.flow.core.gym.GymBuiltinExerciseCatalog.all()
            .filter { exercise ->
                muscles.any { muscle ->
                    exercise.primaryMuscle == muscle || muscle in exercise.secondaryMuscles
                }
            }
            .filter { exercise ->
                equipment.isEmpty() || exercise.equipment in equipment
            }
            .take(limit)
            .map { it.id }
    }
}

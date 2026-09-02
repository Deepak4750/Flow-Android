package com.deepak.flow.core.command

import com.deepak.flow.core.gym.GymEquipment
import com.deepak.flow.core.gym.GymMuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowCommandValidatorTest {

    @Test
    fun validate_createTask_requiresName() {
        val result = FlowCommandValidator.validate(
            FlowCommand.CreateTask(name = ""),
        )
        assertTrue(result is FlowCommandValidationResult.Invalid)
    }

    @Test
    fun validate_logWater_requiresPositiveAmount() {
        val result = FlowCommandValidator.validate(
            FlowCommand.LogWater(amountMl = 0),
        )
        assertTrue(result is FlowCommandValidationResult.Invalid)
    }

    @Test
    fun validate_addExercise_requiresCatalogueId() {
        val result = FlowCommandValidator.validate(
            FlowCommand.AddExercise(exerciseId = "random_string"),
        )
        assertTrue(result is FlowCommandValidationResult.Invalid)
    }

    @Test
    fun validate_addExercise_acceptsBuiltinId() {
        val result = FlowCommandValidator.validate(
            FlowCommand.AddExercise(exerciseId = "builtin:barbell_bench_press"),
        )
        assertTrue(result is FlowCommandValidationResult.Valid)
    }

    @Test
    fun validate_createRoutine_rejectsInventedExerciseIds() {
        val result = FlowCommandValidator.validate(
            FlowCommand.CreateRoutine(
                name = "PPL",
                days = listOf(
                    GeneratedRoutineDay(
                        name = "Push",
                        exercises = listOf(
                            GeneratedRoutineExercise(exerciseId = "made_up_exercise"),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(result is FlowCommandValidationResult.Invalid)
    }

    @Test
    fun validate_createRoutine_acceptsCatalogueExerciseIds() {
        val result = FlowCommandValidator.validate(
            FlowCommand.CreateRoutine(
                name = "Push",
                days = listOf(
                    GeneratedRoutineDay(
                        name = "Day 1",
                        exercises = listOf(
                            GeneratedRoutineExercise(exerciseId = "builtin:barbell_bench_press"),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(result is FlowCommandValidationResult.Valid)
    }
}

class FlowCommandJsonTest {

    @Test
    fun encodeDecode_createTask() {
        val command = FlowCommand.CreateTask(
            name = "Take zinc tablets",
            frequency = "DAILY",
            anchor = "DINNER",
        )
        val encoded = FlowCommandJson.encode(command)
        val decoded = FlowCommandJson.decode(encoded)
        assertNotNull(decoded)
        assertTrue(decoded is FlowCommand.CreateTask)
        assertEquals("Take zinc tablets", (decoded as FlowCommand.CreateTask).name)
    }

    @Test
    fun encodeDecode_logWater() {
        val command = FlowCommand.LogWater(amountMl = 250)
        val encoded = FlowCommandJson.encode(command)
        val decoded = FlowCommandJson.decode(encoded)
        assertTrue(decoded is FlowCommand.LogWater)
        assertEquals(250, (decoded as FlowCommand.LogWater).amountMl)
    }
}

class FlowCommandExerciseCatalogTest {

    @Test
    fun resolveExerciseIdsForMuscles_returnsBuiltinIds() {
        val ids = FlowCommandExerciseCatalog.resolveExerciseIdsForMuscles(
            muscles = setOf(GymMuscleGroup.CHEST),
            equipment = setOf(GymEquipment.BARBELL),
            limit = 5,
        )
        assertTrue(ids.isNotEmpty())
        assertTrue(ids.all { it.startsWith("builtin:") })
    }
}

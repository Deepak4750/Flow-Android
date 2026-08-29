package com.deepak.flow.core.gym

import com.deepak.flow.feature.gym.presentation.dayDisplacementYSmooth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineBuilderReorderTest {

    @Test
    fun reorderListByMove_day4ToDay2() {
        val keys = listOf("d1", "d2", "d3", "d4", "d5")
        assertEquals(
            listOf("d1", "d4", "d2", "d3", "d5"),
            GymLogic.reorderListByMove(keys, fromIndex = 3, toIndex = 1),
        )
    }

    @Test
    fun reorderListByMove_day8ToDay4() {
        val keys = (1..8).map { "day$it" }
        assertEquals(
            listOf("day1", "day2", "day3", "day8", "day4", "day5", "day6", "day7"),
            GymLogic.reorderListByMove(keys, fromIndex = 7, toIndex = 3),
        )
    }

    @Test
    fun reorderListByMove_firstToLast() {
        val keys = listOf("a", "b", "c", "d")
        assertEquals(
            listOf("b", "c", "d", "a"),
            GymLogic.reorderListByMove(keys, fromIndex = 0, toIndex = 3),
        )
    }

    @Test
    fun reorderListByMove_lastToFirst() {
        val keys = listOf("a", "b", "c", "d")
        assertEquals(
            listOf("d", "a", "b", "c"),
            GymLogic.reorderListByMove(keys, fromIndex = 3, toIndex = 0),
        )
    }

    @Test
    fun reorderListByKey_secondDragMovesTouchedItem_notPreviousDrag() {
        val day1 = "day-1"
        val day2 = "day-2"
        val day3 = "day-3"
        val day4 = "day-4"
        var keys = listOf(day1, day2, day3, day4)

        keys = GymLogic.reorderListByKey(keys, movedKey = day3, toIndex = 1)
        assertEquals(listOf(day1, day3, day2, day4), keys)

        keys = GymLogic.reorderListByKey(keys, movedKey = day2, toIndex = 3)
        assertEquals(listOf(day1, day3, day4, day2), keys)
        assertEquals(day2, keys[3])
        assertEquals(day3, keys[1])
    }

    @Test
    fun reorderListByKey_sequentialFourStepChain() {
        val day1 = "k1"
        val day2 = "k2"
        val day3 = "k3"
        val day4 = "k4"
        var keys = listOf(day1, day2, day3, day4)

        keys = GymLogic.reorderListByKey(keys, movedKey = day3, toIndex = 1)
        keys = GymLogic.reorderListByKey(keys, movedKey = day2, toIndex = 3)
        keys = GymLogic.reorderListByKey(keys, movedKey = day4, toIndex = 0)
        keys = GymLogic.reorderListByKey(keys, movedKey = day1, toIndex = 2)

        assertEquals(listOf(day4, day3, day1, day2), keys)
    }

    @Test
    fun reorderListByKey_restDaySecondDrag() {
        val day1 = "w1"
        val day2 = "w2"
        val rest = "rest"
        val day4 = "w4"
        val day5 = "w5"
        var keys = listOf(day1, day2, rest, day4, day5)

        keys = GymLogic.reorderListByKey(keys, movedKey = rest, toIndex = 1)
        assertEquals(listOf(day1, rest, day2, day4, day5), keys)

        keys = GymLogic.reorderListByKey(keys, movedKey = day2, toIndex = 4)
        assertEquals(listOf(day1, rest, day4, day5, day2), keys)

        keys = GymLogic.reorderListByKey(keys, movedKey = rest, toIndex = 3)
        assertEquals(listOf(day1, day4, day5, rest, day2), keys)
    }

    @Test
    fun reorderDaysByKeys_secondSequentialReorderPreservesIdentity() {
        val days = listOf(
            day(key = "day-1", index = 0, name = "Day 1"),
            day(key = "day-2", index = 1, name = "Day 2"),
            day(key = "day-3", index = 2, name = "Day 3"),
            day(key = "day-4", index = 3, name = "Day 4"),
        )
        var keys = days.map { it.localKey }
        keys = GymLogic.reorderListByKey(keys, movedKey = "day-3", toIndex = 1)
        var reordered = GymLogic.reorderDaysByKeys(days, keys)!!
        keys = GymLogic.reorderListByKey(keys, movedKey = "day-2", toIndex = 3)
        reordered = GymLogic.reorderDaysByKeys(days, keys)!!

        assertEquals(listOf("day-1", "day-3", "day-4", "day-2"), reordered.map { it.localKey })
        assertEquals(listOf("Day 1", "Day 3", "Day 4", "Day 2"), reordered.map { it.name })
    }

    @Test
    fun reorderDaysByKeys_preservesStableIdentity() {
        val days = listOf(
            day(key = "101", index = 0, name = "A"),
            day(key = "102", index = 1, name = "B"),
            day(key = "103", index = 2, name = "C"),
            day(key = "104", index = 3, name = "D"),
        )
        val reordered = GymLogic.reorderDaysByKeys(
            days,
            orderedKeys = listOf("101", "104", "102", "103"),
        )!!

        assertEquals(listOf("101", "104", "102", "103"), reordered.map { it.localKey })
        assertEquals(listOf("A", "D", "B", "C"), reordered.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), reordered.map { it.dayIndex })
    }

    @Test
    fun reorderDaysByKeys_restDayCrossesWorkoutDay() {
        val days = listOf(
            day(key = "w1", index = 0, name = "Push", isRest = false),
            day(key = "r1", index = 1, name = "", isRest = true),
            day(key = "w2", index = 2, name = "Pull", isRest = false),
        )
        val reordered = GymLogic.reorderDaysByKeys(
            days,
            orderedKeys = listOf("r1", "w1", "w2"),
        )!!

        assertEquals(listOf("r1", "w1", "w2"), reordered.map { it.localKey })
        assertEquals(listOf(true, false, false), reordered.map { it.isRestDay })
        assertEquals(listOf(0, 1, 2), reordered.map { it.dayIndex })
    }

    @Test
    fun reorderDaysByKeys_exercisesStayAttachedToDay() {
        val exercise = GymRoutineExercise(
            stableKey = "ex1",
            name = "Bench Press",
            sortOrder = 0,
        )
        val days = listOf(
            day(key = "d1", index = 0, name = "A"),
            day(key = "d2", index = 1, name = "B", exercises = listOf(exercise)),
        )
        val reordered = GymLogic.reorderDaysByKeys(
            days,
            orderedKeys = listOf("d2", "d1"),
        )!!

        assertEquals("d2", reordered.first().localKey)
        assertEquals("Bench Press", reordered.first().exercises.single().name)
    }

    @Test
    fun reorderDaysByKeys_rejectsUnknownOrMismatchedKeys() {
        val days = listOf(day(key = "d1", index = 0, name = "A"))
        assertNull(GymLogic.reorderDaysByKeys(days, orderedKeys = emptyList()))
        assertNull(GymLogic.reorderDaysByKeys(days, orderedKeys = listOf("missing")))
    }

    @Test
    fun reorderDaysByKeys_simulatesSaveReloadOrder() {
        val days = (1..8).map { index ->
            day(key = "id$index", index = index - 1, name = "Day $index")
        }
        val afterDrag = GymLogic.reorderDaysByKeys(
            days,
            orderedKeys = GymLogic.reorderListByMove(days.map { it.localKey }, fromIndex = 7, toIndex = 3),
        )!!

        val persistedDayIndices = afterDrag.mapIndexed { index, day -> day.copy(dayIndex = index) }
        val reloaded = persistedDayIndices.sortedBy { it.dayIndex }

        assertEquals((1..8).map { "id$it" }.let {
            listOf(it[0], it[1], it[2], it[7], it[3], it[4], it[5], it[6])
        }, reloaded.map { it.localKey })
        assertEquals(
            listOf("Day 1", "Day 2", "Day 3", "Day 8", "Day 4", "Day 5", "Day 6", "Day 7"),
            reloaded.map { it.name },
        )
    }

    @Test
    fun dayDisplacementYSmooth_movesNeighborProportionallyBeforeBoundary() {
        val days = listOf(
            day(key = "d1", index = 0, name = "Day 1"),
            day(key = "d2", index = 1, name = "Day 2"),
            day(key = "d3", index = 2, name = "Day 3"),
        )
        val heights = mapOf("d1" to 80f, "d2" to 80f, "d3" to 80f)
        val gap = 20f
        val defaultHeight = 80f

        val dragged = dayDisplacementYSmooth(
            index = 1,
            fromIndex = 1,
            dragOffsetY = 50f,
            orderedDays = days,
            heights = heights,
            defaultHeightPx = defaultHeight,
            gapPx = gap,
        )
        assertEquals(50f, dragged, 0.001f)

        val neighbor = dayDisplacementYSmooth(
            index = 2,
            fromIndex = 1,
            dragOffsetY = 120f,
            orderedDays = days,
            heights = heights,
            defaultHeightPx = defaultHeight,
            gapPx = gap,
        )
        assertEquals(-20f, neighbor, 0.001f)
    }

    @Test
    fun dayDisplacementYSmooth_isSymmetricForUpwardDrag() {
        val days = listOf(
            day(key = "d1", index = 0, name = "Day 1"),
            day(key = "d2", index = 1, name = "Day 2"),
            day(key = "d3", index = 2, name = "Day 3"),
        )
        val heights = mapOf("d1" to 80f, "d2" to 80f, "d3" to 80f)
        val gap = 20f
        val defaultHeight = 80f

        val dragged = dayDisplacementYSmooth(
            index = 2,
            fromIndex = 2,
            dragOffsetY = -50f,
            orderedDays = days,
            heights = heights,
            defaultHeightPx = defaultHeight,
            gapPx = gap,
        )
        assertEquals(-50f, dragged, 0.001f)

        val neighbor = dayDisplacementYSmooth(
            index = 1,
            fromIndex = 2,
            dragOffsetY = -120f,
            orderedDays = days,
            heights = heights,
            defaultHeightPx = defaultHeight,
            gapPx = gap,
        )
        assertEquals(20f, neighbor, 0.001f)
    }

    private fun day(
        key: String,
        index: Int,
        name: String,
        isRest: Boolean = false,
        exercises: List<GymRoutineExercise> = emptyList(),
    ) = GymRoutineDay(
        dayIndex = index,
        name = name,
        isRestDay = isRest,
        localKey = key,
        exercises = exercises,
    )
}

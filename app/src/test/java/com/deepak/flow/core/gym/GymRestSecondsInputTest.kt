package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Test

class GymRestSecondsInputTest {

    private val min = GymLimits.SET_REST_MIN_SECONDS
    private val max = GymLimits.SET_REST_MAX_SECONDS

    @Test
    fun commit_empty_defaultsToMin() {
        assertEquals(10, GymRestSecondsInput.commit("", min, max))
    }

    @Test
    fun commit_45() {
        assertEquals(45, GymRestSecondsInput.commit("45", min, max))
    }

    @Test
    fun commit_60() {
        assertEquals(60, GymRestSecondsInput.commit("60", min, max))
    }

    @Test
    fun commit_belowMin_clampsToMin() {
        assertEquals(10, GymRestSecondsInput.commit("5", min, max))
        assertEquals(10, GymRestSecondsInput.commit("9", min, max))
        assertEquals(10, GymRestSecondsInput.commit("1", min, max))
    }

    @Test
    fun commit_atMin_staysMin() {
        assertEquals(10, GymRestSecondsInput.commit("10", min, max))
    }

    @Test
    fun commitScenario_45_clearBlurTo10() {
        assertEquals(10, GymRestSecondsInput.commit("", min, max))
    }

    @Test
    fun commitScenario_45_retypeBlurTo45() {
        var field = "45"
        field = GymRestSecondsInput.filterDigits("")
        assertEquals("", field)
        field = GymRestSecondsInput.filterDigits("45")
        assertEquals(45, GymRestSecondsInput.commit(field, min, max))
    }

    @Test
    fun commit_aboveMax_clampsToMax() {
        assertEquals(max, GymRestSecondsInput.commit("999", min, max))
    }

    @Test
    fun commit_existingValue_unchanged() {
        assertEquals(90, GymRestSecondsInput.commit("90", min, max))
    }

    @Test
    fun editingSequence_emptyTo45() {
        var field = ""
        field = GymRestSecondsInput.filterDigits("4")
        assertEquals("4", field)
        field = GymRestSecondsInput.filterDigits("45")
        assertEquals("45", field)
        assertEquals(45, GymRestSecondsInput.commit(field, min, max))
    }

    @Test
    fun editingSequence_45_backspaceTo4_then45() {
        var field = "45"
        field = GymRestSecondsInput.filterDigits("4")
        assertEquals("4", field)
        assertEquals(10, GymRestSecondsInput.commit(field, min, max))
        field = GymRestSecondsInput.filterDigits("45")
        assertEquals("45", field)
        assertEquals(45, GymRestSecondsInput.commit(field, min, max))
    }

    @Test
    fun editingSequence_emptyTo60() {
        var field = ""
        field = GymRestSecondsInput.filterDigits("6")
        field = GymRestSecondsInput.filterDigits("60")
        assertEquals("60", field)
        assertEquals(60, GymRestSecondsInput.commit(field, min, max))
    }
}

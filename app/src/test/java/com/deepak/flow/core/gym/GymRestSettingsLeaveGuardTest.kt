package com.deepak.flow.core.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymRestSettingsLeaveGuardTest {

    private val min = GymLimits.SET_REST_MIN_SECONDS
    private val max = GymLimits.SET_REST_MAX_SECONDS

    @Test
    fun setRest_empty_cannotLeave() {
        val result = GymRestSettingsLeaveGuard.validate("", "10", min, max)
        assertFalse(result.canLeave)
        assertEquals(GymRestSettingsField.SET_REST, result.invalidField)
    }

    @Test
    fun setRest_5_cannotLeave() {
        val result = GymRestSettingsLeaveGuard.validate("5", "10", min, max)
        assertFalse(result.canLeave)
        assertEquals(GymRestSettingsField.SET_REST, result.invalidField)
    }

    @Test
    fun setRest_9_cannotLeave() {
        val result = GymRestSettingsLeaveGuard.validate("9", "10", min, max)
        assertFalse(result.canLeave)
        assertEquals(GymRestSettingsField.SET_REST, result.invalidField)
    }

    @Test
    fun setRest_10_canLeave() {
        val result = GymRestSettingsLeaveGuard.validate("10", "10", min, max)
        assertTrue(result.canLeave)
    }

    @Test
    fun setRest_45_canLeave() {
        val result = GymRestSettingsLeaveGuard.validate("45", "10", min, max)
        assertTrue(result.canLeave)
    }

    @Test
    fun exerciseRest_empty_cannotLeave() {
        val result = GymRestSettingsLeaveGuard.validate("10", "", min, max)
        assertFalse(result.canLeave)
        assertEquals(GymRestSettingsField.EXERCISE_REST, result.invalidField)
    }

    @Test
    fun exerciseRest_5_cannotLeave() {
        val result = GymRestSettingsLeaveGuard.validate("10", "5", min, max)
        assertFalse(result.canLeave)
        assertEquals(GymRestSettingsField.EXERCISE_REST, result.invalidField)
    }

    @Test
    fun exerciseRest_10_canLeave() {
        val result = GymRestSettingsLeaveGuard.validate("10", "10", min, max)
        assertTrue(result.canLeave)
    }

    @Test
    fun bothValid_canLeave() {
        val result = GymRestSettingsLeaveGuard.validate("45", "60", min, max)
        assertTrue(result.canLeave)
    }
}

package com.deepak.flow.feature.gym.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class FreeWorkoutBackBehaviorTest {

    @Test
    fun resting_doesNotSkipRestOnBack() {
        assertEquals(FreeWorkoutBackEffect.None, freeWorkoutBackEffect(FreeWorkoutPhase.RESTING))
    }

    @Test
    fun session_opensEndOptionsOnBack() {
        assertEquals(FreeWorkoutBackEffect.OpenEndOptions, freeWorkoutBackEffect(FreeWorkoutPhase.SESSION))
    }
}

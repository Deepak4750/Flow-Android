package com.deepak.flow.feature.gym.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class GymActiveStepperDimensionsTest {

    @Test
    fun stepperTouchTargetRemains56dp() {
        assertEquals(56, GymActiveStepperTouchTargetDp.value.toInt())
    }

    @Test
    fun stepperIconSizeRemains28dp() {
        assertEquals(28, GymActiveStepperIconSizeDp.value.toInt())
    }
}

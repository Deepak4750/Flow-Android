package com.deepak.flow.core.gym

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymSetDraftPolicyTest {

    private val weightReps = setOf(TrackingField.WEIGHT, TrackingField.REPS)

    @Test
    fun emptyDraftHasNoUserEnteredData() {
        assertFalse(
            GymSetDraftPolicy.hasUserEnteredData(
                trackingFields = weightReps,
            ),
        )
    }

    @Test
    fun weightOnlyCountsAsEnteredData() {
        assertTrue(
            GymSetDraftPolicy.hasUserEnteredData(
                trackingFields = weightReps,
                weight = "10",
            ),
        )
    }

    @Test
    fun zeroWeightCountsAsEnteredData() {
        assertTrue(
            GymSetDraftPolicy.hasUserEnteredData(
                trackingFields = weightReps,
                weight = "0",
            ),
        )
    }

    @Test
    fun repsOnlyCountsAsEnteredData() {
        assertTrue(
            GymSetDraftPolicy.hasUserEnteredData(
                trackingFields = weightReps,
                reps = "10",
            ),
        )
    }

    @Test
    fun clearingFieldsRemovesEnteredData() {
        assertFalse(
            GymSetDraftPolicy.hasUserEnteredData(
                trackingFields = weightReps,
                weight = "",
                reps = "",
            ),
        )
    }
}

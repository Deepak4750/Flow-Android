package com.deepak.flow.core.notification

import com.deepak.flow.core.gym.GymWorkoutExercise
import com.deepak.flow.core.gym.GymWorkoutSession
import com.deepak.flow.core.gym.GymWorkoutType
import com.deepak.flow.core.widget.WidgetLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GymRestAlarmCoordinatorTest {

    private lateinit var alarms: FakeGymRestAlarmPort
    private lateinit var alerter: RecordingGymRestAlerter
    private lateinit var coordinator: GymRestAlarmCoordinator

    @Before
    fun setUp() {
        GymRestAlertDeduper.resetForTests()
        alarms = FakeGymRestAlarmPort()
        alerter = RecordingGymRestAlerter()
        coordinator = GymRestAlarmCoordinator(alarms, alerter)
    }

    @Test
    fun startRest_schedulesExactAlarmAtRestEndsAt() {
        val request = restRequest(endsAt = 90_000L)

        coordinator.onRestStarted(request, nowEpochMilli = 0L)

        assertEquals(listOf("cancel", "schedule:90000"), alarms.operations)
        assertEquals(request, alarms.scheduled)
    }

    @Test
    fun plusTen_reschedulesToNewEndTime() {
        coordinator.onRestStarted(restRequest(endsAt = 90_000L), nowEpochMilli = 0L)
        alarms.operations.clear()
        val extended = restRequest(endsAt = 100_000L)

        coordinator.onRestExtended(extended)

        assertEquals(listOf("cancel", "schedule:100000"), alarms.operations)
        assertEquals(extended, alarms.scheduled)
    }

    @Test
    fun minusTen_reschedulesToNewEndTime() {
        coordinator.onRestStarted(restRequest(endsAt = 90_000L), nowEpochMilli = 0L)
        alarms.operations.clear()
        val shortened = restRequest(endsAt = 80_000L)

        coordinator.onRestExtended(shortened)

        assertEquals(listOf("cancel", "schedule:80000"), alarms.operations)
        assertEquals(shortened, alarms.scheduled)
    }

    @Test
    fun skip_cancelsAlarmAndDoesNotAlert() {
        val request = restRequest(endsAt = 90_000L)
        coordinator.onRestStarted(request, nowEpochMilli = 0L)
        alarms.operations.clear()

        coordinator.onRestAbandoned()
        simulateAlarmFire(request, nowEpochMilli = 90_000L)

        assertEquals(listOf("cancel"), alarms.operations)
        assertNull(alarms.scheduled)
        assertEquals(0, alerter.signalCount)
    }

    @Test
    fun completeAndDiscard_cancelAlarm() {
        coordinator.onRestStarted(restRequest(endsAt = 90_000L), nowEpochMilli = 0L)
        coordinator.onRestAbandoned()
        assertNull(alarms.scheduled)

        coordinator.onRestStarted(restRequest(endsAt = 120_000L), nowEpochMilli = 30_000L)
        coordinator.onRestAbandoned()
        assertNull(alarms.scheduled)
        assertEquals(0, alerter.signalCount)
    }

    @Test
    fun naturalCompletion_leavesAlarmScheduledSoItCanAlertOnce() {
        val request = restRequest(endsAt = 90_000L)
        coordinator.onRestStarted(request, nowEpochMilli = 0L)

        // Clock advances the workout without cancelling. Alarm remains the alerter.
        assertEquals(request, alarms.scheduled)

        simulateAlarmFire(request, nowEpochMilli = 90_000L)
        simulateAlarmFire(request, nowEpochMilli = 90_250L)
        // Old clock path must not add a second alert.
        simulateAlarmFire(request, nowEpochMilli = 90_500L)

        assertEquals(1, alerter.signalCount)
        assertEquals(request, alarms.scheduled)
    }

    @Test
    fun clockAndAlarm_cannotDoubleAlert() {
        val request = restRequest(endsAt = 60_000L)
        coordinator.onRestStarted(request, nowEpochMilli = 0L)

        simulateAlarmFire(request, nowEpochMilli = 60_000L)
        simulateAlarmFire(request, nowEpochMilli = 60_000L)

        assertEquals(1, alerter.signalCount)
    }

    @Test
    fun startingNextRestAfterDueRest_flushesOneAlertThenReschedules() {
        val first = restRequest(endsAt = 60_000L)
        coordinator.onRestStarted(first, nowEpochMilli = 0L)
        val next = restRequest(endsAt = 150_000L)

        coordinator.onRestStarted(next, nowEpochMilli = 60_100L)

        assertEquals(1, alerter.signalCount)
        assertEquals(next, alarms.scheduled)
    }

    @Test
    fun startRest_doesNotSignalNewRest() {
        val request = restRequest(endsAt = 90_000L)

        coordinator.onRestStarted(request, nowEpochMilli = 0L)

        assertEquals(0, alerter.signalCount)
    }

    @Test
    fun alarmFire_beforeScheduledCompletion_doesNotAlert() {
        val request = restRequest(endsAt = 60_000L)

        simulateAlarmFire(request, nowEpochMilli = 59_500L)

        assertEquals(0, alerter.signalCount)
        assertFalse(GymRestAlarm.shouldAlert(59_500L, 60_000L))
        assertTrue(GymRestAlarm.shouldAlert(60_000L, 60_000L))
    }

    @Test
    fun destination_matchesWorkoutType() {
        assertEquals(
            WidgetLaunch.DEST_GYM_FREE_WORKOUT,
            GymRestAlarmRequest.destinationFor(GymWorkoutType.FREE),
        )
        assertEquals(
            WidgetLaunch.DEST_GYM_ROUTINE_WORKOUT,
            GymRestAlarmRequest.destinationFor(GymWorkoutType.ROUTINE),
        )
    }

    @Test
    fun requestFromSession_snapshotsExerciseNameAndDestination() {
        val session = GymWorkoutSession(
            id = 9L,
            type = GymWorkoutType.ROUTINE,
            startedAtEpochMilli = 1L,
            currentExerciseIndex = 1,
            exercises = listOf(
                GymWorkoutExercise(id = 1L, workoutId = 9L, name = "Bench", sortOrder = 0),
                GymWorkoutExercise(id = 2L, workoutId = 9L, name = "Row", sortOrder = 1),
            ),
        )

        val request = GymRestAlarmRequest.from(session, restEndsAtEpochMilli = 5_000L)

        assertEquals(9L, request.workoutId)
        assertEquals(5_000L, request.restEndsAtEpochMilli)
        assertEquals("Row", request.exerciseName)
        assertEquals(WidgetLaunch.DEST_GYM_ROUTINE_WORKOUT, request.destination)
    }

    private fun simulateAlarmFire(request: GymRestAlarmRequest, nowEpochMilli: Long) {
        if (!GymRestAlarm.shouldAlert(nowEpochMilli, request.restEndsAtEpochMilli)) return
        alerter.signal(request)
    }

    private fun restRequest(endsAt: Long) = GymRestAlarmRequest(
        workoutId = 42L,
        restEndsAtEpochMilli = endsAt,
        exerciseName = "Lat Pulldown",
        destination = WidgetLaunch.DEST_GYM_FREE_WORKOUT,
    )

    private class FakeGymRestAlarmPort : GymRestAlarmPort {
        val operations = mutableListOf<String>()
        var scheduled: GymRestAlarmRequest? = null

        override fun schedule(request: GymRestAlarmRequest) {
            operations += "schedule:${request.restEndsAtEpochMilli}"
            scheduled = request
        }

        override fun cancel() {
            operations += "cancel"
            scheduled = null
        }
    }

    private class RecordingGymRestAlerter : GymRestAlerterPort {
        var signalCount: Int = 0
            private set

        override fun signal(request: GymRestAlarmRequest) {
            if (!GymRestAlertDeduper.tryClaim(request.workoutId, request.restEndsAtEpochMilli)) {
                return
            }
            signalCount++
        }

        override fun suppress(request: GymRestAlarmRequest) {
            GymRestAlertDeduper.suppress(request.workoutId, request.restEndsAtEpochMilli)
        }
    }
}

class GymRestAlertDeduperTest {

    @Before
    fun setUp() {
        GymRestAlertDeduper.resetForTests()
    }

    @Test
    fun firstClaimWins_secondIsIgnored() {
        assertTrue(GymRestAlertDeduper.tryClaim(1L, 100L))
        assertFalse(GymRestAlertDeduper.tryClaim(1L, 100L))
        assertTrue(GymRestAlertDeduper.tryClaim(1L, 200L))
    }

    @Test
    fun suppress_blocksLaterClaim() {
        GymRestAlertDeduper.suppress(7L, 50L)
        assertFalse(GymRestAlertDeduper.tryClaim(7L, 50L))
        assertTrue(GymRestAlertDeduper.tryClaim(7L, 51L))
    }
}

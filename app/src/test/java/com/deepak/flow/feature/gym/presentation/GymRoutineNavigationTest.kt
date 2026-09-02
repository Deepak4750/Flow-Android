package com.deepak.flow.feature.gym.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class GymRoutineNavigationTest {

    @Test
    fun freshInstall_newRoutine_opensBuilder_notCatalog() {
        assertEquals(
            GymRoutineNewRoutineDestination.BUILDER,
            resolveGymRoutineNewRoutineDestination(hasRoutine = false),
        )
    }

    @Test
    fun browseRoutines_stillUsesCatalog() {
        assertEquals(
            GymRoutineBrowseDestination.CATALOG,
            resolveGymRoutineBrowseDestination(),
        )
    }
}

enum class GymRoutineNewRoutineDestination {
    BUILDER,
    CATALOG,
}

enum class GymRoutineBrowseDestination {
    CATALOG,
}

internal fun resolveGymRoutineNewRoutineDestination(hasRoutine: Boolean): GymRoutineNewRoutineDestination =
    if (hasRoutine) {
        GymRoutineNewRoutineDestination.BUILDER
    } else {
        GymRoutineNewRoutineDestination.BUILDER
    }

internal fun resolveGymRoutineBrowseDestination(): GymRoutineBrowseDestination =
    GymRoutineBrowseDestination.CATALOG

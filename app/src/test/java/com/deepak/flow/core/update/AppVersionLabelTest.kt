package com.deepak.flow.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionLabelTest {

    @Test
    fun stableShowsVersionNameAndCode() {
        assertEquals(
            "v1.3.3 (181)",
            formatInstalledVersionLabel(versionName = "1.3.3", versionCode = 181),
        )
    }

    @Test
    fun versionCodeIsDynamicNotHardcoded() {
        val label = formatInstalledVersionLabel(versionName = "1.3.3", versionCode = 205)
        assertTrue(label.contains("1.3.3"))
        assertTrue(label.endsWith("(205)"))
    }

    @Test
    fun betaShowsIterationAndVersionCode() {
        assertEquals(
            "v1.3.3 Beta (1) (181)",
            formatInstalledVersionLabel(
                versionName = "1.3.3",
                versionCode = 181,
                betaIteration = 1,
            ),
        )
    }

    @Test
    fun betaShowsHigherIterationsWithVersionCode() {
        assertEquals(
            "v1.3.3 Beta (4) (999)",
            formatInstalledVersionLabel(
                versionName = "1.3.3",
                versionCode = 999,
                betaIteration = 4,
            ),
        )
    }

    @Test
    fun zeroBetaIterationIsStableWithVersionCode() {
        assertEquals(
            "v1.3.3 (204)",
            formatInstalledVersionLabel(
                versionName = "1.3.3",
                versionCode = 204,
                betaIteration = 0,
            ),
        )
    }
}

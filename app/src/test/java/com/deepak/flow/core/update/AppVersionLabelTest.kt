package com.deepak.flow.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionLabelTest {

    @Test
    fun showsVersionNameOnly() {
        assertEquals(
            "1.3.0",
            formatInstalledVersionLabel(versionName = "1.3.0", versionCode = 171),
        )
    }
}

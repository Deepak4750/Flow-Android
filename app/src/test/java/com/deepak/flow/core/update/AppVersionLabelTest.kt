package com.deepak.flow.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionLabelTest {

    @Test
    fun showsNameCodeAndBeta() {
        assertEquals(
            "1.1.1 (69) Beta",
            formatInstalledVersionLabel(versionName = "1.1.1", versionCode = 69),
        )
    }
}

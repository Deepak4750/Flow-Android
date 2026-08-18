package com.deepak.flow.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionLabelTest {

    @Test
    fun publicChannel_showsPlainVersion() {
        assertEquals("1.0.8", formatInstalledVersionLabel("1.0.8", previewEnabled = false))
    }

    @Test
    fun previewChannel_appendsBeta() {
        assertEquals("1.0.8 Beta", formatInstalledVersionLabel("1.0.8", previewEnabled = true))
    }
}

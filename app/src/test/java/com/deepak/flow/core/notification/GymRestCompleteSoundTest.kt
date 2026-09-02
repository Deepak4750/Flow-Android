package com.deepak.flow.core.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class GymRestCompleteSoundTest {

    @Test
    fun `sound resource uri string uses android resource scheme`() {
        val uri = GymRestCompleteSound.soundResourceUriString(
            packageName = "com.deepak.flow",
            rawResId = 0x7f100001,
        )
        assertEquals("android.resource://com.deepak.flow/${0x7f100001}", uri)
    }
}

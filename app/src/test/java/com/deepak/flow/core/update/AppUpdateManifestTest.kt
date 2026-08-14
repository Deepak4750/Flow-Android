package com.deepak.flow.core.update

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManifestTest {

    @Test
    fun parseManifestJson() {
        val json = """
            {"versionCode":4,"versionName":"1.0.1","apkUrl":"https://example.com/flow.apk","notes":"Ready."}
        """.trimIndent()
        val manifest = Json.decodeFromString(AppUpdateManifest.serializer(), json)
        assertEquals(4, manifest.versionCode)
        assertEquals("1.0.1", manifest.versionName)
        assertEquals("https://example.com/flow.apk", manifest.apkUrl)
    }

    @Test
    fun isNewerThan_comparesVersionCode() {
        val manifest = AppUpdateManifest(
            versionCode = 5,
            versionName = "1.0.2",
            apkUrl = "https://example.com/flow.apk",
        )
        assertTrue(manifest.isNewerThan(4))
        assertFalse(manifest.isNewerThan(5))
        assertFalse(manifest.isNewerThan(6))
    }
}

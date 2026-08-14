package com.deepak.flow.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateManifestUrlsTest {

    @Test
    fun forPreview_selectsPreviewManifest() {
        assertEquals(UpdateManifestUrls.PREVIEW, UpdateManifestUrls.forPreview(true))
    }

    @Test
    fun forPreview_selectsReleaseManifestByDefault() {
        assertEquals(UpdateManifestUrls.RELEASE, UpdateManifestUrls.forPreview(false))
    }

    @Test
    fun releaseUrl_pointsAtLatestJson() {
        assertEquals(
            "https://raw.githubusercontent.com/Deepak4750/Flow-Releases/main/latest.json",
            UpdateManifestUrls.RELEASE,
        )
    }

    @Test
    fun previewUrl_pointsAtPreviewJson() {
        assertEquals(
            "https://raw.githubusercontent.com/Deepak4750/Flow-Releases/main/preview.json",
            UpdateManifestUrls.PREVIEW,
        )
    }
}

package com.deepak.flow.core.update

import android.content.Context

/**
 * Chooses which public manifest Flow checks.
 *
 * Friends stay on [UpdateManifestUrls.RELEASE]. Preview is unlocked on one
 * device (tap the version in About seven times) so test builds can land there
 * before [UpdateManifestUrls.RELEASE] is updated.
 */
class UpdateChannel(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var previewEnabled: Boolean
        get() = prefs.getBoolean(KEY_PREVIEW, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PREVIEW, value).apply()
        }

    fun manifestUrl(): String = UpdateManifestUrls.forPreview(previewEnabled)

    companion object {
        private const val PREFS = "flow_updates"
        private const val KEY_PREVIEW = "preview_enabled"
    }
}

object UpdateManifestUrls {
    const val RELEASE =
        "https://raw.githubusercontent.com/Deepak4750/Flow-Releases/main/latest.json"
    const val PREVIEW =
        "https://raw.githubusercontent.com/Deepak4750/Flow-Releases/main/preview.json"

    fun forPreview(enabled: Boolean): String = if (enabled) PREVIEW else RELEASE
}

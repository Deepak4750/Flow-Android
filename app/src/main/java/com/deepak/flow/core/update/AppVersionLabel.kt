package com.deepak.flow.core.update

fun formatInstalledVersionLabel(versionName: String, previewEnabled: Boolean): String =
    if (previewEnabled) "$versionName Beta" else versionName

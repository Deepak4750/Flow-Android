package com.deepak.flow.core.update

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String = "",
)

fun AppUpdateManifest.isNewerThan(installedVersionCode: Int): Boolean =
    versionCode > installedVersionCode

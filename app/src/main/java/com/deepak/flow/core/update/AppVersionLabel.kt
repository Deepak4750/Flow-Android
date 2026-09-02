package com.deepak.flow.core.update

/**
 * User-visible installed version string for Settings / About.
 *
 * [betaIteration] is display-only (from [com.deepak.flow.BuildConfig.FLOW_BETA_ITERATION]).
 * It is not [versionCode] and must not be used for OTA comparison.
 */
fun formatInstalledVersionLabel(
    versionName: String,
    versionCode: Int,
    betaIteration: Int = 0,
): String {
    val base = "v${versionName.removePrefix("v")}"
    val withBeta = if (betaIteration <= 0) base else "$base Beta ($betaIteration)"
    return "$withBeta ($versionCode)"
}

package com.deepak.flow.core.update

import com.deepak.flow.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

class AppUpdateRepository(
    private val manifestUrl: String = DEFAULT_MANIFEST_URL,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetchManifest(): Result<AppUpdateManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val body = httpGet(manifestUrl)
            json.decodeFromString(AppUpdateManifest.serializer(), body)
        }
    }

    suspend fun downloadApk(url: String, destination: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            destination.parentFile?.mkdirs()
            if (destination.exists()) destination.delete()
            httpDownload(url, destination)
            require(destination.length() > MIN_APK_BYTES) { "Download was empty" }
            destination
        }
    }

    private fun httpGet(url: String): String {
        val connection = open(url)
        try {
            val code = connection.responseCode
            require(code in 200..299) { "Update check failed ($code)" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun httpDownload(url: String, destination: File) {
        val connection = open(url)
        try {
            val code = connection.responseCode
            require(code in 200..299) { "Download failed ($code)" }
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true
        connection.useCaches = false
        connection.setRequestProperty("User-Agent", "Flow/${BuildConfig.VERSION_NAME}")
        connection.setRequestProperty("Cache-Control", "no-cache")
        return connection
    }

    companion object {
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/Deepak4750/Flow-Releases/main/latest.json"
        private const val MIN_APK_BYTES = 50_000L
    }
}

package com.deepak.flow.core.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deepak.flow.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class AppUpdateStatus {
    Idle,
    Checking,
    UpToDate,
    Available,
    Downloading,
    NeedsPermission,
    Failed,
}

data class AppUpdateUiState(
    val status: AppUpdateStatus = AppUpdateStatus.Idle,
    val available: AppUpdateManifest? = null,
    val promptVisible: Boolean = false,
    val downloadedApk: File? = null,
    val previewEnabled: Boolean = false,
    val previewUnlocked: Boolean = false,
)

class AppUpdateViewModel(
    application: Application,
    private val channel: UpdateChannel = UpdateChannel(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        AppUpdateUiState(
            previewEnabled = channel.previewEnabled,
            previewUnlocked = channel.previewEnabled,
        ),
    )
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    init {
        checkQuietly()
    }

    private fun repository(): AppUpdateRepository =
        AppUpdateRepository(channel.manifestUrl())

    fun setPreviewEnabled(enabled: Boolean) {
        channel.previewEnabled = enabled
        _uiState.update {
            it.copy(
                previewEnabled = enabled,
                previewUnlocked = it.previewUnlocked || enabled,
                status = AppUpdateStatus.Idle,
                available = null,
                promptVisible = false,
            )
        }
        if (enabled) {
            checkQuietly()
        }
    }

    fun unlockPreviewControls() {
        _uiState.update { it.copy(previewUnlocked = true) }
    }

    fun checkQuietly() {
        viewModelScope.launch {
            if (_uiState.value.status == AppUpdateStatus.Downloading) return@launch
            val manifest = repository().fetchManifest().getOrNull() ?: return@launch
            if (!manifest.isNewerThan(BuildConfig.VERSION_CODE)) return@launch
            _uiState.update {
                it.copy(
                    status = AppUpdateStatus.Available,
                    available = manifest,
                    promptVisible = true,
                )
            }
        }
    }

    fun onAppOpened() {
        checkQuietly()
    }

    fun checkNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = AppUpdateStatus.Checking) }
            val result = repository().fetchManifest()
            val manifest = result.getOrNull()
            when {
                manifest == null -> _uiState.update {
                    it.copy(status = AppUpdateStatus.Failed, promptVisible = false)
                }
                manifest.isNewerThan(BuildConfig.VERSION_CODE) -> _uiState.update {
                    it.copy(
                        status = AppUpdateStatus.Available,
                        available = manifest,
                        promptVisible = true,
                    )
                }
                else -> _uiState.update {
                    it.copy(
                        status = AppUpdateStatus.UpToDate,
                        available = null,
                        promptVisible = false,
                    )
                }
            }
        }
    }

    fun dismissPrompt() {
        _uiState.update { it.copy(promptVisible = false) }
    }

    fun installAvailableUpdate() {
        val manifest = _uiState.value.available ?: return
        val context = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(status = AppUpdateStatus.Downloading) }
            val destination = File(File(context.cacheDir, "updates"), "Flow-update.apk")
            val downloaded = repository().downloadApk(manifest.apkUrl, destination).getOrNull()
            if (downloaded == null) {
                _uiState.update { it.copy(status = AppUpdateStatus.Failed) }
                return@launch
            }
            if (!AppUpdateInstaller.canInstallPackages(context)) {
                _uiState.update {
                    it.copy(
                        status = AppUpdateStatus.NeedsPermission,
                        downloadedApk = downloaded,
                        promptVisible = true,
                    )
                }
                AppUpdateInstaller.requestInstallPermission(context)
                return@launch
            }
            _uiState.update {
                it.copy(
                    status = AppUpdateStatus.Available,
                    downloadedApk = downloaded,
                    promptVisible = false,
                )
            }
            AppUpdateInstaller.installApk(context, downloaded)
        }
    }

    fun retryInstallAfterPermission() {
        val apk = _uiState.value.downloadedApk ?: return
        val context = getApplication<Application>()
        if (!AppUpdateInstaller.canInstallPackages(context)) return
        _uiState.update { it.copy(status = AppUpdateStatus.Available, promptVisible = false) }
        AppUpdateInstaller.installApk(context, apk)
    }
}

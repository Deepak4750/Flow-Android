package com.deepak.flow.core.repository

import com.deepak.flow.core.database.UserProfileDao
import com.deepak.flow.core.database.UserProfileEntity
import com.deepak.flow.core.model.SnoozeSettings
import com.deepak.flow.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val dao: UserProfileDao,
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        dao.observeProfile().map { it?.toDomain() }

    override suspend fun getProfile(): UserProfile? =
        dao.getProfile()?.toDomain()

    override fun isOnboardingComplete(): Flow<Boolean> =
        dao.observeProfile().map { it?.onboardingCompleted == true }

    override suspend fun completeOnboarding(displayName: String?, nickname: String?) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                nickname = nickname?.trim()?.takeIf { it.isNotEmpty() },
                onboardingCompleted = true,
            ),
        )
    }

    override suspend fun updateProfile(displayName: String?, nickname: String?) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                nickname = nickname?.trim()?.takeIf { it.isNotEmpty() },
                onboardingCompleted = existing?.onboardingCompleted ?: true,
            ),
        )
    }

    override suspend fun updateSnoozeEnabled(enabled: Boolean) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                snoozeEnabled = enabled,
            ),
        )
    }

    override suspend fun updateSnoozeInterval(minutes: Int) {
        val existing = dao.getProfile()
        dao.upsert(
            existing.toUpsertEntity(
                displayName = existing?.displayName,
                nickname = existing?.nickname,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                snoozeIntervalMinutes = SnoozeSettings.coerceIntervalMinutes(minutes),
            ),
        )
    }
}

private fun UserProfileEntity?.toUpsertEntity(
    displayName: String?,
    nickname: String?,
    onboardingCompleted: Boolean,
    snoozeEnabled: Boolean = this?.snoozeEnabled ?: SnoozeSettings.DEFAULT_ENABLED,
    snoozeIntervalMinutes: Int = this?.snoozeIntervalMinutes
        ?: SnoozeSettings.DEFAULT_INTERVAL_MINUTES,
) = UserProfileEntity(
    displayName = displayName,
    nickname = nickname,
    onboardingCompleted = onboardingCompleted,
    snoozeEnabled = snoozeEnabled,
    snoozeIntervalMinutes = snoozeIntervalMinutes,
)

private fun UserProfileEntity.toDomain() = UserProfile(
    displayName = displayName,
    nickname = nickname,
    onboardingCompleted = onboardingCompleted,
    snoozeEnabled = snoozeEnabled,
    snoozeIntervalMinutes = snoozeIntervalMinutes,
)

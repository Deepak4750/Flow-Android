package com.deepak.flow.core.repository

import com.deepak.flow.core.database.UserProfileDao
import com.deepak.flow.core.database.UserProfileEntity
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
        dao.upsert(
            UserProfileEntity(
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                nickname = nickname?.trim()?.takeIf { it.isNotEmpty() },
                onboardingCompleted = true,
            ),
        )
    }

    override suspend fun updateProfile(displayName: String?, nickname: String?) {
        val onboardingCompleted = dao.getProfile()?.onboardingCompleted ?: true
        dao.upsert(
            UserProfileEntity(
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                nickname = nickname?.trim()?.takeIf { it.isNotEmpty() },
                onboardingCompleted = onboardingCompleted,
            ),
        )
    }
}

private fun UserProfileEntity.toDomain() = UserProfile(
    displayName = displayName,
    nickname = nickname,
    onboardingCompleted = onboardingCompleted,
)

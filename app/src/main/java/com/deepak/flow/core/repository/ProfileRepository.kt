package com.deepak.flow.core.repository

import com.deepak.flow.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>
    suspend fun getProfile(): UserProfile?
    fun isOnboardingComplete(): Flow<Boolean>
    suspend fun completeOnboarding(displayName: String?, nickname: String?)
    suspend fun updateProfile(displayName: String?, nickname: String?)
}

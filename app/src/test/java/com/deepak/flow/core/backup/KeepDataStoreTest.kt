package com.deepak.flow.core.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepDataStoreTest {

    @Test
    fun writesKeepCopyOnlyAfterOnboardingWhenEnabled() {
        assertTrue(KeepDataStore.shouldWriteKeepCopy(keepEnabled = true, onboardingCompleted = true))
        assertFalse(KeepDataStore.shouldWriteKeepCopy(keepEnabled = true, onboardingCompleted = false))
        assertFalse(KeepDataStore.shouldWriteKeepCopy(keepEnabled = false, onboardingCompleted = true))
        assertFalse(KeepDataStore.shouldWriteKeepCopy(keepEnabled = false, onboardingCompleted = false))
    }

    @Test
    fun restoresBackupOnlyForMatchingAndroidUser() {
        assertTrue(KeepDataStore.shouldRestoreBackup(metaOwnerUserId = 0, currentUserId = 0))
        assertFalse(KeepDataStore.shouldRestoreBackup(metaOwnerUserId = 0, currentUserId = 10))
    }
}

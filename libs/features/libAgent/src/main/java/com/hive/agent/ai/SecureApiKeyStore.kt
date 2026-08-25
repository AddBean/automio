// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import android.content.Context
import com.hive.utils.security.SecureCredentialStore

/** Stores provider credentials encrypted by a non-exportable Android Keystore key. */
internal class SecureApiKeyStore(context: Context) {
    private val primary = SecureCredentialStore(
        context,
        SecureCredentialStore.DEFAULT_PREFS_NAME,
        SecureCredentialStore.DEFAULT_KEY_ALIAS,
    )
    private val legacy = SecureCredentialStore(
        context,
        SecureCredentialStore.AI_LEGACY_PREFS_NAME,
        SecureCredentialStore.AI_LEGACY_KEY_ALIAS,
    )

    fun get(providerId: String): String? {
        primary.get(providerId)?.let { return it }
        val migrated = legacy.get(providerId) ?: return null
        primary.put(providerId, migrated)
        legacy.remove(providerId)
        return migrated
    }

    fun put(providerId: String, value: String) {
        primary.put(providerId, value)
        legacy.remove(providerId)
    }

    fun remove(providerId: String) {
        primary.remove(providerId)
        legacy.remove(providerId)
    }
}

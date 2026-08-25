// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts small secrets with a non-exportable Android Keystore AES key.
 * Preferred storage for API keys / speech credentials (do not commit plaintext into the repo).
 */
class SecureCredentialStore(
    context: Context,
    prefsName: String = DEFAULT_PREFS_NAME,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) {
    private val preferences = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun get(id: String): String? {
        val encoded = preferences.getString(id, null) ?: return null
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLength = payload.first().toInt() and 0xff
            val iv = payload.copyOfRange(1, 1 + ivLength)
            val encrypted = payload.copyOfRange(1 + ivLength, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    fun put(id: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
        preferences.edit().putString(id, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    fun remove(id: String) {
        preferences.edit().remove(id).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    companion object {
        const val DEFAULT_PREFS_NAME = "automio_secure_credentials"
        const val DEFAULT_KEY_ALIAS = "automio_secure_credentials_v1"
        /** Legacy AI-only store alias — keep decrypting existing installs. */
        const val AI_LEGACY_PREFS_NAME = "secure_ai_credentials"
        const val AI_LEGACY_KEY_ALIAS = "automio_ai_credentials_v1"

        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

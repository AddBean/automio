// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.base.core

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Reads and writes the AES format used by historical encrypted workflows.
 *
 * The format is AES-128-CBC with PKCS#5 padding and lowercase hexadecimal output.
 * Keeping it here avoids retaining the old JNI signature and crash library solely for file
 * compatibility.
 */
object LegacyScriptCipher {
    private const val DEFAULT_KEY = "ed5fdsgucxumegqa"
    private val iv = DEFAULT_KEY.toByteArray(StandardCharsets.UTF_8)

    fun encrypt(value: String, key: String = DEFAULT_KEY): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(key), IvParameterSpec(iv))
        return cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8)).toHex()
    }

    fun decrypt(value: String, key: String = DEFAULT_KEY): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(key), IvParameterSpec(iv))
        return String(cipher.doFinal(value.hexToBytes()), StandardCharsets.UTF_8)
    }

    private fun secretKey(key: String): SecretKeySpec {
        val bytes = key.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size == 16) { "Legacy workflow encryption keys must be 16 bytes" }
        return SecretKeySpec(bytes, "AES")
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Invalid legacy workflow ciphertext" }
        return ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}

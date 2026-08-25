// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import com.hive.script.base.core.LegacyScriptCipher
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyScriptCipherTest {

    @Test
    fun decryptsFixedLegacyDefaultKeySample() {
        val ciphertext = "2039b071fdb63cd145a5512a5542edf4"

        assertEquals("hex68656C6C6F", LegacyScriptCipher.decrypt(ciphertext))
    }

    @Test
    fun decryptsFixedLegacyCustomKeySample() {
        val ciphertext = "0b881d173de1a1ec570649b76fcdaa2425ec4c7b620bee8a31cdde300f2e670d"

        assertEquals("hex776F726B666C6F77", LegacyScriptCipher.decrypt(ciphertext, "b3d5302674bd5f66"))
    }
}

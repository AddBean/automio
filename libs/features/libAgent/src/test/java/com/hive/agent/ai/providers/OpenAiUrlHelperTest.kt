// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai.providers

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAiUrlHelperTest {

    @Test
    fun `root base URL receives v1 models path`() {
        assertEquals(
            "https://example.com/v1/models",
            OpenAiUrlHelper.modelsUrl("https://example.com/")
        )
    }

    @Test
    fun `versioned base URL receives models path directly`() {
        assertEquals(
            "https://example.com/v1/models",
            OpenAiUrlHelper.modelsUrl("https://example.com/v1")
        )
    }

    @Test
    fun `existing models URL is unchanged`() {
        assertEquals(
            "https://example.com/api/models",
            OpenAiUrlHelper.modelsUrl("https://example.com/api/models")
        )
    }
}

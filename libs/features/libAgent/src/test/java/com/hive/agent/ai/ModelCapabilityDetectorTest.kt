// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCapabilityDetectorTest {

    @Test
    fun `detects common vision model families`() {
        val capabilities = ModelCapabilityDetector.detect("qwen2.5-vl-72b-instruct")

        assertTrue(capabilities.supportsVision)
        assertTrue(capabilities.supportsFunctionCall)
    }

    @Test
    fun `does not mark embedding and reranker models as tool capable`() {
        assertFalse(ModelCapabilityDetector.detect("text-embedding-3-large").supportsFunctionCall)
        assertFalse(ModelCapabilityDetector.detect("bge-reranker-v2-m3").supportsFunctionCall)
    }

    @Test
    fun `uses known context windows when model family is recognizable`() {
        assertEquals(1_000_000, ModelCapabilityDetector.detect("gemini-2.5-pro").contextWindow)
        assertEquals(200_000, ModelCapabilityDetector.detect("claude-3-7-sonnet").contextWindow)
        assertEquals(128_000, ModelCapabilityDetector.detect("gpt-4.1-mini").contextWindow)
    }

    @Test
    fun `keeps generic chat models conservative`() {
        val capabilities = ModelCapabilityDetector.detect("private-chat-model")

        assertFalse(capabilities.supportsVision)
        assertTrue(capabilities.supportsFunctionCall)
        assertEquals(0, capabilities.contextWindow)
    }
}

// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import com.hive.plugin.agent.ModelCapabilities
import com.hive.plugin.agent.ModelInfo
import com.hive.plugin.agent.ModelType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceModelSelectionResolverTest {

    private val selected = model("deepseek-chat", "DeepSeek V3", vision = false)
    private val visionSelected = model("qwen-vl", "Qwen VL", vision = true)

    @Test
    fun `null selection is not set`() = runBlocking {
        val status = InferenceModelSelectionResolver.resolve(
            selected = null,
            requireVision = false,
            providerExists = { true },
            providerEnabled = { true },
            providerReady = { true },
            findInCatalog = { selected }
        )
        assertEquals(InferenceModelSelectionKind.NOT_SET, status.kind)
        assertNull(status.model)
        assertFalse(status.hasSelection)
    }

    @Test
    fun `provider not ready keeps selection as needs config`() = runBlocking {
        val status = InferenceModelSelectionResolver.resolve(
            selected = selected,
            requireVision = false,
            providerExists = { true },
            providerEnabled = { true },
            providerReady = { false },
            findInCatalog = { error("should not fetch") }
        )
        assertEquals(InferenceModelSelectionKind.NEEDS_CONFIG, status.kind)
        assertEquals(selected, status.model)
        assertTrue(status.hasSelection)
        assertFalse(status.countsAsConfiguredVisionModel)
    }

    @Test
    fun `catalog miss keeps selection as refresh failed`() = runBlocking {
        val status = InferenceModelSelectionResolver.resolve(
            selected = selected,
            requireVision = false,
            providerExists = { true },
            providerEnabled = { true },
            providerReady = { true },
            findInCatalog = { null }
        )
        assertEquals(InferenceModelSelectionKind.REFRESH_FAILED, status.kind)
        assertEquals("DeepSeek V3", status.displayName)
        assertTrue(status.hasSelection)
    }

    @Test
    fun `catalog throw keeps selection as refresh failed`() = runBlocking {
        val status = InferenceModelSelectionResolver.resolve(
            selected = selected,
            requireVision = false,
            providerExists = { true },
            providerEnabled = { true },
            providerReady = { true },
            findInCatalog = { error("network") }
        )
        assertEquals(InferenceModelSelectionKind.REFRESH_FAILED, status.kind)
        assertEquals(selected.modelId, status.model?.modelId)
    }

    @Test
    fun `ready returns refreshed catalog model`() = runBlocking {
        val refreshed = selected.copy(displayName = "DeepSeek V3 Fresh")
        val status = InferenceModelSelectionResolver.resolve(
            selected = selected,
            requireVision = false,
            providerExists = { true },
            providerEnabled = { true },
            providerReady = { true },
            findInCatalog = { refreshed }
        )
        assertEquals(InferenceModelSelectionKind.READY, status.kind)
        assertEquals("DeepSeek V3 Fresh", status.displayName)
        assertTrue(status.countsAsConfiguredVisionModel)
    }

    @Test
    fun `vision slot rejects non vision without clearing selection`() = runBlocking {
        val status = InferenceModelSelectionResolver.resolve(
            selected = selected,
            requireVision = true,
            providerExists = { true },
            providerEnabled = { true },
            providerReady = { true },
            findInCatalog = { selected }
        )
        assertEquals(InferenceModelSelectionKind.INVALID_FOR_TYPE, status.kind)
        assertEquals(selected.modelId, status.model?.modelId)
        assertFalse(status.countsAsConfiguredVisionModel)
    }

    @Test
    fun `vision ready model counts as configured`() = runBlocking {
        val status = InferenceModelSelectionResolver.resolve(
            selected = visionSelected,
            requireVision = true,
            providerExists = { true },
            providerEnabled = { true },
            providerReady = { true },
            findInCatalog = { visionSelected }
        )
        assertEquals(InferenceModelSelectionKind.READY, status.kind)
        assertTrue(status.countsAsConfiguredVisionModel)
    }

    private fun model(id: String, name: String, vision: Boolean) = ModelInfo(
        modelId = id,
        displayName = name,
        providerId = "deepseek",
        buildIn = true,
        capabilities = ModelCapabilities(
            supportsFunctionCall = true,
            supportsVision = vision,
            contextWindow = 128000,
            modelType = ModelType.CHAT
        )
    )
}

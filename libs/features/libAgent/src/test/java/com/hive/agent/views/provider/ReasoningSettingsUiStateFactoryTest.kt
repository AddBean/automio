// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.views.provider

import com.hive.plugin.agent.ReasoningAvailability
import com.hive.plugin.agent.ReasoningCapabilities
import com.hive.plugin.agent.model.ReasoningEffort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningSettingsUiStateFactoryTest {

    private val allEfforts = setOf(
        ReasoningEffort.LOW,
        ReasoningEffort.MEDIUM,
        ReasoningEffort.HIGH
    )

    @Test
    fun `OPTIONAL shows global saved values and is interactive`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.HIGH,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = allEfforts,
                defaultEffort = ReasoningEffort.MEDIUM
            )
        )

        assertTrue(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertEquals(ReasoningSwitchHint.OPTIONAL, state.switchHint)
        assertTrue(state.effortRowVisible)
        assertTrue(state.effortRowEnabled)
        assertEquals(ReasoningEffort.HIGH, state.selectedEffort)
        assertEquals(allEfforts, state.supportedEfforts)
        assertTrue(state.canPersistSwitch)
        assertTrue(state.canPersistEffort)
    }

    @Test
    fun `OPTIONAL off still shows global effort and remains interactive`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = false,
            savedEffort = ReasoningEffort.LOW,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = allEfforts,
                defaultEffort = ReasoningEffort.MEDIUM
            )
        )

        assertFalse(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertEquals(ReasoningEffort.LOW, state.selectedEffort)
        assertFalse(state.effortRowVisible)
        assertFalse(state.effortRowEnabled)
        assertTrue(state.canPersistSwitch)
        assertFalse(state.canPersistEffort)
    }

    @Test
    fun `OPTIONAL on shows subordinate effort row`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.MEDIUM,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = allEfforts,
                defaultEffort = ReasoningEffort.MEDIUM
            )
        )
        assertTrue(state.effortRowVisible)
        assertTrue(state.canPersistEffort)
    }

    @Test
    fun `REQUIRED forces switch on disabled with always-on hint`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = false,
            savedEffort = ReasoningEffort.MEDIUM,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.REQUIRED,
                supportedEfforts = allEfforts,
                defaultEffort = ReasoningEffort.MEDIUM
            )
        )

        assertTrue(state.switchChecked)
        assertFalse(state.switchEnabled)
        assertEquals(ReasoningSwitchHint.REQUIRED, state.switchHint)
        assertFalse(state.canPersistSwitch)
        assertTrue(state.effortRowVisible)
        assertTrue(state.effortRowEnabled)
        assertTrue(state.canPersistEffort)
        assertEquals(ReasoningEffort.MEDIUM, state.selectedEffort)
    }

    @Test
    fun `UNSUPPORTED forces switch off disabled`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.HIGH,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.UNSUPPORTED
            )
        )

        assertFalse(state.switchChecked)
        assertFalse(state.switchEnabled)
        assertEquals(ReasoningSwitchHint.UNSUPPORTED, state.switchHint)
        assertFalse(state.effortRowVisible)
        assertFalse(state.effortRowEnabled)
        assertFalse(state.canPersistSwitch)
        assertFalse(state.canPersistEffort)
    }

    @Test
    fun `UNKNOWN keeps global preference editable with unknown hint`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.LOW,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.UNKNOWN
            )
        )

        assertTrue(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertEquals(ReasoningSwitchHint.UNKNOWN, state.switchHint)
        assertTrue(state.effortRowVisible)
        assertTrue(state.canPersistSwitch)
        assertTrue(state.canPersistEffort)
        assertEquals(ReasoningEffort.LOW, state.selectedEffort)
    }

    @Test
    fun `UNKNOWN off hides effort row`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = false,
            savedEffort = ReasoningEffort.MEDIUM,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.UNKNOWN
            )
        )
        assertFalse(state.switchChecked)
        assertFalse(state.effortRowVisible)
        assertFalse(state.canPersistEffort)
    }

    @Test
    fun `null capabilities treated as UNKNOWN editable preference`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.MEDIUM,
            capabilities = null
        )

        assertTrue(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertEquals(ReasoningSwitchHint.UNKNOWN, state.switchHint)
        assertTrue(state.effortRowVisible)
        assertTrue(state.canPersistSwitch)
    }

    @Test
    fun `no model selected still allows editing global preference`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.LOW,
            capabilities = null,
            modelSelected = false
        )

        assertTrue(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertEquals(ReasoningSwitchHint.NO_MODEL, state.switchHint)
        assertTrue(state.effortRowVisible)
        assertTrue(state.canPersistSwitch)
        assertTrue(state.canPersistEffort)
        assertEquals(ReasoningEffort.LOW, state.selectedEffort)
    }

    @Test
    fun `no model with switch off hides effort`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = false,
            savedEffort = ReasoningEffort.MEDIUM,
            capabilities = null,
            modelSelected = false
        )
        assertFalse(state.effortRowVisible)
    }

    @Test
    fun `empty supportedEfforts hides effort row for OPTIONAL`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.HIGH,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = emptySet(),
                defaultEffort = null
            )
        )

        assertTrue(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertFalse(state.effortRowVisible)
        assertFalse(state.effortRowEnabled)
        assertFalse(state.canPersistEffort)
    }

    @Test
    fun `empty supportedEfforts hides effort row for REQUIRED`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = false,
            savedEffort = ReasoningEffort.LOW,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.REQUIRED,
                supportedEfforts = emptySet()
            )
        )

        assertTrue(state.switchChecked)
        assertFalse(state.switchEnabled)
        assertFalse(state.effortRowVisible)
        assertFalse(state.canPersistEffort)
    }

    @Test
    fun `unsupported saved effort falls back to default without mutating semantics`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.HIGH,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = setOf(ReasoningEffort.LOW, ReasoningEffort.MEDIUM),
                defaultEffort = ReasoningEffort.LOW
            )
        )

        assertEquals(ReasoningEffort.LOW, state.selectedEffort)
        assertEquals(
            setOf(ReasoningEffort.LOW, ReasoningEffort.MEDIUM),
            state.supportedEfforts
        )
        assertTrue(state.canPersistEffort)
    }

    @Test
    fun `model switch refreshes UI from same saved globals`() {
        val savedEnabled = true
        val savedEffort = ReasoningEffort.HIGH

        val optional = ReasoningSettingsUiStateFactory.create(
            savedEnabled = savedEnabled,
            savedEffort = savedEffort,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = allEfforts,
                defaultEffort = ReasoningEffort.MEDIUM
            )
        )
        val unsupported = ReasoningSettingsUiStateFactory.create(
            savedEnabled = savedEnabled,
            savedEffort = savedEffort,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.UNSUPPORTED
            )
        )
        val required = ReasoningSettingsUiStateFactory.create(
            savedEnabled = savedEnabled,
            savedEffort = savedEffort,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.REQUIRED,
                supportedEfforts = allEfforts,
                defaultEffort = ReasoningEffort.MEDIUM
            )
        )
        val backToOptional = ReasoningSettingsUiStateFactory.create(
            savedEnabled = savedEnabled,
            savedEffort = savedEffort,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = allEfforts,
                defaultEffort = ReasoningEffort.MEDIUM
            )
        )

        assertTrue(optional.switchChecked)
        assertEquals(ReasoningEffort.HIGH, optional.selectedEffort)

        assertFalse(unsupported.switchChecked)
        assertFalse(unsupported.switchEnabled)

        assertTrue(required.switchChecked)
        assertFalse(required.switchEnabled)
        assertEquals(ReasoningEffort.HIGH, required.selectedEffort)

        // Same globals again → OPTIONAL restores interactive global values
        assertTrue(backToOptional.switchChecked)
        assertTrue(backToOptional.switchEnabled)
        assertEquals(ReasoningEffort.HIGH, backToOptional.selectedEffort)
        assertEquals(optional, backToOptional)
    }

    @Test
    fun `partial efforts only expose supported set`() {
        val state = ReasoningSettingsUiStateFactory.create(
            savedEnabled = true,
            savedEffort = ReasoningEffort.MEDIUM,
            capabilities = ReasoningCapabilities(
                availability = ReasoningAvailability.OPTIONAL,
                supportedEfforts = setOf(ReasoningEffort.HIGH),
                defaultEffort = ReasoningEffort.HIGH
            )
        )

        assertEquals(setOf(ReasoningEffort.HIGH), state.supportedEfforts)
        assertEquals(ReasoningEffort.HIGH, state.selectedEffort)
        assertTrue(state.effortRowVisible)
    }
}

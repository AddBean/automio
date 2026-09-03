// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.agent.ai

import com.google.gson.JsonObject
import com.hive.plugin.agent.model.ReasoningEffort
import com.hive.plugin.agent.model.ReasoningOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasoningRequestMapperTest {

    private fun baseRequest(
        temperature: Float = 0.7f,
        maxTokens: Int = 2000
    ): JsonObject = JsonObject().apply {
        addProperty("model", "test-model")
        addProperty("temperature", temperature)
        addProperty("max_tokens", maxTokens)
        addProperty("stream", false)
    }

    @Test
    fun `null options never injects reasoning fields`() {
        val dialects = ReasoningWireDialect.entries
        dialects.forEach { dialect ->
            val json = baseRequest()
            ReasoningRequestMapper.apply(json, dialect, null)
            assertFalse(dialect.name, json.has("reasoning_effort"))
            assertFalse(dialect.name, json.has("reasoning"))
            assertFalse(dialect.name, json.has("thinking"))
            assertFalse(dialect.name, json.has("enable_thinking"))
            assertFalse(dialect.name, json.has("thinking_budget"))
            assertTrue(dialect.name, json.has("temperature"))
            assertTrue(dialect.name, json.has("max_tokens"))
        }
    }

    @Test
    fun `openai maps off low med high and reasoning models swap token field`() {
        fun map(options: ReasoningOptions): JsonObject {
            val json = baseRequest()
            ReasoningRequestMapper.apply(json, ReasoningWireDialect.OPENAI, options)
            return json
        }

        assertEquals("none", map(ReasoningOptions(false, ReasoningEffort.HIGH))["reasoning_effort"].asString)
        assertEquals("low", map(ReasoningOptions(true, ReasoningEffort.LOW))["reasoning_effort"].asString)
        assertEquals("medium", map(ReasoningOptions(true, ReasoningEffort.MEDIUM))["reasoning_effort"].asString)
        assertEquals("high", map(ReasoningOptions(true, ReasoningEffort.HIGH))["reasoning_effort"].asString)

        val on = map(ReasoningOptions(true, ReasoningEffort.MEDIUM))
        assertFalse(on.has("temperature"))
        assertFalse(on.has("max_tokens"))
        assertEquals(2000, on["max_completion_tokens"].asInt)

        val off = map(ReasoningOptions(false, ReasoningEffort.MEDIUM))
        assertFalse(off.has("temperature"))
        assertEquals(2000, off["max_completion_tokens"].asInt)
    }

    @Test
    fun `openrouter maps enabled effort exclude false`() {
        fun map(options: ReasoningOptions): JsonObject {
            val json = baseRequest()
            ReasoningRequestMapper.apply(json, ReasoningWireDialect.OPENROUTER, options)
            return json["reasoning"].asJsonObject
        }

        val off = map(ReasoningOptions(false, ReasoningEffort.HIGH))
        assertFalse(off["enabled"].asBoolean)
        assertEquals("high", off["effort"].asString)
        assertFalse(off["exclude"].asBoolean)

        val on = map(ReasoningOptions(true, ReasoningEffort.LOW))
        assertTrue(on["enabled"].asBoolean)
        assertEquals("low", on["effort"].asString)
        assertFalse(on["exclude"].asBoolean)
    }

    @Test
    fun `deepseek maps thinking type and omits sampling when enabled`() {
        fun map(options: ReasoningOptions): JsonObject {
            val json = baseRequest()
            ReasoningRequestMapper.apply(json, ReasoningWireDialect.DEEPSEEK, options)
            return json
        }

        val off = map(ReasoningOptions(false, ReasoningEffort.MEDIUM))
        assertEquals("disabled", off["thinking"].asJsonObject["type"].asString)
        assertTrue(off.has("temperature"))

        val on = map(ReasoningOptions(true, ReasoningEffort.HIGH))
        assertEquals("enabled", on["thinking"].asJsonObject["type"].asString)
        assertEquals("high", on["reasoning_effort"].asString)
        assertFalse(on.has("temperature"))
    }

    @Test
    fun `bailian never sends both reasoning_effort and thinking_budget`() {
        fun map(options: ReasoningOptions): JsonObject {
            val json = baseRequest()
            ReasoningRequestMapper.apply(json, ReasoningWireDialect.BAILIAN, options)
            return json
        }

        val off = map(ReasoningOptions(false, ReasoningEffort.LOW))
        assertFalse(off["enable_thinking"].asBoolean)
        assertFalse(off.has("thinking_budget"))
        assertFalse(off.has("reasoning_effort"))

        val low = map(ReasoningOptions(true, ReasoningEffort.LOW))
        assertTrue(low["enable_thinking"].asBoolean)
        assertEquals(4096, low["thinking_budget"].asInt)
        assertFalse(low.has("reasoning_effort"))

        val med = map(ReasoningOptions(true, ReasoningEffort.MEDIUM))
        assertEquals(16384, med["thinking_budget"].asInt)
        assertFalse(med.has("reasoning_effort"))

        val high = map(ReasoningOptions(true, ReasoningEffort.HIGH))
        assertTrue(high["enable_thinking"].asBoolean)
        assertFalse(high.has("thinking_budget"))
        assertFalse(high.has("reasoning_effort"))
    }

    @Test
    fun `kimi maps thinking and omits temperature`() {
        fun map(options: ReasoningOptions): JsonObject {
            val json = baseRequest()
            ReasoningRequestMapper.apply(json, ReasoningWireDialect.KIMI, options)
            return json
        }

        assertEquals("disabled", map(ReasoningOptions(false))["thinking"].asJsonObject["type"].asString)
        assertFalse(map(ReasoningOptions(false)).has("temperature"))

        val on = map(ReasoningOptions(true, ReasoningEffort.MEDIUM))
        assertEquals("enabled", on["thinking"].asJsonObject["type"].asString)
        assertFalse(on.has("temperature"))
    }

    @Test
    fun `siliconflow maps enable_thinking with clamped budgets`() {
        fun map(options: ReasoningOptions): JsonObject {
            val json = baseRequest()
            ReasoningRequestMapper.apply(json, ReasoningWireDialect.SILICONFLOW, options)
            return json
        }

        val off = map(ReasoningOptions(false, ReasoningEffort.HIGH))
        assertFalse(off["enable_thinking"].asBoolean)
        assertFalse(off.has("thinking_budget"))

        assertEquals(4096, map(ReasoningOptions(true, ReasoningEffort.LOW))["thinking_budget"].asInt)
        assertEquals(16384, map(ReasoningOptions(true, ReasoningEffort.MEDIUM))["thinking_budget"].asInt)
        assertEquals(32768, map(ReasoningOptions(true, ReasoningEffort.HIGH))["thinking_budget"].asInt)
        assertTrue(map(ReasoningOptions(true, ReasoningEffort.HIGH))["enable_thinking"].asBoolean)
    }

    @Test
    fun `none dialect never sends control params even when options present`() {
        val json = baseRequest()
        ReasoningRequestMapper.apply(
            json,
            ReasoningWireDialect.NONE,
            ReasoningOptions(true, ReasoningEffort.HIGH)
        )
        assertFalse(json.has("reasoning_effort"))
        assertFalse(json.has("reasoning"))
        assertFalse(json.has("thinking"))
        assertFalse(json.has("enable_thinking"))
        assertTrue(json.has("temperature"))
    }

    @Test
    fun `provider dialect lookup routes minimax stepfun to none`() {
        assertEquals(ReasoningWireDialect.NONE, ReasoningModelCatalog.wireDialectFor("minimax"))
        assertEquals(ReasoningWireDialect.NONE, ReasoningModelCatalog.wireDialectFor("stepfun"))
        assertEquals(ReasoningWireDialect.BAILIAN, ReasoningModelCatalog.wireDialectFor("bailian_code"))
        assertEquals(ReasoningWireDialect.OPENAI, ReasoningModelCatalog.wireDialectFor("openai"))
        assertEquals(ReasoningWireDialect.NONE, ReasoningModelCatalog.wireDialectFor("custom_openai"))
        assertEquals(ReasoningWireDialect.NONE, ReasoningModelCatalog.wireDialectFor("openai_custom"))
    }
}

package com.hive.script.mcp.tools

import com.hive.plugin.mcp.model.ActionResult
import com.hive.plugin.mcp.model.McpAction
import com.hive.plugin.mcp.model.McpActionParameters
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdVoiceInteract
import com.hive.script.extensions.executeSync
import com.hive.script.mcp.CheckActionResult
import com.hive.script.mcp.McpToolBuilder
import com.hive.script.mcp.toLongCompat
import com.hive.utils.GlobalApp
import com.hive.utils.utils.StringUtils

@AutoMcpToolsRegister(MCP_IDS.ToolVoiceInteract)
class ScriptToolBuilder_CmdVoiceInteract : McpToolBuilder() {

    private var cmd: CmdVoiceInteract? = null

    override fun matchAction(actionName: String): Boolean {
        return "voiceInteract" == actionName
    }

    override fun getAction(): McpAction =
        McpAction(
            action = "voiceInteract",
            extraName = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_name),
            description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_description),
            paramInfo =
                mutableListOf(
                    McpActionParameters(
                        name = "mode",
                        type = "string",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_mode_desc),
                        required = true,
                        examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_mode_examples)),
                    ),
                    McpActionParameters(
                        name = "ttsText",
                        type = "string",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_tts_text_desc),
                        required = false,
                        examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_tts_text_examples)),
                    ),
                    McpActionParameters(
                        name = "preferLanguage",
                        type = "string",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_prefer_language_desc),
                        required = false,
                        examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_prefer_language_examples)),
                    ),
                    McpActionParameters(
                        name = "timeoutMs",
                        type = "number",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_timeout_desc),
                        required = false,
                        examples = listOf("15000"),
                    ),
                    McpActionParameters(
                        name = "listenStopMode",
                        type = "string",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_listen_stop_mode_desc),
                        required = false,
                        examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_listen_stop_mode_examples)),
                    ),
                    McpActionParameters(
                        name = "listenDurationMs",
                        type = "number",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_listen_duration_desc),
                        required = false,
                        examples = listOf("600000"),
                    ),
                    McpActionParameters(
                        name = "silenceStopMs",
                        type = "number",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_silence_stop_desc),
                        required = false,
                        examples = listOf("1200"),
                    ),
                    McpActionParameters(
                        name = "title",
                        type = "string",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_title_desc),
                        required = false,
                        examples = listOf(GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_title_examples)),
                    ),
                    McpActionParameters(
                        name = "showUi",
                        type = "boolean",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_show_ui_desc),
                        required = false,
                        examples = listOf("true", "false"),
                    ),
                    McpActionParameters(
                        name = "keepAudio",
                        type = "boolean",
                        description = GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_keep_audio_desc),
                        required = false,
                        examples = listOf("false", "true"),
                    )
                ),
            paramValues = emptyMap(),
        )

    override fun onCheckAction(action: McpAction): CheckActionResult {
        if (action.paramValues.isEmpty()) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_dialog_confirm_error_params_empty)
            )
        }

        val mode = action.paramValues["mode"]?.trim()?.lowercase()
        if (mode.isNullOrBlank()) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_error_mode_empty)
            )
        }
        if (mode != CmdVoiceInteract.MODE_TTS &&
            mode != CmdVoiceInteract.MODE_ASR &&
            mode != CmdVoiceInteract.MODE_TTS_ASR
        ) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_error_mode_invalid)
            )
        }

        if ((mode == CmdVoiceInteract.MODE_TTS || mode == CmdVoiceInteract.MODE_TTS_ASR) &&
            action.paramValues["ttsText"].isNullOrBlank()
        ) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_error_tts_text_empty)
            )
        }

        val listenStopMode = action.paramValues["listenStopMode"]?.trim()?.lowercase()
        if (!listenStopMode.isNullOrBlank() &&
            listenStopMode != CmdVoiceInteract.LISTEN_STOP_MODE_AUTO &&
            listenStopMode != CmdVoiceInteract.LISTEN_STOP_MODE_DURATION
        ) {
            return CheckActionResult(
                false,
                GlobalApp.getString(com.hive.i8n.R.string.tool_voice_interact_error_listen_stop_mode_invalid)
            )
        }

        return CheckActionResult(true, null)
    }

    override fun getCommand(): ScriptCommand? = cmd

    override fun withScreenLayout() = false

    override fun supportDelay() = false

    override fun onCreateCommand(params: Map<String, String>): ScriptCommand? {
        val mode = params["mode"]?.trim()?.lowercase() ?: CmdVoiceInteract.MODE_TTS
        val ttsText = params["ttsText"]?.takeIf { it.isNotBlank() }?.let { StringUtils.encoding(it) }
        val preferLanguage = params["preferLanguage"]
        val timeoutMs = params["timeoutMs"].toLongCompat(15000L)
        val listenStopMode = params["listenStopMode"]?.trim()?.lowercase() ?: CmdVoiceInteract.LISTEN_STOP_MODE_AUTO
        val listenDurationMs = params["listenDurationMs"].toLongCompat(0L)
        val silenceStopMs = params["silenceStopMs"].toLongCompat(1200L)
        val title = params["title"]
        val showUi = params["showUi"]?.toBooleanStrictOrNull() ?: true
        // Speech keys resolve from SecureCredentialStore / local.properties — never via MCP params.
        val targetParamId = params["output"]?.takeIf { it.isNotBlank() }
        val keepAudio = params["keepAudio"]?.toBooleanStrictOrNull() ?: false

        cmd = CmdVoiceInteract.createCommand(
            mode = mode,
            ttsText = ttsText,
            preferLanguage = preferLanguage,
            timeoutMs = timeoutMs,
            listenStopMode = listenStopMode,
            listenDurationMs = listenDurationMs,
            silenceStopMs = silenceStopMs,
            title = title,
            showUi = showUi,
            appKey = null,
            region = null,
            targetParamId = targetParamId,
            keepAudio = keepAudio
        )
        return cmd
    }
}

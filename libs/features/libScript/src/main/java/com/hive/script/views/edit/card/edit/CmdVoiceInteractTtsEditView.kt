package com.hive.script.views.edit.card.edit

import android.content.Context
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdVoiceInteract
import com.hive.script.views.widgets.ScriptSpanParamLayout
import com.hive.utils.utils.StringUtils

class CmdVoiceInteractTtsEditView(context: Context) : BaseCommandEditCard(context) {

    var cmd: CmdVoiceInteract? = null
    private var editTtsText: ScriptSpanParamLayout? = null

    override fun initView() {
        editTtsText = findViewById(R.id.edit_tts_text)
        editTtsText?.addTextChangedListener(object : ScriptSpanParamLayout.ScriptTextWatcher {
            override fun afterTextChanged(s: String?) {
                cmd?.ttsText = if (s.isNullOrBlank()) "" else StringUtils.encoding(s)
            }
        })
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdVoiceInteract
        editTtsText?.setText(StringUtils.decoding(cmd?.ttsText ?: ""))
    }

    override fun checkCommandOrThrowError() {
        if ((cmd?.ttsText ?: "").isBlank()) {
            throw IllegalArgumentException(context.getString(com.hive.i8n.R.string.tool_voice_interact_error_tts_text_empty))
        }
    }

    override fun getEditContentId() = R.layout.cmd_voice_interact_tts_card
}

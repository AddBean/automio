package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.params.ScriptParam
import com.hive.script.cmd.CmdVoiceInteract
import com.hive.script.utils.ScriptCommandHelper
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.widgets.ScriptFloatView
import com.hive.script.views.widgets.ScriptTabSelectorView
import com.hive.script.views.widgets.ScriptValueView
import com.hive.views.widgets.FloatOptView
import com.hive.views.widgets.SelectorTabView

class CmdVoiceInteractAsrEditView(context: Context) : BaseCommandEditCard(context) {

    var cmd: CmdVoiceInteract? = null
    private var targetParam: ScriptValueView? = null
    private var listenStopMode: ScriptTabSelectorView? = null
    private var listenDuration: ScriptFloatView? = null
    private var silenceStop: ScriptFloatView? = null

    override fun initView() {
        targetParam = findViewById(R.id.target_param)
        listenStopMode = findViewById(R.id.listen_stop_mode)
        listenDuration = findViewById(R.id.listen_duration)
        silenceStop = findViewById(R.id.silence_stop)

        targetParam?.onMaskClickListener = OnClickListener {
            DialogParamsManager(context)
                .setWritable(true)
                .setParamListener(object : DialogParamsManager.OnParamListener {
                    override fun onParamSelected(param: ScriptParam?) {
                        cmd?.targetParamId = param?.getFullId() ?: ""
                        cmd?.let { onBindCommand(it) }
                    }
                }).show()
        }
        listenStopMode?.onTabSelectedChangedListener = object : SelectorTabView.OnTabSelectedChangedListener {
            override fun onSelectedChanged(p: Pair<String?, String?>?) {
                cmd?.listenStopMode = p?.second ?: CmdVoiceInteract.LISTEN_STOP_MODE_AUTO
                updateListenFieldsVisibility()
            }
        }
        listenDuration?.changedListener = FloatOptView.OnValueChangedListener { v ->
            cmd?.listenDurationMs = v.toLong()
        }
        silenceStop?.changedListener = FloatOptView.OnValueChangedListener { v ->
            cmd?.silenceStopMs = v.toLong()
        }
    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdVoiceInteract
        targetParam?.setValue(ScriptCommandHelper.paramFormat.format(cmd?.targetParamId ?: ""))
        listenStopMode?.setValue(cmd?.listenStopMode ?: CmdVoiceInteract.LISTEN_STOP_MODE_AUTO)
        listenDuration?.setNumber(((cmd?.listenDurationMs ?: (10 * 1000L))).toFloat())
        silenceStop?.setNumber((cmd?.silenceStopMs ?: 1200L).toFloat())
        updateListenFieldsVisibility()
    }

    private fun updateListenFieldsVisibility() {
        val isDurationMode = cmd?.listenStopMode == CmdVoiceInteract.LISTEN_STOP_MODE_DURATION
        listenDuration?.visibleOrGone(isDurationMode)
        silenceStop?.visibleOrGone(!isDurationMode)
    }

    override fun checkCommandOrThrowError() {
        if (cmd?.targetParamId.isNullOrBlank()) {
            throw IllegalArgumentException(context.getString(com.hive.i8n.R.string.sc_cmd_select_param_title) + " required")
        }
        if (cmd?.listenStopMode == CmdVoiceInteract.LISTEN_STOP_MODE_DURATION) {
            val duration = listenDuration?.getNumber()?.toLong() ?: 0L
            if (duration <= 0) {
                throw IllegalArgumentException(context.getString(com.hive.i8n.R.string.sc_edit_voice_listen_duration) + " > 0")
            }
        }
    }

    override fun getEditContentId() = R.layout.cmd_voice_interact_asr_card
}

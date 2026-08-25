// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import android.content.Context
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdAiRequest
import com.hive.script.cmd.CmdPatternTap
import com.hive.script.cmd.CmdClick
import com.hive.script.cmd.CmdClickColor
import com.hive.script.cmd.CmdClickImage
import com.hive.script.cmd.CmdClickText
import com.hive.script.cmd.CmdClickView
import com.hive.script.cmd.CmdCopyToClipboard
import com.hive.script.cmd.CmdCurl
import com.hive.script.cmd.CmdDelay
import com.hive.script.cmd.CmdDownload
import com.hive.script.cmd.CmdPythonExecutor
import com.hive.script.cmd.CmdRepeatTap
import com.hive.script.cmd.CmdFor
import com.hive.script.cmd.CmdIf
import com.hive.script.cmd.CmdInput
import com.hive.script.cmd.CmdJump
import com.hive.script.cmd.CmdCallScript
import com.hive.script.cmd.CmdRunSkill
import com.hive.script.cmd.CmdPinch
import com.hive.script.cmd.CmdOpenApp
import com.hive.script.cmd.CmdOpenUrl
import com.hive.script.cmd.CmdPlayAudio
import com.hive.script.cmd.CmdPress
import com.hive.script.cmd.CmdReadScreenText
import com.hive.script.cmd.CmdReadViewText
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.cmd.CmdScriptEnd
import com.hive.script.cmd.CmdScriptStart
import com.hive.script.cmd.CmdScroll
import com.hive.script.cmd.CmdScrollMultiple
import com.hive.script.cmd.CmdSet
import com.hive.script.cmd.CmdAlignToSecond
import com.hive.script.cmd.CmdToast
import com.hive.script.cmd.CmdVoiceInteract
import com.hive.script.views.edit.card.BaseCommandCard
import com.hive.script.views.edit.card.edit.BaseCommandEditCard
import com.hive.script.views.edit.card.edit.CmdAiRequestEditView
import com.hive.script.views.edit.card.edit.CmdBatchClickEditView
import com.hive.script.views.edit.card.edit.CmdClickColorEditView
import com.hive.script.views.edit.card.edit.CmdClickEditView
import com.hive.script.views.edit.card.edit.CmdClickImageEditView
import com.hive.script.views.edit.card.edit.CmdClickTextEditView
import com.hive.script.views.edit.card.edit.CmdClickViewEditView
import com.hive.script.views.edit.card.edit.CmdCopyEditView
import com.hive.script.views.edit.card.edit.CmdCurlEditView
import com.hive.script.views.edit.card.edit.CmdDelayEditView
import com.hive.script.views.edit.card.edit.CmdDownloadEditView
import com.hive.script.views.edit.card.edit.CmdFastClickEditView
import com.hive.script.views.edit.card.edit.CmdPythonExecutorEditView
import com.hive.script.views.edit.card.edit.CmdForEditView
import com.hive.script.views.edit.card.edit.CmdIfEditView
import com.hive.script.views.edit.card.edit.CmdInputEditView
import com.hive.script.views.edit.card.edit.CmdJumpEditView
import com.hive.script.views.edit.card.edit.CmdLoadScriptEditView
import com.hive.script.views.edit.card.edit.CmdRunSkillEditView
import com.hive.script.views.edit.card.edit.CmdMultipleEditView
import com.hive.script.views.edit.card.edit.CmdNoSupportEditView
import com.hive.script.views.edit.card.edit.CmdOpenAppEditView
import com.hive.script.views.edit.card.edit.CmdOpenSchemeEditView
import com.hive.script.views.edit.card.edit.CmdPressEditView
import com.hive.script.views.edit.card.edit.CmdReadScreenTextEditView
import com.hive.script.views.edit.card.edit.CmdReadViewTextEditView
import com.hive.script.views.edit.card.edit.CmdScaleEditView
import com.hive.script.views.edit.card.edit.CmdScriptEndEditView
import com.hive.script.views.edit.card.edit.CmdScriptStartEditView
import com.hive.script.views.edit.card.edit.CmdScrollEditView
import com.hive.script.views.edit.card.edit.CmdScrollMultipleEditView
import com.hive.script.views.edit.card.edit.CmdSetEditView
import com.hive.script.views.edit.card.edit.CmdTimeCalibratorEditView
import com.hive.script.views.edit.card.edit.CmdToastEditView
import com.hive.script.views.edit.card.edit.CmdVoiceInteractAsrEditView
import com.hive.script.views.edit.card.edit.CmdVoiceInteractTtsEditView
import com.hive.script.views.edit.card.edit.CommonDelayEditView
import com.hive.script.views.edit.editor.ListScriptEditView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/22/21
 */
object ScriptEditFactory {

    fun createItemEditView(
        context: Context,
        cmd: ScriptCommand,
        isDelayEdit: Boolean
    ): BaseCommandEditCard {
        val itemView: BaseCommandEditCard = if (!isDelayEdit) {
            when (cmd::class.java) {
                CmdClick::class.java -> {
                    CmdClickEditView(context)
                }

                CmdFor::class.java -> {
                    CmdForEditView(context)
                }

                CmdIf::class.java -> {
                    CmdIfEditView(context)
                }

                CmdPress::class.java -> {
                    CmdPressEditView(context)
                }

                CmdScroll::class.java -> {
                    CmdScrollEditView(context)
                }

                CmdScrollMultiple::class.java -> {
                    CmdScrollMultipleEditView(context)
                }

                CmdRepeatTap::class.java -> {
                    CmdFastClickEditView(context)
                }

                CmdOpenApp::class.java -> {
                    CmdOpenAppEditView(context)
                }

                CmdOpenUrl::class.java -> {
                    CmdOpenSchemeEditView(context)
                }

                CmdClickImage::class.java -> {
                    CmdClickImageEditView(context)
                }

                CmdClickView::class.java -> {
                    CmdClickViewEditView(context)
                }

                CmdClickColor::class.java -> {
                    CmdClickColorEditView(context)
                }

                CmdClickText::class.java -> {
                    CmdClickTextEditView(context)
                }

                CmdPinchZoom::class.java -> {
                    CmdScaleEditView(context)
                }

                CmdCopyToClipboard::class.java -> {
                    CmdCopyEditView(context)
                }

                CmdPinch::class.java -> {
                    CmdMultipleEditView(context)
                }

                CmdDelay::class.java -> {
                    CmdDelayEditView(context)
                }

                CmdPatternTap::class.java -> {
                    CmdBatchClickEditView(context)
                }

                CmdToast::class.java -> {
                    CmdToastEditView(context)
                }

                CmdInput::class.java -> {
                    CmdInputEditView(context)
                }

                CmdAlignToSecond::class.java -> {
                    CmdTimeCalibratorEditView(context)
                }

                CmdJump::class.java -> {
                    CmdJumpEditView(context)
                }

                CmdCallScript::class.java -> {
                    CmdLoadScriptEditView(context)
                }

                CmdRunSkill::class.java -> {
                    CmdRunSkillEditView(context)
                }

                CmdPlayAudio::class.java -> {
                    CommonDelayEditView(context)
                }

                CmdReadScreenText::class.java -> {
                    CmdReadScreenTextEditView(context)
                }

                CmdSet::class.java -> {
                    CmdSetEditView(context)
                }

                CmdCurl::class.java -> {
                    CmdCurlEditView(context)
                }

                CmdReadViewText::class.java -> {
                    CmdReadViewTextEditView(context)
                }

                CmdAiRequest::class.java -> {
                    CmdAiRequestEditView(context)
                }

                CmdDownload::class.java -> {
                    CmdDownloadEditView(context)
                }

                CmdPythonExecutor::class.java -> {
                    CmdPythonExecutorEditView(context)
                }

                CmdScriptStart::class.java -> {
                    CmdScriptStartEditView(context)
                }

                CmdScriptEnd::class.java -> {
                    CmdScriptEndEditView(context)
                }

                CmdVoiceInteract::class.java -> {
                    val voiceCmd = cmd as CmdVoiceInteract
                    when (voiceCmd.mode) {
                        CmdVoiceInteract.MODE_ASR -> CmdVoiceInteractAsrEditView(context)
                        else -> CmdVoiceInteractTtsEditView(context)
                    }
                }

                else -> {
                    CmdNoSupportEditView(context)
                }
            }
        } else {
            CommonDelayEditView(context)
        }

        return itemView
    }

    fun createItemView(view: ListScriptEditView, context: Context, viewType: Int): AbsListItemView {
//        val it = ScriptParser.getCmdClassByType(viewType)
        val itemView = BaseCommandCard(context)
        itemView.onItemEventListener = view
        return itemView
    }
}
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views

import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdPatternTap
import com.hive.script.cmd.CmdClick
import com.hive.script.cmd.CmdClickColor
import com.hive.script.cmd.CmdClickImage
import com.hive.script.cmd.CmdClickText
import com.hive.script.cmd.CmdClickView
import com.hive.script.cmd.CmdRepeatTap
import com.hive.script.cmd.CmdInput
import com.hive.script.cmd.CmdPinch
import com.hive.script.cmd.CmdPress
import com.hive.script.cmd.CmdReadScreenText
import com.hive.script.cmd.CmdReadViewText
import com.hive.script.cmd.CmdPinchZoom
import com.hive.script.cmd.CmdScroll
import com.hive.script.cmd.CmdScrollMultiple
import com.hive.script.views.preview.impl.ScriptItemBatchClickView
import com.hive.script.views.preview.impl.ScriptItemClickView
import com.hive.script.views.preview.impl.ScriptItemCommonClickView
import com.hive.script.views.preview.impl.ScriptItemFastClickView
import com.hive.script.views.preview.impl.ScriptItemInputView
import com.hive.script.views.preview.impl.ScriptItemMultipleView
import com.hive.script.views.preview.impl.ScriptItemPressView
import com.hive.script.views.preview.impl.ScriptItemReadView
import com.hive.script.views.preview.impl.ScriptItemScaleView
import com.hive.script.views.preview.impl.ScriptItemScrollMultipleView
import com.hive.script.views.preview.impl.ScriptItemScrollView
import com.hive.script.views.preview.ScriptItemView
import com.hive.script.views.record.impl.ScriptRecordView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/9/21
 */
object ScriptItemViewFactory {

    fun createItemViewByCommand(
        parentView: ScriptRecordView,
        command: ScriptCommand
    ): ScriptItemView? {
        val view: ScriptItemView? = when (command) {

            is CmdClick -> ScriptItemClickView()

            is CmdPress -> ScriptItemPressView()

            is CmdScroll -> ScriptItemScrollView()

            is CmdScrollMultiple -> ScriptItemScrollMultipleView()

            is CmdRepeatTap -> ScriptItemFastClickView()

            is CmdClickImage -> ScriptItemCommonClickView()

            is CmdClickView -> ScriptItemCommonClickView()

            is CmdClickColor -> ScriptItemCommonClickView()

            is CmdClickText -> ScriptItemCommonClickView()

            is CmdPinchZoom -> ScriptItemScaleView()

            is CmdPinch -> ScriptItemMultipleView()

            is CmdPatternTap -> ScriptItemBatchClickView()

            is CmdInput -> ScriptItemInputView()

            is CmdReadViewText -> ScriptItemReadView()

            is CmdReadScreenText -> ScriptItemReadView()

            else -> null
        }
        view?.parentView = parentView
        view?.command = command
        return view
    }
}
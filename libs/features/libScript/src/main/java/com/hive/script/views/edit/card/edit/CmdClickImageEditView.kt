// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.card.edit

import android.content.Context
import android.view.View
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.cmd.CmdClickImage
import com.hive.script.views.edit.views.ClickImageEditView

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/16/21
 */
class CmdClickImageEditView(context: Context) : BaseCommandEditCard(context), View.OnClickListener {

    var cmd: CmdClickImage? = null

    override fun initView() {

    }

    override fun onBindCommand(command: ScriptCommand) {
        cmd = command as CmdClickImage
//        setDrawableRight(Drawable.createFromPath(cmd!!.getAttachmentFullPath()))
        findViewById<ClickImageEditView>(R.id.spot_editor)?.loadCmdSpot(cmd!!)
    }

    override fun checkCommandOrThrowError() {
        findViewById<ClickImageEditView>(R.id.spot_editor)?.checkCommandOrThrowError()
    }


    override fun getEditContentId() = R.layout.cmd_spot_card

}
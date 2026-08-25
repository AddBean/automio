// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.impl

import android.content.Context
import android.util.AttributeSet
import com.hive.script.R
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordSelectRectView
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptClickImageHandler
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/29/21
 */
class ScriptRecordClickImageView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordSelectRectView(context, attributeSet) {

    override fun isUseNormalization() = false

    override fun getLayoutName(): String {
        return GlobalApp.getString(com.hive.i8n.R.string.sc_spot_layout_name_3)
    }

    override fun getActionType()= ScriptRecordEventHandler.RecordResultAction.ACTION_CLICK_IMAGE

    override fun getViewTypes() = mutableListOf(ScriptRecordViewManager.RecordViewType.CLICK_IMAGE)

    override fun getEventHandler() = ScriptClickImageHandler(this)

}
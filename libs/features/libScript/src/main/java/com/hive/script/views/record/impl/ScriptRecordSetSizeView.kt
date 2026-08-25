// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.impl

import android.content.Context
import android.util.AttributeSet
import com.hive.script.R
import com.hive.script.views.record.ScriptRecordEventHandler
import com.hive.script.views.record.ScriptRecordSelectRectView
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.record.handler.ScriptSetSizeHandler
import com.hive.utils.GlobalApp

/**
 *
 * @author jiadou
 * @date 6/29/21
 */
class ScriptRecordSetSizeView(context: Context, attributeSet: AttributeSet) :
    ScriptRecordSelectRectView(context, attributeSet) {

    override fun getLayoutName(): String {
        return GlobalApp.getString(com.hive.i8n.R.string.sc_spot_layout_name_6)
    }

    override fun isUseNormalization() = true

    override fun getActionType() = ScriptRecordEventHandler.RecordResultAction.ACTION_SIZE

    override fun getViewTypes() = mutableListOf(ScriptRecordViewManager.RecordViewType.LAYOUT_SIZE)

    override fun getEventHandler() = ScriptSetSizeHandler(this)
}
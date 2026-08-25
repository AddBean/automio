// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record.handler

import com.hive.script.views.beans.PointVectorFloat
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.IScriptRecordView
import com.hive.script.views.record.ScriptRecordEventHandler

class ScriptDragHandler(recordView: IScriptRecordView) :
    ScriptRecordEventHandler(recordView) {

    override fun handleEvent(action: RecordResultAction, obj: Any?) {
        ScriptRecordManager.notifyRecordResultListener(
            action,
            obj as PointVectorFloat?
        )
    }

}
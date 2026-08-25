// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record

abstract class ScriptRecordEventHandler(var recordView: IScriptRecordView) {

    enum class RecordResultAction {
        ACTION_CANCEL, ACTION_DRAG, ACTION_CLICK_COLOR, ACTION_CLICK_IMAGE, ACTION_CLICK_VIEW, ACTION_SIZE, ACTION_MULTIPLE, ACTION_SCALE, ACTION_BATCH
    }

    fun notifyEvent(action: RecordResultAction, obj: Any?) {
//        if (!ScriptInterpreter.getDefault().isRecording()) {

//        }

        recordView.setViewState(
            recordView.getViewState()
                .ofFalse(recordView.getViewTypes().first())
        )
        handleEvent(action, obj)
    }

    abstract fun handleEvent(action: RecordResultAction, obj: Any?)
}
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit.xeditor.utils

import com.hive.script.base.ScriptCommand
import com.hive.script.event.RefreshScriptEditEvent
import com.hive.script.extensions.findRootCommand
import com.hive.script.extensions.updateAllParent
import com.hive.script.views.edit.xeditor.XCellLayout
import org.greenrobot.eventbus.EventBus
import java.util.Stack

class XEditorSnapManager {

    private val mUndoStack = Stack<SnapData>()

    private val mRedoStack = Stack<SnapData>()

    private var mRevertCmd: ScriptCommand? = null

    private val Max_Stack_Size = 1000


    fun setRevertCommand(cmd: ScriptCommand?) {
        mRevertCmd = cmd?.findRootCommand()?.deepCopy()
        save(mRevertCmd, false)
    }

    fun undo() {
        if (mUndoStack.isNotEmpty()) {
            val snapData = mUndoStack.pop()
            if (mUndoStack.isNotEmpty()) {
                EventBus.getDefault()
                    .post(RefreshScriptEditEvent(mUndoStack.peek().deepCopy().apply {
                        this.cmd?.updateAllParent()
                    }))
            }
            mRedoStack.push(snapData)
        }
    }

    fun redo() {
        if (mRedoStack.isNotEmpty()) {
            val snap = mRedoStack.pop()
            EventBus.getDefault().post(RefreshScriptEditEvent(snap.deepCopy()))
            mUndoStack.push(snap)
        }
    }

    fun isUndoEnable(): Boolean {
        return mUndoStack.size > 1
    }

    fun isRedoEnable(): Boolean {
        return mRedoStack.isNotEmpty()
    }

    fun isAnyEnable(): Boolean {
        return isUndoEnable() || isRedoEnable()
    }

    fun clear() {
        mUndoStack.clear()
        mRedoStack.clear()
        mRevertCmd = null
    }

    fun save(cmd: ScriptCommand?, needRelayout: Boolean = true) {
        cmd ?: return
        val root = cmd.findRootCommand()
        if (mUndoStack.size >= Max_Stack_Size) {
            mUndoStack.removeAt(0)
        }
        if (needRelayout) {
            XEditorHelper.onScriptChanged(root)
        }
        mUndoStack.push(SnapData(root.deepCopy(), XEditorHelper.snapCellLayout()))
        mRedoStack.clear()
    }

    data class SnapData(var cmd: ScriptCommand?, var layout: XCellLayout?) {
        fun deepCopy(): SnapData {
            return SnapData(cmd?.deepCopy(), layout?.deepCopy())
        }
    }

    companion object {
        private val instance: XEditorSnapManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            XEditorSnapManager()
        }

        fun get(): XEditorSnapManager {
            return instance
        }
    }
}
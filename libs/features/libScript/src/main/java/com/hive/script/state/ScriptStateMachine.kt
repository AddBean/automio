// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.state

class ScriptStateMachine {

    private var state: ScriptState = ScriptState.INIT

    private var recordState: RecordState = RecordState.RECORDING

    private var editingState: EditingState = EditingState.EDITING

    private var stateObservers = mutableListOf<IStateObserver>()

    fun addStateObserver(observer: IStateObserver) {
        if (!stateObservers.contains(observer))
            stateObservers.add(observer)
    }

    fun removeStateObserver(observer: IStateObserver) {
        if (stateObservers.contains(observer))
            stateObservers.remove(observer)
    }

    private fun notifyStateObservers() {
        stateObservers.forEach {
            try {
                it.onStateChanged(state, recordState, editingState)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun switchToRunning() {
        if (state == ScriptState.RUNNING) return
        state = ScriptState.RUNNING
        notifyStateObservers()
    }

    fun switchToPaused() {
        if (state == ScriptState.PAUSED) return
        state = ScriptState.PAUSED
        notifyStateObservers()
    }

    fun switchToEditing() {
        if (state == ScriptState.EDITING) return
        state = ScriptState.EDITING
        notifyStateObservers()
    }

    fun switchToRecording() {
        if (state == ScriptState.RECORDING) return
        state = ScriptState.RECORDING
        notifyStateObservers()
    }

    fun switchRecordToRecording() {
        if (recordState == RecordState.RECORDING) return
        recordState = RecordState.RECORDING
        notifyStateObservers()
    }

    fun switchRecordToInsertCmd() {
        if (recordState == RecordState.INSERT_CMD) return
        recordState = RecordState.INSERT_CMD
        notifyStateObservers()
    }

    fun switchEditingToEditing() {
        if (editingState == EditingState.EDITING) return
        editingState = EditingState.EDITING
        notifyStateObservers()
    }

    fun switchEditingToInsertRecord() {
        if (editingState == EditingState.INSERT_RECORD) return
        editingState = EditingState.INSERT_RECORD
        notifyStateObservers()
    }

    fun switchEditingToInsertCmd() {
        if (editingState == EditingState.INSERT_CMD) return
        editingState = EditingState.INSERT_CMD
        notifyStateObservers()
    }

    fun reset() {
        state = ScriptState.INIT
        recordState = RecordState.RECORDING
        editingState = EditingState.EDITING
        notifyStateObservers()
    }

    /**
     * 检查是否可以切换到目标状态，规则如下
     * 1. 当前状态为INIT，可切换到RUNNING, EDITING, RECORDING, RECORDING_UNLOCK，反之也可切换到INIT
     * 2，当前状态为RUNNING，可切换到PAUSED和INIT，反之也可切换到RUNNING
     * 3，其他不可切换到其他状态
     */
    private fun checkTargetState(
        targetState: ScriptState
    ): Boolean {
        return when (state) {
            ScriptState.INIT -> {
                targetState == ScriptState.RUNNING
                        || targetState == ScriptState.EDITING
                        || targetState == ScriptState.RECORDING
                        || targetState == ScriptState.RECORDING_UNLOCK
            }

            ScriptState.RUNNING -> {
                targetState == ScriptState.PAUSED || targetState == ScriptState.INIT
            }

            ScriptState.PAUSED -> {
                targetState == ScriptState.RUNNING
            }

            ScriptState.EDITING -> {
                targetState == ScriptState.RUNNING
            }

            ScriptState.RECORDING -> {
                targetState == ScriptState.RUNNING
            }

            ScriptState.RECORDING_UNLOCK -> {
                targetState == ScriptState.RUNNING
            }

            else -> {
                false
            }
        }
    }

    enum class ScriptState(val stateName: String) {
        INIT("Initializing"),
        RUNNING("Running"),
        PAUSED("Paused"),
        EDITING("Editing"),
        RECORDING("Recording"),
        RECORDING_UNLOCK("Recording Unlock")
    }

    enum class RecordState(val stateName: String) {
        RECORDING("Recording"),
        INSERT_CMD("Insert Command")
    }


    enum class EditingState(val stateName: String) {
        EDITING("Editing"),
        INSERT_RECORD("Insert Record"),
        INSERT_CMD("Insert Command")
    }

    interface IStateObserver {
        fun onStateChanged(state: ScriptState, recordState: RecordState, editingState: EditingState)
    }
}
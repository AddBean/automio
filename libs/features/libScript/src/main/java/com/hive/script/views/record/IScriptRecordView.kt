// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record

interface IScriptRecordView {

    fun getViewState(): ScriptRecordViewManager.ViewState

    fun setViewState(state: ScriptRecordViewManager.ViewState)

    fun getViewTypes(): List<ScriptRecordViewManager.RecordViewType>

    fun getEventHandler(): ScriptRecordEventHandler

}
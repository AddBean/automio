// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.record

import com.hive.extension.isVisible
import com.hive.script.R
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager

class ScriptRecordViewManager(val view: ScriptRecordContainerView) {
    private val recordViewMap = mutableMapOf<RecordViewType, IScriptRecordView>()

    fun registerAllRecordView() {
        recordViewMap.clear()
        recordViewMap[RecordViewType.PREVIEW_DRAW] = view.findViewById(R.id.record_view)
        recordViewMap[RecordViewType.TOUCHABLE] = view.findViewById(R.id.record_view)
        recordViewMap[RecordViewType.MENU] = view.findViewById(R.id.record_view)

        recordViewMap[RecordViewType.CLICK_IMAGE] = view.findViewById(R.id.spot_image)
        recordViewMap[RecordViewType.CLICK_VIEW] = view.findViewById(R.id.spot_layout)
        recordViewMap[RecordViewType.CLICK_COLOR] = view.findViewById(R.id.spot_color)
        recordViewMap[RecordViewType.SCALE_IN_OUT] = view.findViewById(R.id.scale_inout)
        recordViewMap[RecordViewType.MULTIPLE] = view.findViewById(R.id.multiple_view)
        recordViewMap[RecordViewType.BATCH_CLICK] = view.findViewById(R.id.batch_click)
        recordViewMap[RecordViewType.LAYOUT_SIZE] = view.findViewById(R.id.layout_size)
        recordViewMap[RecordViewType.MATCH_DRAG] = view.findViewById(R.id.drag_layout)
        recordViewMap[RecordViewType.FAST_CLICK] = view.findViewById(R.id.click_layout)


    }

    fun setViewState(state: ViewState) {
        val viewSet = mutableSetOf<IScriptRecordView>()
        recordViewMap.forEach { (_, value) ->
            if (!viewSet.contains(value)) {
                if (value is ScriptRecordBaseView) {
                    setSpViewState(value, state)
                } else {
                    value.setViewState(state)
                }
                viewSet.add(value)
            }
        }
    }

    private fun setSpViewState(
        value: ScriptRecordBaseView,
        state: ViewState
    ) {
        val enable = state.isEnable(value.getViewTypes().first())
        if (value.isVisible() == enable) return
        value.setViewState(state)
        if (enable) {
            ScriptManager.pauseOrResumePlay(true)
            view.saveRecordViewState()
        } else {
            ScriptManager.pauseOrResumePlay(false)
            ScriptMenuManager.updateView(enableRecord = true)
        }
    }

    class ViewState private constructor() {

        private val defaultMap = mutableMapOf(
            RecordViewType.TOUCHABLE to false,
            RecordViewType.PREVIEW_DRAW to false,
            RecordViewType.MENU to true,
            RecordViewType.FAST_CLICK to false,
            RecordViewType.CLICK_VIEW to false,
            RecordViewType.CLICK_COLOR to false,
            RecordViewType.CLICK_IMAGE to false,
            RecordViewType.SCALE_IN_OUT to false,
            RecordViewType.MULTIPLE to false,
            RecordViewType.LAYOUT_SIZE to false,
            RecordViewType.BATCH_CLICK to false,
            RecordViewType.MATCH_DRAG to false
        )

        fun copy(): ViewState {
            return ViewState().let { vs ->
                defaultMap.forEach { (key, value) ->
                    vs.defaultMap[key] = value
                }
                this
            }
        }

        fun isEnable(type: RecordViewType): Boolean {
            return defaultMap[type] ?: false
        }

        fun of(type: RecordViewType, enable: Boolean): ViewState {
            defaultMap[type] = enable
            return this
        }

        fun ofTrue(type: RecordViewType): ViewState {
            defaultMap[type] = true
            return this
        }

        fun ofFalse(type: RecordViewType): ViewState {
            defaultMap[type] = false
            return this
        }

        companion object {
            fun default(): ViewState {
                return ViewState()
            }
        }
    }

    enum class RecordViewType {
        PREVIEW_DRAW,
        TOUCHABLE,
        MENU,
        FAST_CLICK,
        CLICK_VIEW,
        CLICK_COLOR,
        CLICK_IMAGE,
        SCALE_IN_OUT,
        MULTIPLE,
        LAYOUT_SIZE,
        BATCH_CLICK,
        MATCH_DRAG
    }
}
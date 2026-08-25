// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.menu.contents

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.hive.base.BaseLayout
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.views.dialog.DialogChooseScriptStart
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.utils.CommomListener
import com.hive.views.widgets.TextDrawableView

/**
 * 录制内容视图
 * @author jiadou
 * @date 2024/12/19
 */
class ScriptRecordContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseLayout(context, attrs, defStyleAttr) {

    private var contentContainer: LinearLayout? = null
    private var btnRecordStart: TextDrawableView? = null
    private var recordDescriptionText: View? = null


    override fun initView(view: View?) {
        contentContainer = findViewById(R.id.content_container)
        btnRecordStart = findViewById(R.id.btn_record_start)
        recordDescriptionText = findViewById(R.id.record_description_text)
        
        // 设置录制按钮点击事件
        btnRecordStart?.setOnClickListener {
            handleRecordButtonClick()
        }
    }

    /**
     * 处理录制按钮点击
     */
    private fun handleRecordButtonClick() {

        ScriptManager.pauseOrResumePlay(true)
        DialogChooseScriptStart(ScriptProvider.getViewContext()).apply {
            mCallback = CommomListener.Callback { _, cmd ->
                ScriptRecordManager.startRecord()
                ScriptManager.pauseOrResumePlay(false)
                if (cmd != null) {
                    ScriptManager.addAndExecuteCommand(cmd as ScriptCommand)
                }
            }
        }.show()
    }


    override fun getLayoutId(): Int = R.layout.widget_script_record_content_view
} 
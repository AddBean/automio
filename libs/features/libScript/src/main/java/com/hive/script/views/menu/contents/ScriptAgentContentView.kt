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
import com.hive.script.views.dialog.DialogAgentTextInput
import com.hive.views.widgets.TextDrawableView

/**
 * 智能体内容视图
 * @author jiadou
 * @date 2024/12/19
 */
class ScriptAgentContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseLayout(context, attrs, defStyleAttr) {
    private var contentContainer: LinearLayout? = null
    private var btnStart: TextDrawableView? = null
    private var descriptionText: View? = null


    override fun initView(view: View?) {
        contentContainer = findViewById(R.id.content_container)
        btnStart = findViewById(R.id.btn_start)
        descriptionText = findViewById(R.id.description_text)
        // 设置录制按钮点击事件
        btnStart?.setOnClickListener {
            DialogAgentTextInput(ScriptProvider.getViewContext())
                .setTitle(context.getString(com.hive.i8n.R.string.script_provider_new_task))
                .setHint(context.getString(com.hive.i8n.R.string.script_provider_task_prompt))
                .setOnCommonListener(object : DialogAgentTextInput.OnCommonListener {
                    override fun onSubmitted(content: String) {
                        ScriptProvider().executeAgentTask(content)
                    }

                    override fun onCanceled() {
                    }
                })
                .show()
        }
    }

    override fun getLayoutId(): Int = R.layout.widget_script_agent_content_view
} 
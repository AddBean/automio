// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.menu.contents

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.hive.base.BaseLayout
import com.hive.script.R

/**
 * 工作流内容视图
 * @author jiadou
 * @date 2024/12/19
 */
class ScriptWorkflowContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseLayout(context, attrs, defStyleAttr) {
    override fun initView(view: View?) {

    }


    override fun getLayoutId(): Int = R.layout.widget_script_workflow_content_view
} 
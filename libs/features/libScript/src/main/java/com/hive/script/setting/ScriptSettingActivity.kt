// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.hive.base.BaseFragmentActivity
import com.hive.script.R
import com.hive.utils.utils.IntentUtils

/**
 *
 * @author jiadou
 * @date 2021/10/13
 */
class ScriptSettingActivity : BaseFragmentActivity() {
    override fun doOnCreate(savedState: Bundle?) {}

    override fun getLayoutId() = R.layout.script_setting_activity

    companion object {
        fun start(context: Context) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, ScriptSettingActivity::class.java)
            )
        }
    }
}
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.base.BaseFragmentActivity
import com.hive.utils.utils.IntentUtils


class ActivityMoreSetting : BaseFragmentActivity() {


    override fun doOnCreate(savedState: Bundle?) {
    }


    override fun getLayoutId() = R.layout.activity_more_setting


    companion object {

        fun start(context: Context) {
            IntentUtils.safeStartActivity(context, Intent(context, ActivityMoreSetting::class.java))
        }
    }


}
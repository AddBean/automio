// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.hive.base.BaseFragmentActivity
import com.hive.libfiles.R
import com.hive.utils.utils.IntentUtils

class XFileSearchActivity : BaseFragmentActivity() {
    override fun doOnCreate(savedState: Bundle?) {
    }
    override fun getLayoutId() = R.layout.activity_file_search

    companion object{
        fun start(context:Context){
            IntentUtils.safeStartActivity(context, Intent(context,XFileSearchActivity::class.java))
        }
    }
}
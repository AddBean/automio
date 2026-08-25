// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.handler.preview

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import com.hive.base.BaseActivity
import com.hive.files.model.FileCardData
import com.hive.libfiles.R
import com.hive.utils.utils.IntentUtils


/**
 *
 * @author jiadou
 * @date 4/8/21
 */
class XPreviewActivity : BaseActivity() {

    override fun doOnCreate() {
    }

    override fun getLayoutId() = R.layout.x_preview_activity

    companion object {
        fun start(context: Context, file: FileCardData, imageList: ArrayList<FileCardData>?) {
            XPreviewFragment.sFile = file
            XPreviewFragment.sFileList = imageList
            IntentUtils.safeStartActivity(context, Intent(context, XPreviewActivity::class.java))
        }
    }
}
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor

import android.content.Context
import android.content.Intent
import com.hive.utils.utils.IntentUtils
import java.io.File

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2021/10/21
 */
object EditorHelper {
    @JvmStatic
    fun jumpEditor(context: Context, file: File) {
        if (file.path.endsWith("html")) {
            jumpHtmlEditor(context,file)
        } else {
            jumpTxtEditor(context,file)
        }
    }

    @JvmStatic
    fun jumpHtmlEditor(context: Context, file: File) {
        val starter = Intent(context, ActivityEditor::class.java).apply {
            putExtra(ActivityEditor.FILE_KEY, file.path)
        }
        IntentUtils.safeStartActivity(context, starter)
    }

    @JvmStatic
    fun jumpTxtEditor(context: Context, file: File) {
        val starter = Intent(context, ActivityTextEditor::class.java).apply {
            putExtra(ActivityTextEditor.FILE_KEY, file.path)
        }
        IntentUtils.safeStartActivity(context, starter)
    }

    @JvmStatic
    fun jumpHtmlEditor(context: Context) {
        val starter = Intent(context, ActivityEditor::class.java)
        IntentUtils.safeStartActivity(context, starter)
    }

    @JvmStatic
    fun jumpTxtEditor(context: Context) {
        val starter = Intent(context, ActivityTextEditor::class.java)
        IntentUtils.safeStartActivity(context, starter)
    }
}
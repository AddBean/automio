// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.hive.files.XFileUtils
import com.hive.libfiles.R
import com.hive.utils.BaseConfig
import com.hive.utils.BaseConst
import com.hive.utils.CommomListener
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.utils.file.MediaFileUtil
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.DialogLoading
import java.io.File

/**
 *
 * @author jiadou
 * @date 4/12/21
 */

object XFileShareHelper {
    var isShareRunning = false

    /**
     * 分享多个文件到系统，先复制再分享
     */
    fun shareFilesToSystem(context: Context, fileList: List<File>) {
        if (isShareRunning) return
        isShareRunning = true
        var dialog = DialogLoading(context)
        dialog.show()
        dialog.setOnDismissListener {
            isShareRunning = false
        }
        tryCopyFilesToTempDir(fileList, CommomListener.Callback { event, ls: Any? ->
            UIHandlerUtils.getInstance().executeInMainThread {
                if (event == 1) {
                    isShareRunning = false

                    dialog.dismiss()
                    val fileUris = ArrayList<Uri>()
                    (ls as List<File>?)?.forEach {
                        fileUris.add(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            FileProvider.getUriForFile(context, BaseConfig.FILE_PROVIDER, it)
                        } else {
                            Uri.fromFile(it)
                        })
                    }
                    val shareIntent = if (fileUris.size > 1) {
                        Intent(Intent.ACTION_SEND_MULTIPLE)//发送多个文件
                    } else {
                        Intent(Intent.ACTION_SEND)
                    }

                    if (fileUris.size > 1) {
                        shareIntent.type = "*/*" //多个文件格式
                        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                    } else {
                        shareIntent.type = MediaFileUtil.getFileMime(fileUris[0].path)
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUris[0])
                    }

                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

//                    shareIntent.addCategory("android.intent.category.DEFAULT");

                    IntentUtils.safeStartActivity(context, Intent.createChooser(shareIntent, GlobalApp.getString(com.hive.i8n.R.string.x_file_share_title)))

                } else if (event == 0) {
                    isShareRunning = false

                    dialog.dismiss()

                    CommonToast.getInstance().showToast(com.hive.i8n.R.string.x_file_empty_file_error)
                }
            }
        })
    }

    /**
     * 复制到分享临时目录
     */
    fun tryCopyFilesToTempDir(fileList: List<File>, listener: CommomListener.Callback) {
        Thread {
            FileUtils.clearDirectory(File(BaseConst.getShareTempPath()), false)
            fileList.forEach {
                FileUtils.copyAllFolder(it.path, BaseConst.getShareTempPath(), null)
            }

            var list = arrayListOf<File>()
            FileUtils.listAllFiles(list, BaseConst.getShareTempPath())
            var finalList = list.filter { fl -> fl.length() > 0 && XFileUtils.checkFileLegal(fl) }
            if (finalList.size > 0) {
                listener.onEvent(1, finalList)
            } else {
                listener.onEvent(0, finalList)
            }
        }.start()
    }
}
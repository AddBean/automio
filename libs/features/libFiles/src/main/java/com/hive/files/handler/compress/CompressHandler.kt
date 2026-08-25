// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.handler.compress

import androidx.fragment.app.FragmentActivity
import com.hive.files.XFileSelectorFolderDialog1
import com.hive.files.event.FileChangedEvent
import com.hive.files.utils.XFileOperateHelper
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import com.hive.utils.file.MediaFileUtil
import com.hive.views.widgets.CommonToast
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 *
 * @author jiadou
 * @date 4/12/21
 */
class CompressHandler {

    companion object {
        fun openFile(fa: FragmentActivity, file: File): Boolean {
            return when (MediaFileUtil.getFileType(file.path).fileType) {
                MediaFileUtil.FILE_TYPE_ZIP -> {
                    XFileSelectorFolderDialog1.show(fa.supportFragmentManager,GlobalApp.getString(com.hive.i8n.R.string.decompress_selected_btn), object : XFileSelectorFolderDialog1.OnFileSelectedListener {
                        override fun onFileSelected(targetFiles: List<File>) {
                            var desFile = File(targetFiles[0].path + File.separator + file.name.replace(".zip", ""))
                            XFileOperateHelper.unzipFiles(fa, file, desFile, object : XFileOperateHelper.OnFileOperateListener {
                                override fun onSuccess() {
                                    EventBus.getDefault().post(FileChangedEvent())
                                    CommonToast.getInstance().showToast("${GlobalApp.getString(com.hive.i8n.R.string.x_file_unziping_success)}${desFile?.path}")
                                }

                                override fun onFailure(e: Throwable) {

                                }

                            })
                        }
                    })
                    true
                }
                MediaFileUtil.FILE_TYPE_GZ -> {
                    false
                }
                MediaFileUtil.FILE_TYPE_TAR -> {
                    false
                }
                MediaFileUtil.FILE_TYPE_RAR -> {
                    false
                }
                else -> false
            }


        }
    }
}
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.hive.files.filedb.service.XFileHistoryService
import com.hive.files.handler.compress.CompressHandler
import com.hive.files.handler.preview.XPreviewActivity
import com.hive.files.model.FileCardData
import com.hive.files.views.XFileOpenAs
import com.hive.libfiles.R
import com.hive.utils.BaseConfig
import com.hive.utils.file.MediaFileUtil
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast
import java.io.File
import java.net.URLConnection

/**
 *
 * @author jiadou
 * @date 4/8/21
 */

class XFileHandler {

    companion object {
        val instance: XFileHandler by lazy { XFileHandler() }
    }

    var mDefaultFileHandler: DefaultFileHandler? = null
    var fileHandlerInterface: IFileHandlerInterface? = null

    fun openFile(
        context: Context,
        file: FileCardData,
        playList: MutableList<FileCardData>?,
        fragmentManager: FragmentManager? = null
    ) {
        if (!file.exists()) {
            CommonToast.getInstance().showToast(com.hive.i8n.R.string.file_not_exists)
            return
        }
        if (!XFileHistoryService.hasAdd(file.filePath)) {
            XFileHistoryService.add(file.filePath)
        }
        if (fileHandlerInterface != null) {
            if (fileHandlerInterface?.openFile(context, file, playList) == true) {
                return
            }
        }
        if (mDefaultFileHandler == null)
            mDefaultFileHandler = DefaultFileHandler()
        if (mDefaultFileHandler?.openFile(context, file, playList) == false) {
            if (fragmentManager != null) {
                XFileOpenAs.show(fragmentManager, file.newFile())
            } else {
                openWithThird(context, file.newFile())
            }
        }
    }


    fun openWithThird(context: Context, file: File) {
        var targetType = MediaFileUtil.getFileMime(file.path)
        if (targetType == "*/*") {
            targetType = URLConnection.guessContentTypeFromName(file.path)
        }
        if (TextUtils.isEmpty(targetType)) {
            targetType = "*/*"
        }

        openWithThird(context, file, targetType)
    }

    fun openWithThird(context: Context, file: File, targetType: String) {
        var uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, BaseConfig.FILE_PROVIDER, file!!)
        } else {
            Uri.fromFile(file)
        }

        var intent = Intent(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.setDataAndType(uri, targetType)
        intent.putExtra("path", file.path)
        IntentUtils.safeStartActivity(context, intent)
    }


    class DefaultFileHandler : IFileHandlerInterface {
        override fun openFile(
            context: Context,
            file: FileCardData,
            playList: MutableList<FileCardData>?
        ): Boolean {
            when {
                file.targetOpenType.contains("image") -> {
                    var imageList = arrayListOf<FileCardData>()
                    playList?.filter { MediaFileUtil.isImageFileType(it.filePath) }?.forEach {
                        imageList.add(it)
                    }
                    XPreviewActivity.start(context, file, imageList)
                    true
                }
            }
            return when {
                MediaFileUtil.isImageFileType(file.filePath) -> {
                    var imageList = arrayListOf<FileCardData>()
                    playList?.filter { MediaFileUtil.isImageFileType(it.filePath) }?.forEach {
                        imageList.add(it)
                    }
                    XPreviewActivity.start(context, file, imageList)
                    true
                }
                MediaFileUtil.isZipFileType(file.filePath) -> {
                    CompressHandler.openFile(context as FragmentActivity, file.newFile())
                }
                else -> false
            }
        }

    }

    interface IFileHandlerInterface {
        fun openFile(
            context: Context,
            file: FileCardData,
            playList: MutableList<FileCardData>?
        ): Boolean
    }
}

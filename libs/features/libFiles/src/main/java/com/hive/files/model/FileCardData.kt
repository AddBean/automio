// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.model

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.hive.libfiles.R
import com.hive.net.image.GlideApp
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.utils.file.MediaFileUtil
import java.io.File
import java.io.Serializable

/**
 *
 * @author jiadou
 * @date 4/7/21
 */
class FileCardData : Serializable {
    var filePath = Environment.getExternalStorageDirectory().absolutePath
    var fileName = GlobalApp.getString(com.hive.i8n.R.string.file_root_name)
    var fileSize = 0L
    var fileType = MediaFileUtil.FILE_TYPE_FOLDER
    var isDir = true
    var isRoot = true
    var subFileCount = 0
    var lastModified = 0L
    var parent: FileCardData? = null
    var lastPos: Int = 0
    var lastPosOffset: Int = 0
    var orientation: Int = 0
    var cardType: Int = 0
    var cardData: Any? = null
    var searchData: String? = null
    var targetOpenType = "*/*"

    fun isVideo(): Boolean = MediaFileUtil.isVideoFileType(filePath)

    fun isImage(): Boolean = MediaFileUtil.isImageFileType(filePath)

    /**
     * 获取同级文件列表
     */
    fun listDirFiles(): List<FileCardData>? {
        return File(filePath)?.parent?.run {
            File(this).listFiles()
        }?.map {
            parseFile(it)
        }?.toMutableList()
    }

    fun newFile(): File = File(filePath)

    fun newUri(): Uri = Uri.parse("file://" + filePath)

    fun exists() = newFile().exists()

    companion object {


        fun parsePath(it: String): FileCardData {
            return parseFile(File(it))
        }

        fun parseUri(uri: Uri): FileCardData {
            return parseFile(File(FileUtils.getPath(GlobalApp.getContext(), uri)))
        }


        fun parseFile(it: File): FileCardData {
            return FileCardData().apply {
                this.filePath = it.path
                this.fileName = it.name
                this.fileSize = it.length()
                this.isRoot = false
                this.isDir = it.isDirectory
                this.fileType = MediaFileUtil.getFileType(it.path)?.fileType
                        ?: MediaFileUtil.FILE_TYPE_UNKOWN
                this.lastModified = it.lastModified()
                this.subFileCount = if (this.isDir) {
                    it?.list()?.size ?: 0
                } else {
                    0
                }
            }
        }


        fun getPathFromContentUri(url: Uri, contentResolver: ContentResolver): String? {
            var filePath: String? = null
            val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
            val cursor: Cursor? = contentResolver.query(url, filePathColumn, null, null, null)
            cursor?.moveToFirst()
            cursor?.let {
                val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
                filePath = cursor.getString(columnIndex)
                cursor?.close()
                return filePath
            }
            return filePath
        }
    }

}
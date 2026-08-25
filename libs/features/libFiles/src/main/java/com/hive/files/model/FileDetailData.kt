// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.model

import com.hive.utils.file.FileUtils
import com.hive.utils.file.MediaFileUtil
import java.io.File
import java.io.Serializable

/**
 *
 * @author jiadou
 * @date 4/23/21
 */
class FileDetailData : Serializable {
    var filePath = "/sdcard"
    var fileName = "/sdcard"
    var fileSize = 0L
    var fileType = MediaFileUtil.FILE_TYPE_FOLDER
    var isDir = true
    var totalFileCount = 0
    var lastModified = 0L
    var parent: FileDetailData? = null

    fun isVideo(): Boolean = MediaFileUtil.isVideoFileType(filePath)

    fun isImage(): Boolean = MediaFileUtil.isImageFileType(filePath)

    /**
     * 获取同级文件列表
     */
    fun listDirFiles(): List<FileDetailData>? {
        return File(filePath)?.parent?.run {
            File(this).listFiles()
        }?.map {
            parseFile(it)
        }?.toMutableList()
    }

    fun newFile(): File = File(filePath)

    companion object {

        fun parsePath(it: String): FileDetailData {
            return parseFile(File(it))
        }

        fun parseFile(it: File): FileDetailData {
            return FileDetailData().apply {
                this.filePath = it.path
                this.fileName = it.name
                this.isDir = it.isDirectory
                this.fileType = MediaFileUtil.getFileType(it.path)?.fileType
                        ?: MediaFileUtil.FILE_TYPE_UNKOWN
                this.lastModified = it.lastModified()
                if (this.isDir) {
                    this.fileSize = FileUtils.getFileSize(it)
                    this.totalFileCount=FileUtils.getFileCount(it)
                } else {
                    this.fileSize = it.length()
                    this.totalFileCount=1
                }
            }
        }
    }

}
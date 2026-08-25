// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.utils

import android.database.Cursor
import android.graphics.drawable.GradientDrawable
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import com.hive.utils.GlobalApp
import com.hive.utils.file.MediaFileUtil
import java.io.File

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/12/21
 */
object XMediaStoreHelper {
    var contentResolver = GlobalApp.getApp().contentResolver


    fun queryFiles(page: Int, pageSize: Int, files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        var mediaArray = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_IMAGE_FILE_TYPE, MediaFileUtil.LAST_IMAGE_FILE_TYPE).map { it.extension }.toTypedArray()
        var mediaArray2 = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_AUDIO_FILE_TYPE, MediaFileUtil.LAST_AUDIO_FILE_TYPE).map { it.extension }.toTypedArray()
        var mediaArray3 = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_VIDEO_FILE_TYPE, MediaFileUtil.LAST_VIDEO_FILE_TYPE).map { it.extension }.toTypedArray()
        var mediaArray4 = mutableListOf<String>().apply {
            add(MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FILE_TYPE_APK).extension)
            add(MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FILE_TYPE_TORRENT).extension)
            add(MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FILE_TYPE_TXT).extension)
            add(MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FILE_TYPE_DOC).extension)
            add(MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FILE_TYPE_HTML).extension)
        }.toTypedArray()
        mediaArray = mediaArray.plus(mediaArray2)
        mediaArray = mediaArray.plus(mediaArray3)
        mediaArray = mediaArray.plus(mediaArray4)
        var condition = getSqlCondition(mediaArray)
        val selection = "(" + MediaStore.Files.FileColumns.SIZE + " > 0) and $condition "
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, selection, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC limit ${page * pageSize},$pageSize")
        parseCursor(cursor, files)
    }

    fun queryOther(files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        val selection = "(" + MediaStore.Files.FileColumns.DATA + " LIKE '%.xls'" +
                " or " + MediaStore.Files.FileColumns.DATA + " LIKE '%.docx'" +
                " or " + MediaStore.Files.FileColumns.DATA + " LIKE '%.apk'" +
                " or " + MediaStore.Files.FileColumns.DATA + " LIKE '%.xlsx'" +
                " or " + MediaStore.Files.FileColumns.DATA + " LIKE '%.zip'" +
                " or " + MediaStore.Files.FileColumns.DATA + " LIKE '%.mp3'" +
                " or " + MediaStore.Files.FileColumns.DATA + " LIKE '%.aac'" +
                " or " + MediaStore.Files.FileColumns.DATA + " LIKE '%.rar'" + ")"
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, selection, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC ${getSqlDescCondition()}")
        parseCursor(cursor, files)
    }

    fun querySearch(key: String, files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        val selection = "(" + MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE '%$key%')"
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, selection, null, MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC ${getSqlDescCondition()}")
        parseCursor(cursor, files)
    }


    fun queryTorrent(files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        var mediaArray = arrayOf("torrent")
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, getSqlCondition(mediaArray), null, MediaStore.Images.Media.DATE_MODIFIED + " DESC ${getSqlDescCondition()}")
        parseCursor(cursor, files)
    }

    fun queryImage(files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        var mediaArray = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_IMAGE_FILE_TYPE, MediaFileUtil.LAST_IMAGE_FILE_TYPE).map { it.extension }.toTypedArray()
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, getSqlCondition(mediaArray), null, MediaStore.Images.Media.DATE_MODIFIED + " DESC ${getSqlDescCondition(2000)}")
        parseCursor(cursor, files)
    }

    fun queryAudio(files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        var mediaArray = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_AUDIO_FILE_TYPE, MediaFileUtil.LAST_AUDIO_FILE_TYPE).map { it.extension }.toTypedArray()
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, getSqlCondition(mediaArray), null, MediaStore.Images.Media.DATE_MODIFIED + " DESC ${getSqlDescCondition()}")
        parseCursor(cursor, files)
    }

    fun queryDocs(files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        var mediaArray = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_DOC_FILE_TYPE, MediaFileUtil.LAST_DOC_FILE_TYPE).map { it.extension }.toTypedArray()
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, getSqlCondition(mediaArray), null, MediaStore.Images.Media.DATE_MODIFIED + " DESC ${getSqlDescCondition()}")
        parseCursor(cursor, files)
    }

    fun queryVideo(files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        var mediaArray = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_VIDEO_FILE_TYPE, MediaFileUtil.LAST_VIDEO_FILE_TYPE).map { it.extension }.toTypedArray()
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, getSqlCondition(mediaArray), null, MediaStore.Images.Media.DATE_MODIFIED + " DESC ${getSqlDescCondition()}")
        parseCursor(cursor, files)
    }

    fun queryApk(files: MutableList<MediaFile>) {
        if (!StoragePermissionsCheck.checkPermission()) {
            return
        }
        var mediaArray = mutableListOf<String>().apply {
            add(MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FILE_TYPE_APK).extension)
        }.toTypedArray()
        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, getSqlCondition(mediaArray), null, MediaStore.Images.Media.DATE_MODIFIED + " DESC ${getSqlDescCondition()}")
        parseCursor(cursor, files)
    }

    fun queryZip(files: MutableList<MediaFile>) {
        var mediaArray = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_ZIP_FILE_TYPE, MediaFileUtil.LAST_ZIP_FILE_TYPE).map { it.extension }.toTypedArray()

        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, getSqlCondition(mediaArray), null, MediaStore.Images.Media.DATE_MODIFIED + " DESC ${getSqlDescCondition()}")
        parseCursor(cursor, files)
    }

//    fun querySearch(files: MutableList<File>) {
//        var mediaArray = MediaFileUtil.getMediaFileByFileType(MediaFileUtil.FIRST_VIDEO_FILE_TYPE, MediaFileUtil.LAST_VIDEO_FILE_TYPE).map { it.mimeType }.toTypedArray()
//        val cursor = contentResolver.query(MediaStore.Files.getContentUri("external"), null, getSqlCondition(mediaArray), mediaArray, MediaStore.Images.Media.DATE_MODIFIED)
//        parseCursor(cursor, files)
//    }

    /**
     * 生成sql查询条件
     */
    private fun getSqlCondition(mediaArray: Array<String>): String {
        var sql = "("
        for (i in mediaArray.indices) {
            sql = if (i == mediaArray.size - 1) {
                "$sql ${MediaStore.Files.FileColumns.DATA} LIKE '%.${mediaArray[i].lowercase()}'  "
            } else {
                "$sql ${MediaStore.Files.FileColumns.DATA} LIKE '%.${mediaArray[i].lowercase()}' or "
            }
        }
        sql = "$sql)"
        return sql
    }

    private fun getSqlDescCondition(maxCount: Int = -1): String? {
        if (maxCount <= 0) return "";
        return "LIMIT 0,$maxCount"
    }


    private fun parseCursor(cursor: Cursor?, files: MutableList<MediaFile>) {
        if (cursor == null) {
            return
        }
        while (cursor.moveToNext()) {
            //String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE));
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
            val orientation = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.ORIENTATION))
            var mediaFile = MediaFile()
            mediaFile.file = File(path)
            mediaFile.orientation = orientation
            files.add(mediaFile)
        }
        cursor.close()
    }


    class MediaFile {
        var file: File = File("")
        var orientation: Int = -1
    }
}
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.engine

import com.hive.files.XFileUtils
import com.hive.files.filedb.XFileIndex
import com.hive.files.filedb.service.XFileIndexService
import com.hive.utils.debug.DLog
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.File

class FileEngine {
    var mFileIndex: XFileIndex? = null
    val taskSize = 8

    fun startIndexing() {
//        mFileIndex = XFileIndexService.getLastFile();
//        getIndexTask().flatMap { ls ->
//            Observable.create<List<File>> {
//                var list = mutableListOf<File>()
//                ls.forEach { f ->
//                    XFileUtils.listAllFiles2(f, { shouldBeAdd(it) && XFileUtils.checkFileLegal(it) }, {
//                        list.add(it)
//                        XFileIndexService.add(it)
//                        null
//                    })
//                }
//                it.onNext(list)
//                it.onComplete()
//            }
//        }.subscribeOn(Schedulers.io()).subscribe {
//            DLog.e("FileEngine 增加文件数量： ${Thread.currentThread().name}::${it.size}");
//        }
    }

    /**
     * 是否应该被添加；
     */
    private fun shouldBeAdd(file: File): Boolean {
        if (mFileIndex == null) return true
        DLog.e("shouldBeAdd==${ mFileIndex?.lastModified }")
        return file.lastModified() > mFileIndex?.lastModified ?: 0
    }


    private fun getIndexTask(): Observable<List<File>> {
        return Observable.create<List<File>> {
            var files = File("/sdcard")
            var fileChilds = files.listFiles().filter { it.isDirectory }.toList()
            var fileCount = fileChilds.size / taskSize
            if (fileCount == 0) fileCount = 1
            for (i in 0 until taskSize) {
                var start = i * fileCount
                var end = start + fileCount
                if (end > fileChilds.size - 1) {
                    end = fileChilds.size - 1
                }
                var ts = fileChilds.subList(start, end)
                it.onNext(ts)
                if (end == fileChilds.size - 1) {
                    break
                }
            }
            it.onNext(files.listFiles().filter { !it.isDirectory }.toList())
            it.onComplete()
        }
    }

    companion object {
        val instance: FileEngine by lazy {
            FileEngine()
        }
    }
}
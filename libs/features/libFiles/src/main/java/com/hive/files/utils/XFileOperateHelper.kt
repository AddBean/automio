// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.utils

import android.content.Context
import android.text.TextUtils
import com.hive.files.filedb.service.XFileRecycleService
import com.hive.files.views.XFileInputDialog
import com.hive.files.views.XFileOperateDialog
import com.hive.libfiles.R
import com.hive.utils.BaseConst
import com.hive.utils.file.FileUtils
import com.hive.utils.file.OnFileChangedListener
import com.hive.utils.file.ZipUtils
import java.io.File

/**
 *
 * @author jiadou
 * @date 4/9/21
 */
object XFileOperateHelper {

    fun getRecyclerPath(): String = BaseConst.getRecyclerBinPath()

    fun copyFiles(context: Context, fs: List<File>, targetFile: File, callback: OnFileOperateListener) {
        var dialog = XFileOperateDialog(context)
        dialog.show()
        dialog.startTask({ emitter ->
            var index = 0
            fs.forEach { f ->
                FileUtils.copyAllFolder(f.path, targetFile.path, object : OnFileChangedListener {
                    override fun onChanged(f: File?) {
                        index++
                        emitter.onNext(Pair(f!!, index))
                    }

                    override fun isStoped(): Boolean = dialog.mIsDismiss
                })
            }
            emitter.onComplete()
        }, { callback.onSuccess() }, { callback.onFailure(it) })
    }

    fun moveFiles(context: Context, fs: List<File>, targetFile: File, callback: OnFileOperateListener) {
        var dialog = XFileOperateDialog(context)
        dialog.show()
        dialog.startTask({ emitter ->
            var index = 0
            fs.forEach { f ->
                FileUtils.moveAllFolder(f.path, targetFile.path, object : OnFileChangedListener {
                    override fun onChanged(f: File?) {
                        index++
                        emitter.onNext(Pair(f!!, index))
                    }

                    override fun isStoped(): Boolean = dialog.mIsDismiss
                })
            }
            emitter.onComplete()
        }, { callback.onSuccess() }, { callback.onFailure(it) })

    }

    /**
     * 移动到回收站
     */
    private fun moveFilesToTrash(context: Context, fs: List<File>, targetFile: File, callback: OnFileOperateListener) {
        var dialog = XFileOperateDialog(context)
        dialog.show()
        dialog.startTask({ emitter ->
            var index = 0
            fs.forEach { f ->
                var record = XFileRecycleService.add(f.path)
                var trashPath = targetFile.path + File.separator + record.recyclerKey + File.separator
                FileUtils.makeDirs(trashPath)
                FileUtils.moveAllFolder(f.path, trashPath, object : OnFileChangedListener {
                    override fun onChanged(f: File?) {
                        index++
                        emitter.onNext(Pair(f!!, index))
                    }

                    override fun isStoped(): Boolean = dialog.mIsDismiss
                })
            }
            emitter.onComplete()
        }, { callback.onSuccess() }, { callback.onFailure(it) })

    }


    /**
     * 恢复回收站
     */
    fun recoverFilesToTrash(context: Context, fs: List<File>, callback: OnFileOperateListener) {
        var dialog = XFileOperateDialog(context)
        dialog.show()
        dialog.startTask({ emitter ->
            fs.forEach { f ->
                var record = XFileRecycleService.getByKey(f.parentFile.name)
                var index = 0
                FileUtils.moveAllFolder(f.path, record.originPath , object : OnFileChangedListener {
                    override fun onChanged(f: File?) {
                        index++
                        emitter.onNext(Pair(f!!, index))
                    }

                    override fun isStoped(): Boolean = dialog.mIsDismiss
                })
                XFileRecycleService.remove(f.path)
            }
            emitter.onComplete()
        }, { callback.onSuccess() }, { callback.onFailure(it) })

    }

    fun deleteFiles(context: Context, fs: List<File>, moveToBin: Boolean = false, callback: OnFileOperateListener) {
        if (moveToBin) {
            moveFilesToTrash(context, fs, File(getRecyclerPath()), callback)
        } else {
            var dialog = XFileOperateDialog(context)
            dialog.show()
            dialog.startTask({ emitter ->
                var index = 0
                fs.forEach { f ->
                    FileUtils.clearDirectory(f, true)
                    index++
                    emitter.onNext(Pair(f, index))

                }
                emitter.onComplete()
            }, { callback.onSuccess() }, { callback.onFailure(it) })
        }

    }

    fun newFile(context: Context, f: File, callback: OnFileOperateListener) {
        var dialog = XFileInputDialog(context, context.getString(com.hive.i8n.R.string.x_file_new_file)) { v, d ->
            if (TextUtils.isEmpty(v)) {
                callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_file_input_empty)))
                return@XFileInputDialog
            }
            var newPath = f.path + File.separator + v
            var file = File(newPath)
            if (file.exists()) {
                callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_fold_exist)))
            } else {
                if (file.mkdir()) {
                    callback.onSuccess()
                    d.dismiss()
                } else {
                    callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_fold_fail)))
                }

            }

        }
        dialog.show()
    }


    fun zipFile(context: Context, fs: List<File>, fd: File, callback: OnFileOperateListener) {
        var dialog = XFileInputDialog(context, context.getString(com.hive.i8n.R.string.x_file_zip_file)) { v, d ->
            if (TextUtils.isEmpty(v)) {
                callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_file_input_empty)))
                return@XFileInputDialog
            }

            if (TextUtils.isEmpty(v)) {
                callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_file_input_empty)))
                return@XFileInputDialog
            }

            var newPath = fd.path + File.separator + v + ".zip"
            var file = File(newPath)
            if (file.exists()) {
                callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_file_exist)))
            } else {
                d.dismiss()
                zipFiles(context, fs, File(newPath), callback)
                callback.onSuccess()
            }
        }
        dialog.setEditText(fs[0].name)
        dialog.show()
    }

    fun renameFile(context: Context, f: File, callback: OnFileOperateListener) {
        var dialog = XFileInputDialog(context, context.getString(com.hive.i8n.R.string.x_file_new_file)) { v, d ->
            if (TextUtils.isEmpty(v)) {
                callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_file_input_empty)))
                return@XFileInputDialog
            }

            if (TextUtils.isEmpty(v)) {
                callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_file_input_empty)))
                return@XFileInputDialog
            }

            var newPath = f.parent + File.separator + v
            var file = File(newPath)
            if (file.exists()) {
                callback.onFailure(Exception(context.getString(com.hive.i8n.R.string.x_file_new_file_exist)))
            } else {
                d.dismiss()
                f.renameTo(File(newPath))
                callback.onSuccess()
            }
        }
        dialog.setEditText(f.name)
        dialog.show()
    }


    fun zipFiles(context: Context, fs: List<File>, targetFile: File, callback: OnFileOperateListener) {
        var dialog = XFileOperateDialog(context)
        dialog.show()
        dialog.startTask({ emitter ->
            var index = 0
            ZipUtils.zipFiles(fs, File(targetFile.path), object : OnFileChangedListener {
                override fun onChanged(f: File?) {
                    index++
                    emitter.onNext(Pair(f!!, index))
                }

                override fun isStoped(): Boolean = dialog.mIsDismiss
            })
            emitter.onComplete()
        }, { callback.onSuccess() }, { callback.onFailure(it) })
    }

    fun unzipFiles(context: Context, fs: File, targetFile: File, callback: OnFileOperateListener) {
        var dialog = XFileOperateDialog(context)
        dialog.show()
        dialog.startTask({ emitter ->
            var index = 0
            ZipUtils.startUnzipFiles(File(fs.path), targetFile.path, object : OnFileChangedListener {
                override fun onChanged(f: File?) {
                    index++
                    emitter.onNext(Pair(f!!, index))
                }

                override fun isStoped(): Boolean = dialog.mIsDismiss
            })
            emitter.onComplete()
        }, { callback.onSuccess() }, { callback.onFailure(it) })
    }


    interface OnFileOperateListener {
        fun onSuccess()

        fun onFailure(e: Throwable)
    }

}
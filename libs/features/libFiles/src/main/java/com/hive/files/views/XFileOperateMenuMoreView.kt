// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.hive.anim.AnimUtils
import com.hive.files.XFileActivity
import com.hive.files.XFileHandler
import com.hive.files.XFileSelectorFolderDialog1
import com.hive.files.event.FileChangedEvent
import com.hive.files.filedb.service.XFileFavService
import com.hive.files.handler.compress.CompressHandler
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
 * @email 172111432@qq.com
 * @date 4/8/21
 */
class XFileOperateMenuMoreView(var context: Context, var operateMenuView: XFileOperateMenuView) : PopupWindow(context), View.OnClickListener {


    private var mSelectFiles: List<File>? = null
    private var tv_detail: View? = null
    private var tv_zip: View? = null
    private var tv_unzip: View? = null
    private var tv_fav: TextView? = null
    private var tv_rename: View? = null
    private var tv_open_with: View? = null
    private var tv_open_as: View? = null

    private var tv_open_folder: View? = null


    init {
        contentView = View.inflate(context, R.layout.x_file_operate_menu_more_view, null)
        isOutsideTouchable = false
        val dw = ColorDrawable(-0)
        setBackgroundDrawable(dw)
        isFocusable = true
        isTouchable = true

        tv_rename = contentView.findViewById(R.id.tv_rename)
        tv_fav = contentView.findViewById(R.id.tv_fav)
        tv_zip = contentView.findViewById(R.id.tv_zip)
        tv_unzip = contentView.findViewById(R.id.tv_unzip)
        tv_detail = contentView.findViewById(R.id.tv_detail)
        tv_open_with = contentView.findViewById(R.id.tv_open_with)
        tv_open_as = contentView.findViewById(R.id.tv_open_as)
        tv_open_folder = contentView.findViewById(R.id.tv_open_folder)
        tv_rename?.setOnClickListener(this)
        tv_fav?.setOnClickListener(this)
        tv_zip?.setOnClickListener(this)
        tv_unzip?.setOnClickListener(this)
        tv_detail?.setOnClickListener(this)
        tv_open_as?.setOnClickListener(this)
        tv_open_with?.setOnClickListener(this)
        tv_open_folder?.setOnClickListener(this)
    }


    fun getMeasureHeight(): Int {
        preMeasure(contentView)
        return contentView.measuredHeight
    }

    private fun preMeasure(view: View) {
        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1,
                View.MeasureSpec.AT_MOST) // 测量宽度范围，为View的最大值
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1,
                View.MeasureSpec.AT_MOST) // 测量高度范围，为View的最大值
        view.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onClick(v: View?) {
        AnimUtils.scaleAnim(v)
        dismiss()
        when (v?.id) {
            R.id.tv_open_with -> {
                optOpenFile()
            }
            R.id.tv_open_as -> {
                optOpenAsFile()
            }
            R.id.tv_rename -> {
                optRenameFile()
            }
            R.id.tv_fav -> {
                if (XFileFavService.hasAdd(mSelectFiles?.get(0)?.path)) {
                    XFileFavService.remove(mSelectFiles?.get(0)?.path)
                } else {
                    XFileFavService.add(mSelectFiles?.get(0)?.path)
                }
                updateFavStatus()
                operateMenuView.setOperateViewState(XFileOperateMenuView.OperateViewState.HIDDEN)
            }
            R.id.tv_zip -> {
                optZipFile()
            }
            R.id.tv_unzip -> {
                optUnzipFile()
            }
            R.id.tv_detail -> {
                XFileDetailDialog.show(operateMenuView.getOperateFragmentManager(), mSelectFiles?.get(0)!!)
            }
            R.id.tv_open_folder -> {
                var file = mSelectFiles?.get(0)!!
                if (file.isDirectory) {
                    XFileActivity.start(context, file.path)
                } else {
                    XFileActivity.start(context, file.parentFile.path)
                }
                operateMenuView.setOperateViewState(XFileOperateMenuView.OperateViewState.HIDDEN)
            }
        }
    }


    fun onChangeViewState(selected: List<File>?) {
        mSelectFiles = selected
        changeViewState(tv_rename, mSelectFiles?.size == 1)
        changeViewState(tv_fav, mSelectFiles?.size == 1)
        changeViewState(tv_zip, mSelectFiles?.size ?: 0 > 0)
        changeViewState(tv_unzip, mSelectFiles?.size == 1 && isSupportUnzip(mSelectFiles?.get(0)!!))
        changeViewState(tv_detail, mSelectFiles?.size == 1)
        changeViewState(tv_open_folder, mSelectFiles?.size == 1)
        changeViewState(tv_open_as, mSelectFiles?.size == 1 && mSelectFiles?.get(0)?.isDirectory == false)
        changeViewState(tv_open_with, mSelectFiles?.size == 1 && mSelectFiles?.get(0)?.isDirectory == false)
        updateFavStatus()

    }

    /**
     * 判断是否支持unzip
     */
    private fun isSupportUnzip(selectFiles: File): Boolean {
        return MediaFileUtil.isZipFileType(selectFiles.path) || MediaFileUtil.getFileType(selectFiles.path).fileType == MediaFileUtil.FILE_TYPE_APK;
    }

    private fun updateFavStatus() {
        if (mSelectFiles?.size == 1) {
            tv_fav?.isSelected = XFileFavService.hasAdd(mSelectFiles?.get(0)?.path)
            tv_fav?.text = if (tv_fav?.isSelected == false) context.getString(com.hive.i8n.R.string.x_file_fav_add) else
                context.getString(com.hive.i8n.R.string.x_file_fav_unadd)
        }
    }

    private fun changeViewState(view: View?, enable: Boolean) {
        if (enable) {
            view?.alpha = 1f
            view?.isEnabled = true
        } else {
            view?.alpha = 0.5f
            view?.isEnabled = false
        }
    }

    private fun optOpenFile() {
        XFileHandler.instance.openWithThird(context, mSelectFiles?.get(0)!!)
        operateMenuView.setOperateViewState(XFileOperateMenuView.OperateViewState.HIDDEN)
    }

    private fun optOpenAsFile() {
        XFileOpenAs.show(operateMenuView.getOperateFragmentManager(),mSelectFiles?.get(0)!!)
        operateMenuView.setOperateViewState(XFileOperateMenuView.OperateViewState.HIDDEN)
    }

    private fun optZipFile() {
        XFileSelectorFolderDialog1.show(operateMenuView.getOperateFragmentManager(), GlobalApp.getString(com.hive.i8n.R.string.compress_selected_btn), object : XFileSelectorFolderDialog1.OnFileSelectedListener {
            override fun onFileSelected(file: List<File>) {
                XFileOperateHelper.zipFile(context, mSelectFiles!!, file[0]!!, object : XFileOperateHelper.OnFileOperateListener {
                    override fun onSuccess() {
                        EventBus.getDefault().post(FileChangedEvent())
                        operateMenuView.setOperateViewState(XFileOperateMenuView.OperateViewState.HIDDEN)
                    }

                    override fun onFailure(e: Throwable) {
                        CommonToast.getInstance().showToast(e.message)
                    }
                })
            }
        })

    }

    private fun optUnzipFile() {
        var fa = context as FragmentActivity
        var file = mSelectFiles?.get(0)!!

        if (MediaFileUtil.isZipFileType(file.path)) {
            CompressHandler.openFile(context as FragmentActivity, file)
        } else {//apk格式支持
            XFileSelectorFolderDialog1.show(fa.supportFragmentManager, GlobalApp.getString(com.hive.i8n.R.string.decompress_selected_btn), object : XFileSelectorFolderDialog1.OnFileSelectedListener {
                override fun onFileSelected(targetFiles: List<File>) {
                    var desFile = File(targetFiles[0].path + File.separator + file.name.replace(".apk", ""))
                    XFileOperateHelper.unzipFiles(fa, file, desFile, object : XFileOperateHelper.OnFileOperateListener {
                        override fun onSuccess() {
                            EventBus.getDefault().post(FileChangedEvent())
                            operateMenuView.setOperateViewState(XFileOperateMenuView.OperateViewState.HIDDEN)
                            CommonToast.getInstance().showToast("${GlobalApp.getString(com.hive.i8n.R.string.x_file_unziping_success)}${desFile?.path}")
                        }

                        override fun onFailure(e: Throwable) {
                        }
                    })
                }
            })
        }
    }

    private fun optRenameFile() {
        XFileOperateHelper.renameFile(context, mSelectFiles?.get(0)!!, object : XFileOperateHelper.OnFileOperateListener {
            override fun onSuccess() {
                EventBus.getDefault().post(FileChangedEvent())
                operateMenuView.setOperateViewState(XFileOperateMenuView.OperateViewState.HIDDEN)
            }

            override fun onFailure(e: Throwable) {
                CommonToast.getInstance().showToast(e.message)
            }
        })
    }

}
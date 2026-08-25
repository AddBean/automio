// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.hive.extension.visibleOrGone
import com.hive.files.XFileHandler
import com.hive.files.XFileUtils
import com.hive.files.model.FileCardData
import com.hive.files.model.FileDetailData
import com.hive.files.utils.XImageLoader
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import com.hive.utils.file.FileUtils
import com.hive.utils.file.MediaFileUtil
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.utils.RelativeDateFormat
import com.hive.views.widgets.CommonToast
import io.reactivex.android.schedulers.AndroidSchedulers
import java.io.File
import java.util.Date

/**
 *
 * @author jiadou
 * @date 4/23/21
 */
class XFileDetailDialog : XFileStyleDialog() {

    private var mFileDetailData: FileDetailData? = null
    private var mFile: File? = null
    private var tv_btn_copy: TextView? = null
    private var btn_cancel: View? = null
    private var btn_open: View? = null
    private var iv_icon: ImageView? = null
    private var tv_file_name: TextView? = null
    private var tv_path: TextView? = null
    private var tv_size: TextView? = null
    private var tv_use: TextView? = null
    private var tv_time: TextView? = null
    private var tv_read: TextView? = null
    private var tv_write: TextView? = null
    private var tv_hidden: TextView? = null
    private var tv_type: TextView? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateFileDetail(mFile!!)
        tv_btn_copy = view.findViewById(R.id.tv_btn_copy)
        btn_cancel = view.findViewById(R.id.btn_cancel)
        btn_open = view.findViewById(R.id.btn_open)
        iv_icon = view.findViewById(R.id.iv_icon)
        tv_file_name = view.findViewById(R.id.tv_file_name)
        tv_path = view.findViewById(R.id.tv_path)
        tv_size = view.findViewById(R.id.tv_size)
        tv_use = view.findViewById(R.id.tv_use)
        tv_time = view.findViewById(R.id.tv_time)
        tv_read = view.findViewById(R.id.tv_read)
        tv_write = view.findViewById(R.id.tv_write)
        tv_hidden = view.findViewById(R.id.tv_hidden)
        tv_type = view.findViewById(R.id.tv_type)
        tv_btn_copy?.setOnClickListener {
            mFileDetailData?.let {
                ClipboardUtil.getInstance(context)
                    .copyText(getString(com.hive.i8n.R.string.app_name), it.filePath)
                CommonToast.getInstance().showToast(com.hive.i8n.R.string.toast_copy_path_msg)
            }
        }
        btn_open?.visibleOrGone(mFile?.isFile == true)
        btn_cancel?.setOnClickListener {
            dismissAllowingStateLoss()
        }
        btn_open?.setOnClickListener {
            dismissAllowingStateLoss()
            mFileDetailData?.let {
                XFileHandler.instance.openFile(
                    context!!,
                    FileCardData.parseFile(it.newFile()),
                    null,
                    fragmentManager
                )
            }
        }
    }

    @SuppressLint("CheckResult")
    fun updateFileDetail(file: File) {
        XFileUtils.getFileInfoMap(file).observeOn(AndroidSchedulers.mainThread()).subscribe {
            mFileDetailData = it
//            it?.entries?.forEach { m ->
//                m.value?.run {
//                    layout_info?.addView(VideoInfoItem().apply {
//                        bindInfo(Pair(m.key, m.value))
//                    })
//                }
//            }
            XImageLoader.loadIcon(iv_icon!!, it.filePath)
            tv_file_name?.text = it.fileName
            tv_path?.text = it.filePath
            tv_type?.text = when {
                MediaFileUtil.isVideoFileType(it.fileType) -> str(com.hive.i8n.R.string.file_type_video)
                MediaFileUtil.isAudioFileType(it.fileType) -> str(com.hive.i8n.R.string.file_type_audio)
                MediaFileUtil.isImageFileType(it.fileType) -> str(com.hive.i8n.R.string.file_type_image)
                MediaFileUtil.isZipFileType(it.fileType) -> str(com.hive.i8n.R.string.file_type_zip)
                MediaFileUtil.isDocFileType(it.fileType) -> str(com.hive.i8n.R.string.file_type_doc)
                else -> str(com.hive.i8n.R.string.file_type_other)
            }
            tv_size?.text = FileUtils.getFromatedStroageSize(it.newFile().length().toDouble())
            tv_use?.text = FileUtils.getFromatedStroageSize(it.newFile().usableSpace.toDouble())
            tv_time?.text = RelativeDateFormat.format(Date(it?.lastModified!!))
            tv_read?.text =
                getString(if (it.newFile().canRead()) com.hive.i8n.R.string.x_file_yes else com.hive.i8n.R.string.x_file_no)
            tv_write?.text =
                getString(if (it.newFile().canWrite()) com.hive.i8n.R.string.x_file_yes else com.hive.i8n.R.string.x_file_no)
            tv_hidden?.text =
                getString(if (it.newFile().isHidden) com.hive.i8n.R.string.x_file_yes else com.hive.i8n.R.string.x_file_no)

        }
    }

    fun str(resId: Int): String {
        return GlobalApp.getString(resId);
    }

    override fun getLayoutResId() = R.layout.x_file_detail_dialog

    companion object {
        fun show(manager: FragmentManager, file: File) {
            var dialog = XFileDetailDialog()
            dialog.setFile(file)
            dialog.show(manager.beginTransaction(), "file detail")
        }
    }

    private fun setFile(file: File) {
        mFile = file
    }
}

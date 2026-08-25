// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.os.Bundle
import android.support.rastermillv2.FrameSequenceHelper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.hive.files.model.FileCardData
import com.hive.files.views.XFileOperateMenuView
import com.hive.files.views.XFileStyleDialog
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import com.hive.views.IBackListener

/**
 *
 * @author jiadou
 * @date 4/7/21
 */

open class XFileFragment2 : XFileListFragment(), IBackListener,
    XFileListFragment.OnFileOperateListener, IOperateMenuInterface {

    var mFileOperateMenuView: XFileOperateMenuView? = null
    var mFileBak: MutableList<FileCardData>? = null

    private var tv_chose_folder: TextView? = null
    private var iv_search: View? = null

    override fun getLayoutId(): Int = R.layout.x_file_fragment_2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FrameSequenceHelper.init(GlobalApp.sContext)
    }

    override fun initView() {
        super.initView()
        tv_chose_folder = view?.findViewById(R.id.tv_chose_folder)
        iv_search = view?.findViewById(R.id.iv_search)
        addOnFileOperateListener(this)
        mFileOperateMenuView = getOperateMenuView()
        mFileOperateMenuView?.attachOperateMenuInterface(this)
        mFileOperateMenuView?.attachFileListFragment(this)
        mFileOperateMenuView?.getFileNewFold()?.visibility = View.GONE
        tv_chose_folder?.setOnClickListener {
            if (isFastClick()) return@setOnClickListener
            if (mFileBak == null) {
                mFileBak = mutableListOf();
                mFileList.forEach { mFileBak?.add(it) }
            }
            XFileSelectorFolderDialog2.show(
                fragmentManager!!,
                mFileBak!!,
                object : XFileSelectorFolderDialog2.OnFileSelectedListener {
                    override fun onFileSelected(dialog: XFileStyleDialog?, file: FileCardData) {
                        dialog?.dismiss()
                        tv_chose_folder?.text = file.fileName
                        loadFolderFiles(file)
                    }
                });
        }
        iv_search?.setOnClickListener {
            XFileSearchActivity.start(requireContext())
        }
    }

    private var lastClickTime: Long = 0

    private val THROTTLE_TIME_DEFAULT = 100 // 0.1s


    open fun isFastClick(): Boolean {
        val curClickTime = System.currentTimeMillis()
        if (curClickTime - lastClickTime <= THROTTLE_TIME_DEFAULT) {
            return true
        }
        lastClickTime = curClickTime
        return false
    }

    private fun loadFolderFiles(file: FileCardData?) {
        file?.let {
            mCurrentFolder = it
            refreshFiles()
        }
    }

    open fun getOperateMenuView(): XFileOperateMenuView {
        if (mFileOperateMenuView != null) return mFileOperateMenuView!!
        return XFileOperateMenuView(context!!)
    }

    override fun getOperateMenuContainerView(): ViewGroup = view as ViewGroup

    override fun getOperateFragmentManager(): FragmentManager = fragmentManager!!

    override fun onFileChanged(file: FileCardData) {
    }

    override fun onFileClicked(file: FileCardData, fileList: MutableList<FileCardData>) {
        XFileHandler.instance.openFile(context!!, file, fileList, getOperateFragmentManager())
    }

    override fun isStartLoadFile() = true

    override fun onBackPressed(): Boolean =
        mFileOperateMenuView?.onBackPressed() == true || super.onBackPressed()

    override fun onDestroy() {
        super.onDestroy()
        mFileOperateMenuView?.onDestroyView()
        removeOnFileOperateListener(this)
    }
}
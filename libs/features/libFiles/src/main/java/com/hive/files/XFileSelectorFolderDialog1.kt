// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.hive.files.model.FileCardData
import com.hive.files.views.XFileFilterMenu
import com.hive.files.views.XFileNavigationBar
import com.hive.files.views.XFileStyleDialog
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import com.hive.utils.system.UIUtils
import java.io.File

/**
 *
 * @author jiadou
 * @date 4/9/21
 */
class XFileSelectorFolderDialog1 : XFileStyleDialog(), XFileNavigationBar.INavigationListener, XFileListFragment.OnFileOperateListener, IOperateMenuInterface {

    private var btnConfirmText: String? = null
    private var onFileSelectedListener: OnFileSelectedListener? = null
    private val mXFileListFragment = XFileListFragment()
    private var DP = UIUtils.dp2px(GlobalApp.sContext, 1)

    private var btn_file_paste_now: TextView? = null

    private var btn_file_paste_cancel: TextView? = null

    private var file_navigation_bar: XFileNavigationBar? = null

    private var iv_filter: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_file_paste_now= view.findViewById(R.id.btn_file_paste_now)
        btn_file_paste_cancel = view.findViewById(R.id.btn_file_paste_cancel)
        file_navigation_bar = view.findViewById(R.id.file_navigation_bar)
        iv_filter = view.findViewById(R.id.iv_filter)
        initFileList()
        initEvent()
        btnConfirmText?.let {
            btn_file_paste_now?.text = it
        }
    }

    private fun initFileList() {
        childFragmentManager?.beginTransaction()?.replace(R.id.frag_content, mXFileListFragment, "XFileSelectorDialog->mXFileListFragment")?.commitAllowingStateLoss()
        file_navigation_bar?.mNavigationListener = this
        mXFileListFragment.mEnableEdit = false
        mXFileListFragment.mEnableShowFile = false

    }


    private fun initEvent() {
        iv_filter?.setOnClickListener {
            XFileFilterMenu.showMenu(it, mXFileListFragment, 40 * DP, -16 * DP, Gravity.LEFT or Gravity.BOTTOM)
        }
        mXFileListFragment?.addOnFileOperateListener(this)
        btn_file_paste_cancel?.setOnClickListener {
            dismissAllowingStateLoss()
        }
        btn_file_paste_now?.setOnClickListener {
            onFileSelectedListener?.onFileSelected(arrayListOf(File(mXFileListFragment.mCurrentFolder.filePath)))
            dismissAllowingStateLoss()
        }
    }


    override fun getLayoutResId(): Int = R.layout.x_folder_selector_dialog

    override fun onNavigationClicked(file: FileCardData) {
        mXFileListFragment.setCurrentFile(file)
    }

    override fun onFileChanged(file: FileCardData) {
        file_navigation_bar?.updateBar(file)
    }

    override fun onFileClicked(file: FileCardData, fileList: MutableList<FileCardData>) {

    }

    override fun getOperateMenuContainerView(): ViewGroup = view as ViewGroup

    override fun getOperateFragmentManager(): FragmentManager = fragmentManager!!

    override fun onBackPressed(): Boolean = mXFileListFragment.onBackPressed()

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onFileSelectedListener?.onDismiss()
    }

    companion object {
        fun show(manager: FragmentManager, btnConfirmText: String?, listener: OnFileSelectedListener?) {
            var dialog = XFileSelectorFolderDialog1()
            dialog.btnConfirmText = btnConfirmText
            dialog.onFileSelectedListener = listener
            dialog.show(manager.beginTransaction(), "file selector")
        }
    }

    interface OnFileSelectedListener {
        fun onFileSelected(file: List<File>)

        fun onDismiss(){}
    }
}
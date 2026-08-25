// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.os.Bundle
import android.os.Environment
import android.support.rastermillv2.FrameSequenceHelper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.hive.files.model.FileCardData
import com.hive.files.views.XFileFilterMenu
import com.hive.files.views.XFileNavigationBar
import com.hive.files.views.XFileOperateMenuView
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import com.hive.views.IBackListener

/**
 *
 * @author jiadou
 * @date 4/7/21
 */

open class XFileFragment : XFileListFragment(), IBackListener, XFileNavigationBar.INavigationListener, XFileListFragment.OnFileOperateListener, IOperateMenuInterface {

    var mFileOperateMenuView: XFileOperateMenuView? = null

    private var file_navigation_bar: XFileNavigationBar? = null

    private var iv_filter: View? = null

    override fun getLayoutId(): Int = R.layout.x_file_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FrameSequenceHelper.init(GlobalApp.sContext)
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        args?.getString(INTENT_KEY_TARGET_PATH)?.run {
            var rootPath = Environment.getExternalStorageDirectory().absolutePath;
            mCurrentFolder = FileCardData.parsePath(this)
            var parentFile = mCurrentFolder.newFile().parentFile
            var curParent = FileCardData.parseFile(parentFile)
            mCurrentFolder.parent = curParent
            while (parentFile != null
                    && parentFile.exists()
                    && parentFile.parentFile != null
                    && parentFile.isDirectory
                    && parentFile.absolutePath != rootPath) {
                parentFile = parentFile.parentFile
                var cardData = FileCardData.parseFile(parentFile);
                curParent.parent = cardData
                curParent = cardData
            }
        }
    }

    override fun initView() {
        super.initView()
        file_navigation_bar = view?.findViewById(R.id.file_navigation_bar)
        iv_filter = view?.findViewById(R.id.iv_filter)
        addOnFileOperateListener(this)
        mFileOperateMenuView = getOperateMenuView()
        mFileOperateMenuView?.attachOperateMenuInterface(this)
        mFileOperateMenuView?.attachFileListFragment(this)
        file_navigation_bar?.mNavigationListener = this
        file_navigation_bar?.updateBar(mCurrentFolder)
        iv_filter?.setOnClickListener {
            XFileFilterMenu.showMenu(it, this, 40 * DP, -16 * DP, Gravity.LEFT or Gravity.BOTTOM)
        }
    }

    open fun getOperateMenuView(): XFileOperateMenuView {
        if (mFileOperateMenuView != null) return mFileOperateMenuView!!
        return XFileOperateMenuView(context!!)
    }

    override fun getOperateMenuContainerView(): ViewGroup = view as ViewGroup

    override fun getOperateFragmentManager(): FragmentManager = fragmentManager!!

    override fun onFileChanged(file: FileCardData) {
        file_navigation_bar?.updateBar(file)
    }

    override fun onFileClicked(file: FileCardData, fileList: MutableList<FileCardData>) {
        XFileHandler.instance.openFile(context!!, file, fileList,getOperateFragmentManager())
    }

    override fun onNavigationClicked(file: FileCardData) {
        setCurrentFile(file)
    }

    override fun isStartLoadFile() = true

    override fun onBackPressed(): Boolean = mFileOperateMenuView?.onBackPressed() == true || super.onBackPressed()

    override fun onDestroy() {
        super.onDestroy()
        mFileOperateMenuView?.onDestroyView()
        removeOnFileOperateListener(this)
    }

    companion object{
        const val INTENT_KEY_TARGET_PATH = "targetPath"
    }
}
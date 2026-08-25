// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.handler.preview

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.hive.adapter.core.AbsCardItemView
import com.hive.adapter.core.CardItemData
import com.hive.base.BaseListFragment
import com.hive.files.XFileCardItemFactory
import com.hive.files.XFileCardItemFactory.Companion.Card_Type_PREVIEW_IMAGE
import com.hive.files.model.FileCardData
import com.hive.libfiles.R

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/8/21
 */
class XPreviewFragment : BaseListFragment() {
    private val mSnapHelper = PagerSnapHelper()
    private var mDataList: MutableList<CardItemData>? = null
    private var mCurrentIndex: Int = 0
    private var preview_menu: XPreviewMenuView? = null
    private var view_bg: View? = null

    companion object {
        var sFileList: ArrayList<FileCardData>? = null
        var sFile: FileCardData? = null
    }

    override fun doInitialize() {
        preview_menu = view?.findViewById(R.id.preview_menu)
        view_bg = view?.findViewById(R.id.view_bg)
        preview_menu?.mPreviewFragment = this
        preview_menu?.attachBackgroundView(view_bg)
        if (sFile == null && sFileList == null) {
            return
        }
        if (sFileList == null || sFileList?.isEmpty() == true) {
            sFileList = arrayListOf()
            sFileList?.add(sFile!!)
        }
        mCurrentIndex = getTargetPosition(sFile)
        mSnapHelper.attachToRecyclerView(mViewHolder.mRecyclerView)
        mListHelper.notifyData(true)
        preview_menu?.updateFileStatus(sFileList, mCurrentIndex)
        mListHelper.mRecyclerView.layoutManager?.scrollToPosition(mCurrentIndex)
        mViewHolder.mRecyclerView?.post {
            mListHelper.mRecyclerView.addOnScrollListener(object : XPreviewPageScrollListener() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    mCurrentIndex = position
                    sFile = sFileList?.get(mCurrentIndex)
                    preview_menu?.updateFileStatus(sFileList, position)
                }
            })
        }

    }

    /**
     * 删除文件
     */
    fun deleteFile(file: FileCardData?) {
        var index = sFileList?.indexOf(file)
        sFileList?.remove(file)
        if (sFileList?.isEmpty() == true) {
            requireActivity().finish()
            return
        }
        index?.run {
            if (this >= 0 && this < sFileList?.size ?: 0) {
                sFile = sFileList?.get(this)
                mCurrentIndex = this
                mListHelper.notifyData(true)
                preview_menu?.updateFileStatus(sFileList, mCurrentIndex)
            }
        }

    }

    private fun getTargetPosition(f: FileCardData?): Int {
        var currentFile = sFileList?.find { f?.filePath == it.filePath }
        return sFileList?.indexOf(currentFile) ?: 0
    }

    fun updateCurrentCard() {
        notifyItemChanged(mCurrentIndex)
    }

    override fun onCardEvent(cardEvent: Int, args: Any?, itemView: AbsCardItemView?) {
        super.onCardEvent(cardEvent, args, itemView)
        if (cardEvent == 1) {
            preview_menu?.onPreviewClicked()
        }
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }

    override fun getRequestUrl(): String? = null

    override fun isLoadMoreEnable(): Boolean = false

    override fun isRefreshEnable(): Boolean = false

    override fun isStartRequest(): Boolean = false

    override fun parseData(data: String?): MutableList<CardItemData> {
        mDataList = sFileList?.map { card ->
            CardItemData().apply {
                this.cardType = Card_Type_PREVIEW_IMAGE
                this.data = card
            }
        }?.toMutableList()
        return mDataList ?: mutableListOf()
    }

    override fun onDestroy() {
        super.onDestroy()
        sFileList = null
    }

    override fun getCardFactory() = XFileCardItemFactory.instance

    override fun getLayoutId(): Int = R.layout.x_preview_fragment

}
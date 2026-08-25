// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.adapter.core.AbsCardItemView
import com.hive.adapter.core.CardItemData
import com.hive.adapter.core.ICardItemFactory
import com.hive.adapter.core.ICardItemView
import com.hive.base.BaseListFragment
import com.hive.files.card.XFileBaseCard
import com.hive.files.config.XFileConfig
import com.hive.files.event.FileChangedEvent
import com.hive.files.model.FileCardData
import com.hive.files.views.XFileFastTouchBar
import com.hive.libfiles.R
import com.hive.utils.utils.BaseSPClass
import com.hive.views.IBackListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File


/**
 *
 * @author jiadou
 * @date 4/7/21
 */
open class XFileListFragment : BaseListFragment(), IBackListener {

    private var mGridLayoutManager: GridLayoutManager? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var gridCount: Int = 4
    private var isInGrid: Boolean = false
    private var sortType: Int = 0
    private var mCurrentEditOn = false
    protected var mFileList = mutableListOf<FileCardData>()
    var mDataList = mutableListOf<CardItemData>()
    private var mOnFileOperateListener = mutableListOf<OnFileOperateListener>()
    private var mOnCardEventListener = mutableListOf<OnCardEventListener>()
    var isLoading = false
    var mEnableEdit: Boolean = true
    var mEnableShowFile: Boolean = true
    var mCurrentFolder = FileCardData()
    var touch_bar: XFileFastTouchBar? = null

    override fun getRequestUrl(): String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun initView() {
        updateDataState()
        super.initView()

    }

    override fun doInitialize() {
        touch_bar = view?.findViewById(R.id.touch_bar)
        touch_bar?.mOnFastTouchListener = object : XFileFastTouchBar.OnFastTouchListener {
            override fun onTouchProgress(progress: Float) {
                var lm = mViewHolder.mRecyclerView.layoutManager
                if (lm is LinearLayoutManager) {
                    lm.scrollToPositionWithOffset((mDataList.size * progress).toInt(), 0)
                }
            }
        }
        mViewHolder?.mRecyclerView?.layoutManager = layoutManager
        mViewHolder.mRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                var lm = mViewHolder.mRecyclerView.layoutManager
                if (lm is LinearLayoutManager) {
                    var progress =
                        lm.findFirstVisibleItemPosition().toFloat() / mDataList.size.toFloat()
                    touch_bar?.updateProgressOutside(progress)
                }
            }
        })
        if (isStartLoadFile()) {
            refreshFiles()
        }
    }

    open fun isStartLoadFile() = true

    fun setCurrentFile(file: FileCardData) {
        mCurrentFolder = file
        refreshFiles()
    }

    fun refreshFilesWithRollback() {
        updateLastPosition()
        refreshFiles()
    }

    open fun refreshFiles() {
        if (isLoading) return
        mViewHolder.mLayoutState.showProgress()
        isLoading = true
        var single = getListFiles(mCurrentFolder)
        single.map {
            mFileList = it
            mFileList.map {
                CardItemData().apply {
                    cardType = getCurrentCardType(it)
                    data = it
                    isEditModel = isItemEditOn(it)
                }
            }.toMutableList()
        }.observeOn(AndroidSchedulers.mainThread()).subscribe({
            isLoading = false
            mDataList = it
            updateDataState(true)
        }, {
            it.printStackTrace()
        })
    }

    open fun isItemEditOn(file: FileCardData): Boolean = mCurrentEditOn

    open fun getListFiles(file: FileCardData): Observable<MutableList<FileCardData>> {
        return if (mEnableShowFile) {
            XFileUtils.listFoldFiles(mCurrentFolder)
//            XFileUtils.listVideosFromMediaStore()
        } else {
            XFileUtils.listFoldFolds(mCurrentFolder)
        }
    }

    open fun updateDataState(reload: Boolean = false) {
        isInGrid = BaseSPClass.read(XFileConfig()).inGrid
        sortType = BaseSPClass.read(XFileConfig()).sortType
        gridCount = BaseSPClass.read(XFileConfig()).gridCount
        mViewHolder?.run {
            mRecyclerView?.layoutManager = layoutManager

            mDataList?.forEach {
                it.cardType = getCurrentCardType(it.data as FileCardData)
            }

            when (sortType) {
                0 -> mDataList?.sortBy { (it.data as FileCardData).fileName }
                1 -> mDataList?.sortBy { -(it.data as FileCardData).fileSize }
                2 -> mDataList?.sortBy { (it.data as FileCardData).fileSize }
                3 -> mDataList?.sortBy { (it.data as FileCardData).fileType }
                4 -> mDataList?.sortByDescending { (it.data as FileCardData).lastModified }
            }
            mFileList.clear()
            mDataList?.sortBy { !(it.data as FileCardData).isDir }
            mDataList.forEach {
                mFileList.add(it.data as FileCardData)
            }
            if (reload) {
                mListHelper?.notifyData(true)
            } else {
                notifyDataSetChanged()
            }

        }
    }

    open fun getCurrentCardType(it: FileCardData): Int {
        return when {
            it.isDir && isInGrid -> XFileCardItemFactory.Card_Type_Grid_Folder
            it.isDir && !isInGrid -> XFileCardItemFactory.Card_Type_Folder
            !it.isDir && isInGrid -> XFileCardItemFactory.Card_Type_Grid_File
            !it.isDir && !isInGrid -> XFileCardItemFactory.Card_Type_File
            else -> XFileCardItemFactory.Card_Type_File
        }
    }

    override fun onLoadFinished() {
        super.onLoadFinished()
        mOnFileOperateListener?.forEach {
            it?.onFileChanged(mCurrentFolder)
        }
        mViewHolder.mRecyclerView.post {
            var lm = mViewHolder.mRecyclerView.layoutManager
            if (lm is LinearLayoutManager) {
                lm.scrollToPositionWithOffset(mCurrentFolder.lastPos, mCurrentFolder.lastPosOffset)
            }
        }
    }

    override fun onCardEvent(cardEvent: Int, args: Any?, itemView: AbsCardItemView?) {
        super.onCardEvent(cardEvent, args, itemView)
        if (cardEvent == XFileBaseCard.EVENT_BIND_DATA) {
            var p = args as Pair<XFileBaseCard, FileCardData>?
            bindCardView(p?.first, p?.second)
        } else {
            when (cardEvent) {
                XFileBaseCard.EVENT_SWITCH_MODE -> {
                    if (mEnableEdit) {
                        disSelectAll()
                        if (args is CardItemData) {
                            args.isSelected = true
                        }
                        enableEdit()
                    }
                }

                XFileBaseCard.EVENT_SELECTED -> {
                    notifyItemChanged(args as Int)
                }

                XFileBaseCard.EVENT_CLICKED -> {
                    var fileData = if (args is FileCardData) {
                        args
                    } else {
                        FileCardData.parsePath(args as String)
                    }
                    if (fileData.isDir) {
                        openFolder(fileData)
                    } else {
                        mOnFileOperateListener?.forEach {
                            it?.onFileClicked(fileData, mFileList)
                        }
                    }
                }
            }
            mOnCardEventListener?.forEach {
                it.onCardEvent(cardEvent, args, itemView)
            }
        }

    }

    open fun bindCardView(cardView: XFileBaseCard?, cardData: FileCardData?) {

    }

    open fun openFolder(file: FileCardData) {
        updateLastPosition()
        file.parent = mCurrentFolder
        mCurrentFolder = file
        refreshFiles()
    }

    fun updateLastPosition() {
        mViewHolder?.mRecyclerView?.layoutManager?.takeIf { it.childCount > 0 }?.run {
            var view = getChildAt(0)
            mCurrentFolder?.lastPosOffset = view?.top ?: 0
            mCurrentFolder?.lastPos = getPosition(view!!)
        }
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager {
        return if (isInGrid) {
            if (mGridLayoutManager == null)
                mGridLayoutManager = GridLayoutManager(context, getGridCount())
            mGridLayoutManager!!
        } else {
            if (mLinearLayoutManager == null)
                mLinearLayoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            mLinearLayoutManager!!
        }
    }

    open fun getGridCount() = gridCount

    override fun onRefresh() {
        super.onRefresh()
        refreshFiles()
    }

    override fun onResume() {
        super.onResume()
        if (mDataList == null || mDataList.isEmpty()) {
            refreshFiles()
        }
    }

    /**
     * 获取所有已选项
     */
    fun getAllSelectedFiles(): List<File> {
        return mDataList.filter { it.isSelected }.map {
            File((it.data as FileCardData).filePath)
        }
    }

    fun addOnFileOperateListener(listener: OnFileOperateListener) {
        if (!mOnFileOperateListener.contains(listener)) {
            mOnFileOperateListener.add(listener)
        }
    }

    fun addOnCardEventListener(listener: OnCardEventListener) {
        if (!mOnCardEventListener.contains(listener)) {
            mOnCardEventListener.add(listener)
        }
    }

    fun removeOnFileOperateListener(listener: OnFileOperateListener) {
        if (mOnFileOperateListener.contains(listener)) {
            mOnFileOperateListener.remove(listener)
        }
    }

    fun removeOnCardEventListener(listener: OnCardEventListener) {
        if (mOnCardEventListener.contains(listener)) {
            mOnCardEventListener.remove(listener)
        }
    }

    fun selectAll() {
        mViewHolder?.mRecyclerView?.run {
            mDataList?.forEach {
                it.selected = true
            }
            notifyDataSetChanged()
        }
    }

    fun disSelectAll() {
        mViewHolder?.mRecyclerView?.run {
            mDataList?.forEach {
                it.selected = false
            }
            notifyDataSetChanged()
        }
    }

    fun disableEdit() {
        mViewHolder?.mRecyclerView?.run {
            mCurrentEditOn = false
            mDataList?.forEach {
                it.editModel = false
            }
            notifyDataSetChanged()
        }
    }

    fun enableEdit() {
        if (mEnableEdit) {
            mCurrentEditOn = true
            mViewHolder?.mRecyclerView?.run {
                mDataList?.forEach {
                    it.editModel = true
                }
                notifyDataSetChanged()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFileChangedEvent(e: FileChangedEvent) {
        if (userVisibleHint) {
            disableEdit()
            if (e.rollback) {
                refreshFilesWithRollback()
            } else {
                refreshFiles()
            }
        }
    }


    open fun onMenuShow() {
//        var height = context?.resources?.getDimension(R.dimen.x_file_menu_height)?.toInt()
//        mViewHolder?.mRecyclerView?.setPadding(0, height ?: 0, 0, height ?: 0)
    }

    open fun onMenuHidden() {
//        mViewHolder?.mRecyclerView?.setPadding(0, 0, 0, 0)
    }

    interface OnFileOperateListener {
        fun onFileChanged(file: FileCardData)

        fun onFileClicked(file: FileCardData, fileList: MutableList<FileCardData>)
    }

    interface OnCardEventListener {
        fun onCardEvent(cardEvent: Int, args: Any?, itemView: AbsCardItemView?)
    }

    override fun getLayoutId(): Int = R.layout.x_file_list_fragment

    override fun parseData(data: String?): MutableList<CardItemData>? = mDataList

    override fun isLoadMoreEnable(): Boolean = false

    override fun isRefreshEnable(): Boolean = false

    override fun isStartRequest(): Boolean = false

    override fun getCardFactory(): ICardItemFactory<*, out ICardItemView<*>> {
        return XFileCardItemFactory.instance
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onBackPressed(): Boolean {
        if (mCurrentFolder.parent == null) return false
        if (isLoading) return true
        mCurrentFolder = mCurrentFolder?.parent!!
        refreshFiles()
        return true
    }
}
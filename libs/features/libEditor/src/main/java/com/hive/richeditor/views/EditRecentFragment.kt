// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hive.adapter.core.AbsCardItemView
import com.hive.adapter.core.CardItemData
import com.hive.adapter.core.ICardItemFactory
import com.hive.base.BaseListFragment
import com.hive.base.BaseListHelper
import com.hive.editor.R
import com.hive.files.model.FileCardData
import com.hive.files.utils.XImageLoader
import com.hive.richeditor.editordb.EditHistory
import com.hive.richeditor.editordb.service.EditHistoryService
import com.hive.utils.utils.RelativeDateFormat
import com.hive.utils.utils.StringUtils
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.Date

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/1/21
 */
class EditRecentFragment : BaseListFragment() {

    private var mLisData: List<EditHistory>? = null
    private var mCompositeDisposable = CompositeDisposable()
    var mOnClickListener: OnEditClickListener? = null

    override fun getRequestUrl() = null

    override fun doInitialize() {
        mCompositeDisposable.add(
            Flowable.fromPublisher<List<EditHistory>> {
                it.onNext(EditHistoryService.list())
                it.onComplete()
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe {
                mLisData = it
                mListHelper.notifyData(true)
            }
        )
        view?.findViewById<View>(R.id.view_mask)?.setOnClickListener {
            mOnClickListener?.onDismiss()
        }
    }

    override fun parseData(data: String?): MutableList<CardItemData> =
        mutableListOf<CardItemData>().apply {
            mLisData?.forEach { h ->
                add(CardItemData().apply {
                    this.data = FileCardData.parsePath(h.path)
                })
            }
        }


    override fun getCardFactory() = object : ICardItemFactory<CardItemData, AbsCardItemView> {
        override fun createItemView(context: Context?, type: Int) = RecentFileCard()

        override fun offerTypeCount() = 0
    }

    inner class RecentFileCard : AbsCardItemView(context), View.OnClickListener {
        var fileData: FileCardData? = null
        var cardData: CardItemData? = null
        var tv_name: TextView? = null
        var tv_info: TextView? = null
        var tv_time: TextView? = null
        var iv_icon: ImageView? = null
        var layout_root: View? = null
        override fun initView(view: View?) {
            view?.setOnClickListener(this)
            tv_name = view?.findViewById(R.id.tv_name)
            tv_info = view?.findViewById(R.id.tv_info)
            tv_time = view?.findViewById(R.id.tv_time)
            iv_icon = view?.findViewById(R.id.iv_icon)
            layout_root = view?.findViewById(R.id.layout_root)
        }

        override fun bindData(data: CardItemData?) {
            cardData = data
            fileData = data?.data as FileCardData?
            XImageLoader.load(iv_icon!!, fileData)
            tv_name?.text = fileData?.fileName
            tv_info?.text = StringUtils.byte2XB(fileData?.fileSize!!)
            tv_time?.text = RelativeDateFormat.format(Date(fileData?.lastModified!!))
            if (!TextUtils.isEmpty(fileData?.searchData)) {
                StringUtils.setSpanningText(tv_name, fileData?.searchData)
            }
            layout_root?.isSelected = data?.isSelected == true
        }

        override fun getLayoutId() = R.layout.recent_file_card

        override fun onClick(v: View?) {
            mListHelper?.data?.forEach {
                it.isSelected = false
            }
            cardData?.isSelected = true
            mOnClickListener?.onEditorItemClick(fileData)
            bindData(cardData)
            mListHelper.notifyData(true)
            mOnClickListener?.onDismiss()
        }
    }

    fun updateList() {
        mListHelper.notifyData(true)
    }

    override fun getLayoutId() = R.layout.edit_recent_fragment

    override fun getRequestType() = BaseListHelper.RequestType.REQUEST_LOCAL

    interface OnEditClickListener {
        fun onDismiss()

        fun onEditorItemClick(file: FileCardData?)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.clear()
    }
}
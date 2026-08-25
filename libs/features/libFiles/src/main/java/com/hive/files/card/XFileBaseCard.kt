// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.card

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.hive.adapter.core.AbsCardItemView
import com.hive.adapter.core.CardItemData
import com.hive.files.model.FileCardData
import com.hive.libfiles.R
import com.hive.views.widgets.SwitchImageView

/**
 *
 * @author jiadou
 * @date 4/7/21
 */
abstract class XFileBaseCard(context: Context) : AbsCardItemView(context), View.OnClickListener,
    View.OnLongClickListener, SwitchImageView.OnSwitcherListener {
    var mFileData: FileCardData? = null
    var mData: CardItemData? = null
    var switch_check: SwitchImageView? = null
    var iv_opt: View? = null

    companion object {
        const val EVENT_BIND_DATA = -1
        const val EVENT_SWITCH_MODE = 0
        const val EVENT_SELECTED = 1
        const val EVENT_CLICKED = 2
        const val EVENT_EIDT_LONG_PRESSED = 3
    }

    init {
        setOnClickListener(this)
        setOnLongClickListener(this)
    }

    override fun initView(view: View?) {
        switch_check = getSwitchView()
        iv_opt = getOptView()
        switch_check?.setOnSwitcherListener(this)
        iv_opt?.setOnClickListener {
            postEvent(EVENT_SWITCH_MODE, mData)
        }
    }

    open fun getSwitchView(): SwitchImageView? = findViewById(R.id.switch_check)

    open fun getOptView(): View? = findViewById(R.id.iv_opt)

    override fun bindData(data: CardItemData?) {
        mData = data
        mFileData = data?.data as FileCardData?
        mFileData?.run { bindFileData(this) }
        onEditModelChanged(data?.isEditModel == true)
        onUpdateSelectStatus(mData?.isSelected == true)
        iv_opt?.visibility = if (data?.isEditModel == true) View.GONE else View.VISIBLE
        postEvent(EVENT_BIND_DATA, this to mFileData)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (!this.isEnabled) return false
        return super.dispatchTouchEvent(ev)
    }

    override fun onStateChanged(status: Boolean) {
        mData?.isSelected = status
        postEvent(EVENT_SELECTED, mData?.position)
        onUpdateSelectStatus(mData?.isSelected == true)
    }

    open fun onEditModelChanged(editModel: Boolean) {

    }

    open fun onUpdateSelectStatus(selected: Boolean) {

    }

    abstract fun bindFileData(mFileData: FileCardData)

    override fun onLongClick(v: View?): Boolean {
        if (!mData!!.isEditModel) {
            postEvent(EVENT_SWITCH_MODE, mData)
        } else {
            postEvent(EVENT_EIDT_LONG_PRESSED, mData)
        }
        return true
    }

    override fun onClick(v: View?) {
        if (mData == null) return
        if (mData!!.isEditModel) {
            mData!!.isSelected = mData?.isSelected == false
            postEvent(EVENT_SELECTED, mData?.position)
            onUpdateSelectStatus(mData!!.isSelected)
        } else {
            postEvent(EVENT_CLICKED, mFileData)
        }
    }
}
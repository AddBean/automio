// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.handler.preview

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.hive.anim.AnimUtils
import com.hive.base.BaseLayout
import com.hive.files.event.FileChangedEvent
import com.hive.files.filedb.service.XFileFavService
import com.hive.files.model.FileCardData
import com.hive.files.utils.XFileOperateHelper
import com.hive.files.utils.XFileShareHelper
import com.hive.files.views.SampleDeleteDialog
import com.hive.files.views.XFileDetailDialog
import com.hive.libfiles.R
import com.hive.utils.WorkHandler
import com.hive.utils.statusbar.StatusBarCompat
import com.hive.utils.utils.RelativeDateFormat
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.TextDrawableView
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.Date

/**
 *
 * @author jiadou
 * @date 4/26/21
 */

class XPreviewMenuView(context: Context, attrs: AttributeSet) : BaseLayout(context, attrs),
    View.OnClickListener, WorkHandler.IWorkHandler {

    private var mBackgroundView: View? = null
    private var isMenuShowning: Boolean = true
    private var mParentView: ViewGroup? = null
    var mFile: FileCardData? = null
    var mPreviewFragment: XPreviewFragment? = null
    var mHandler = WorkHandler(this)
    private var iv_back: View? = null
    private var tv_send: TextView? = null
    private var tv_fav: TextDrawableView? = null
    private var tv_delete: TextView? = null
    private var tv_more: TextView? = null
    private var tv_rotate: TextView? = null
    private var tv_detail: TextView? = null
    private var tv_name: TextView? = null
    private var tv_index: TextView? = null
    private var layout_bottom: View? = null
    private var layout_top: View? = null

    override fun initView(view: View?) {
        mParentView = parent as ViewGroup?
        iv_back = view?.findViewById(R.id.iv_back)
        tv_send = view?.findViewById(R.id.tv_send)
        tv_fav = view?.findViewById(R.id.tv_fav)
        tv_delete = view?.findViewById(R.id.tv_delete)
        tv_more = view?.findViewById(R.id.tv_more)
        tv_rotate = view?.findViewById(R.id.tv_rotate)
        tv_detail= view?.findViewById(R.id.tv_detail)
        tv_name = view?.findViewById(R.id.tv_name)
        tv_index = view?.findViewById(R.id.tv_index)
        layout_bottom = view?.findViewById(R.id.layout_bottom)
        layout_top = view?.findViewById(R.id.layout_top)
        iv_back?.setOnClickListener(this)
        tv_send?.setOnClickListener(this)
        tv_fav?.setOnClickListener(this)
        tv_delete?.setOnClickListener(this)
        tv_more?.setOnClickListener(this)
        tv_rotate?.setOnClickListener(this)
    }


    override fun onClick(v: View?) {
        AnimUtils.scaleAnim(v)
        mHandler.removeMessages(WHAT_HIDDEN)
        mHandler.sendEmptyMessageDelayed(WHAT_HIDDEN, 6000)
        when (v?.id) {
            R.id.iv_back -> {
                var cxt = context
                if (cxt is Activity) {
                    cxt.finish()
                }
            }

            R.id.tv_rotate -> {
                mFile?.run {
                    orientation += 90
                    if (orientation > 270)
                        orientation = 0
                    mPreviewFragment?.updateCurrentCard()
                }
            }

            R.id.tv_send -> {
                mFile?.run {
                    XFileShareHelper.shareFilesToSystem(context, arrayListOf<File>().apply {
                        add(newFile())
                    })
                }
            }

            R.id.tv_fav -> {
                mFile?.run {
                    if (tv_fav?.isSelected == true) {
                        XFileFavService.remove(mFile?.filePath)
                    } else {
                        XFileFavService.add(mFile?.filePath)
                    }
                    updateFavStatus()
                }
            }

            R.id.tv_delete -> {
                mFile?.run { optDeleteFiles() }
            }

            R.id.tv_more -> {
                mFile?.run {
                    var cxt = context
                    if (cxt is FragmentActivity) {
                        XFileDetailDialog.show(cxt?.supportFragmentManager!!, this.newFile())
                    }
                }
            }
        }
    }

    fun attachBackgroundView(viewBg: View?) {
        mBackgroundView = viewBg
    }

    private fun updateFavStatus() {
        tv_fav?.isSelected = XFileFavService.hasAdd(mFile?.filePath)
        if (tv_fav?.isSelected == true) {
            tv_fav?.text = resources.getString(com.hive.i8n.R.string.x_file_fav)
            tv_fav?.setTextColor(resources.getColor(com.hive.i8n.R.color.colorRed))
            tv_fav?.setDrawableTop(resources.getDrawable(R.drawable.x_file_liked))
        } else {
            tv_fav?.text = resources.getString(com.hive.i8n.R.string.x_file_fav_no)
            tv_fav?.setTextColor(resources.getColor(com.hive.i8n.R.color.color_ff383838))
            tv_fav?.setDrawableTop(resources.getDrawable(R.drawable.x_file_like))
        }
    }

    fun updateFileStatus(fileList: MutableList<FileCardData>?, position: Int) {
        mFile = fileList?.get(position)
        tv_name?.text = mFile?.fileName
        updateFavStatus()
        tv_detail?.text = RelativeDateFormat.format(Date(mFile?.lastModified ?: 0))
        tv_index?.text = (position + 1).toString() + "/" + fileList?.size.toString()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        ensureShow()
    }

    private fun optDeleteFiles() {
        val dialog = SampleDeleteDialog(context)
        dialog.setDialogTitle(context.getString(com.hive.i8n.R.string.x_file_opt_delete_tips))
        dialog.setDialogContent(context.getString(com.hive.i8n.R.string.x_file_opt_delete_msg))
        dialog.setLeftText(context.getString(com.hive.i8n.R.string.x_file_cancel))
        dialog.setRightText(context.getString(com.hive.i8n.R.string.x_file_opt_delete_btn))
        dialog.setOnDialogListener { isRight ->
            dialog.dismiss()
            if (isRight) {
                XFileOperateHelper.deleteFiles(
                    context, arrayListOf<File>().apply {
                        add(mFile!!.newFile())
                    }, dialog.isRecycleToBin(),
                    object : XFileOperateHelper.OnFileOperateListener {
                        override fun onSuccess() {
                            mPreviewFragment?.deleteFile(mFile)
                            EventBus.getDefault().post(FileChangedEvent())
                        }

                        override fun onFailure(e: Throwable) {
                            CommonToast.getInstance()
                                .showToast(context.getString(com.hive.i8n.R.string.x_file_opt_delete_fail) + e.message)
                        }
                    })
            }
        }
        dialog.show()
    }

    val WHAT_HIDDEN = 1;
    override fun handleMessage(msg: Message?) {
        when (msg?.what) {
            WHAT_HIDDEN -> {
                if (isMenuShowning) {
                    ensureHidden()
                }
            }
        }
    }

    fun ensureShow() {
        isMenuShowning = true
        AnimUtils.fadeOutAnim(mBackgroundView, 300L, object : AnimUtils.AnimListener() {
            override fun onOver(v: View?) {
                super.onOver(v)
                mBackgroundView?.alpha = 0f
                StatusBarCompat.setStatusBarColor(context as Activity, Color.WHITE)
            }
        })
        AnimUtils.startYTranslation(layout_top, -layout_top!!.measuredHeight, 0)
        AnimUtils.startYTranslation(layout_bottom, layout_top!!.measuredHeight, 0)
        mHandler?.removeMessages(WHAT_HIDDEN)
        mHandler?.sendEmptyMessageDelayed(WHAT_HIDDEN, 4000)
    }

    fun ensureHidden() {
        isMenuShowning = false

        AnimUtils.fadeInAnim(mBackgroundView, 300L, object : AnimUtils.AnimListener() {
            override fun onOver(v: View?) {
                super.onOver(v)
                mBackgroundView?.alpha = 1f
                StatusBarCompat.setStatusBarColor(context as Activity, Color.BLACK)
            }
        })
        AnimUtils.startYTranslation(layout_top, 0, -layout_top!!.measuredHeight)
        AnimUtils.startYTranslation(layout_bottom, 0, layout_top!!.measuredHeight)
    }

    fun onPreviewClicked() {
        if (isMenuShowning) {
            ensureHidden()
        } else {
            ensureShow()
        }
    }

    override fun getLayoutId() = R.layout.x_preview_menu_view


}
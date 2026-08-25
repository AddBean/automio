// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.hive.adapter.core.AbsCardItemView
import com.hive.anim.AnimUtils
import com.hive.base.BaseLayout
import com.hive.files.IOperateMenuInterface
import com.hive.files.XFileListFragment
import com.hive.files.XFileSelectorFolderDialog1
import com.hive.files.card.XFileBaseCard
import com.hive.files.event.FileChangedEvent
import com.hive.files.utils.XFileOperateHelper
import com.hive.files.utils.XFileShareHelper
import com.hive.libfiles.R
import com.hive.utils.GlobalApp
import com.hive.views.SampleDialog
import com.hive.views.widgets.CommonToast
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/9/21
 */

class XFileOperateMenuView(context: Context) : BaseLayout(context), View.OnClickListener,
    XFileListFragment.OnCardEventListener {
    private lateinit var mMenuMoreView: XFileOperateMenuMoreView
    private lateinit var mOpreateMenuImpl: IOperateMenuInterface
    private lateinit var mXFileListFragment: XFileListFragment
    var mCurrentSelected: List<File>? = null

    enum class OperateViewState {
        HIDDEN, EDIT
    }

    private var btn_file_select_info: TextView? = null
    private var btn_file_select: View? = null
    private var btn_file_close: View? = null
    private var btn_file_delete: View? = null
    private var btn_file_move: View? = null
    private var btn_file_copy: View? = null
    private var btn_file_rename: View? = null
    private var btn_file_new_fold: View? = null
    private var btn_file_share: View? = null
    private var btn_file_more: View? = null
    private var btn_file_recover: View? = null
    private var layout_top: View? = null
    private var layout_buttom: View? = null
    var mViewState = OperateViewState.HIDDEN

    fun attachFileListFragment(fileListFragment: XFileListFragment) {
        mXFileListFragment = fileListFragment
        mXFileListFragment.addOnCardEventListener(this)
    }

    fun attachOperateMenuInterface(menuInterface: IOperateMenuInterface) {
        mOpreateMenuImpl = menuInterface
    }

    override fun initView(view: View?) {
        btn_file_select_info = view?.findViewById(R.id.btn_file_select_info)
        btn_file_select = view?.findViewById(R.id.btn_file_select)
        btn_file_close = view?.findViewById(R.id.btn_file_close)
        btn_file_delete = view?.findViewById(R.id.btn_file_delete)
        btn_file_move = view?.findViewById(R.id.btn_file_move)
        btn_file_copy = view?.findViewById(R.id.btn_file_copy)
        btn_file_rename = view?.findViewById(R.id.btn_file_rename)
        btn_file_new_fold = view?.findViewById(R.id.btn_file_new_fold)
        btn_file_share = view?.findViewById(R.id.btn_file_share)
        btn_file_more = view?.findViewById(R.id.btn_file_more)
        btn_file_recover = view?.findViewById(R.id.btn_file_recover)
        layout_top = view?.findViewById(R.id.layout_top)
        layout_buttom = view?.findViewById(R.id.layout_buttom)

        mMenuMoreView = XFileOperateMenuMoreView(context, this)
        btn_file_select?.setOnClickListener(this)
        btn_file_close?.setOnClickListener(this)
        btn_file_delete?.setOnClickListener(this)
        btn_file_move?.setOnClickListener(this)
        btn_file_copy?.setOnClickListener(this)
        btn_file_rename?.setOnClickListener(this)
        btn_file_new_fold?.setOnClickListener(this)
        btn_file_share?.setOnClickListener(this)
        btn_file_more?.setOnClickListener(this)
        btn_file_recover?.setOnClickListener(this)
    }

    private fun showMoreMenu(anchorView: View) {
        mMenuMoreView.width = ViewGroup.LayoutParams.WRAP_CONTENT
        mMenuMoreView.height = ViewGroup.LayoutParams.WRAP_CONTENT
        try {
            mMenuMoreView.showAsDropDown(
                anchorView,
                0,
                -mMenuMoreView.getMeasureHeight() - 8 * DP,
                Gravity.BOTTOM
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun getLayoutId(): Int = R.layout.x_file_operate_menu_view

    override fun onClick(v: View?) {
        AnimUtils.scaleAnim(v)
        when (v?.id) {
            R.id.btn_file_more -> {
                showMoreMenu(v);
            }

            R.id.btn_file_select -> {
                if (!btn_file_select!!.isSelected) {
                    mXFileListFragment?.selectAll()
                } else {
                    mXFileListFragment?.disSelectAll()
                }
                btn_file_select!!.isSelected = !btn_file_select!!.isSelected
                updateOperateViewState()
            }
            //新建
            R.id.btn_file_new_fold -> {
                optNewFiles()
            }
            //退出
            R.id.btn_file_close -> {
                setOperateViewState(OperateViewState.HIDDEN)
            }
            //删除
            R.id.btn_file_delete -> {
                optDeleteFiles()
            }
            //移动
            R.id.btn_file_move -> {
                optMoveFiles()
            }
            //恢复
            R.id.btn_file_recover -> {
                optRecoverFiles()
            }
            //复制
            R.id.btn_file_copy -> {
                optCopyFiles()
            }
            //重命名
            R.id.btn_file_rename -> {
                optRenameFile()
            }
            //分享
            R.id.btn_file_share -> {
                XFileShareHelper.shareFilesToSystem(
                    context,
                    mXFileListFragment.getAllSelectedFiles()
                )
            }
        }
    }

    private fun optNewFiles() {
        XFileOperateHelper.newFile(
            context,
            mXFileListFragment.mCurrentFolder.newFile(),
            object : XFileOperateHelper.OnFileOperateListener {
                override fun onSuccess() {
                    EventBus.getDefault().post(FileChangedEvent())
                    setOperateViewState(OperateViewState.HIDDEN)
                }

                override fun onFailure(e: Throwable) {
                    CommonToast.getInstance().showToast(e.message)
                }
            })
    }

    private fun optRenameFile() {
        XFileOperateHelper.renameFile(
            context,
            mXFileListFragment.getAllSelectedFiles()[0],
            object : XFileOperateHelper.OnFileOperateListener {
                override fun onSuccess() {
                    EventBus.getDefault().post(FileChangedEvent())
                    setOperateViewState(OperateViewState.HIDDEN)
                }

                override fun onFailure(e: Throwable) {
                    CommonToast.getInstance().showToast(e.message)
                }
            })
    }

    private fun optCopyFiles() {
        XFileSelectorFolderDialog1.show(
            mOpreateMenuImpl.getOperateFragmentManager(),
            GlobalApp.getString(com.hive.i8n.R.string.copy_selected_btn),
            object : XFileSelectorFolderDialog1.OnFileSelectedListener {
                override fun onFileSelected(file: List<File>) {
                    XFileOperateHelper.copyFiles(
                        context, mXFileListFragment.getAllSelectedFiles(), file[0],
                        object : XFileOperateHelper.OnFileOperateListener {
                            override fun onSuccess() {
                                EventBus.getDefault().post(FileChangedEvent())
                                setOperateViewState(OperateViewState.HIDDEN)
                            }

                            override fun onFailure(e: Throwable) {
                                CommonToast.getInstance().showToast(e.message)
                            }
                        })
                }
            })

    }

    private fun optRecoverFiles() {

        val dialog = SampleDialog(context)
        dialog.setDialogTitle(context.getString(com.hive.i8n.R.string.x_file_opt_recover_tips))
        dialog.setDialogContent(context.getString(com.hive.i8n.R.string.x_file_opt_recover_msg))
        dialog.setLeftText(context.getString(com.hive.i8n.R.string.x_file_cancel))
        dialog.setRightText(context.getString(com.hive.i8n.R.string.ok))
        dialog.setOnDialogListener { isRight ->
            dialog.dismiss()
            if (isRight) {
                XFileOperateHelper.recoverFilesToTrash(
                    context, mXFileListFragment.getAllSelectedFiles(),
                    object : XFileOperateHelper.OnFileOperateListener {
                        override fun onSuccess() {
                            EventBus.getDefault().post(FileChangedEvent())
                            setOperateViewState(OperateViewState.HIDDEN)
                        }

                        override fun onFailure(e: Throwable) {
                            CommonToast.getInstance().showToast(e.message)
                        }
                    })
            }
        }
        dialog.show()
    }

    private fun optMoveFiles() {
        XFileSelectorFolderDialog1.show(
            mOpreateMenuImpl.getOperateFragmentManager(),
            GlobalApp.getString(com.hive.i8n.R.string.move_selected_btn),
            object : XFileSelectorFolderDialog1.OnFileSelectedListener {
                override fun onFileSelected(file: List<File>) {
                    XFileOperateHelper.moveFiles(
                        context, mXFileListFragment.getAllSelectedFiles(), file[0],
                        object : XFileOperateHelper.OnFileOperateListener {
                            override fun onSuccess() {
                                EventBus.getDefault().post(FileChangedEvent())
                                setOperateViewState(OperateViewState.HIDDEN)
                            }

                            override fun onFailure(e: Throwable) {
                                CommonToast.getInstance().showToast(e.message)
                            }
                        })
                }
            })
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
                    context, mXFileListFragment.getAllSelectedFiles(), dialog.isRecycleToBin(),
                    object : XFileOperateHelper.OnFileOperateListener {
                        override fun onSuccess() {
                            EventBus.getDefault().post(FileChangedEvent())
                            setOperateViewState(OperateViewState.HIDDEN)
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

    fun setOperateViewState(state: OperateViewState) {
        mViewState = state

        clearViewVisibility()
        when (state) {
            OperateViewState.HIDDEN -> {
                ensureHidden()
                mXFileListFragment?.disableEdit()
            }

            OperateViewState.EDIT -> {
                ensureShow()
                mXFileListFragment?.enableEdit()
                layout_top?.visibility = View.VISIBLE
                layout_buttom?.visibility = View.VISIBLE
            }
        }
        updateOperateViewState()
    }

    private fun ensureShow() {
        var containerView = mOpreateMenuImpl.getOperateMenuContainerView()
        if (!this.isSelected) {
            if (parent == null)
                containerView.addView(this)
            isSelected = true
            mXFileListFragment.onMenuShow()
            var height = context.resources.getDimension(R.dimen.x_file_menu_height)
            visibility = View.VISIBLE
            AnimUtils.startYTranslation(
                layout_top,
                -height.toInt(),
                0,
                object : AnimUtils.AnimListener() {
                    override fun onBegin(v: View?) {
                        layout_top?.translationY = 0f
                        visibility = View.VISIBLE
                    }
                })
            AnimUtils.startYTranslation(
                layout_buttom,
                height.toInt(),
                0,
                object : AnimUtils.AnimListener() {
                    override fun onBegin(v: View?) {
                        layout_buttom?.translationY = 0f
                        visibility = View.VISIBLE
                    }
                })
        }
    }

    private fun ensureHidden() {
        if (this.isSelected) {
            mXFileListFragment.onMenuHidden()
            isSelected = false
            var height = context.resources.getDimension(R.dimen.x_file_menu_height)
            AnimUtils.startYTranslation(
                layout_top,
                0,
                -height.toInt(),
                object : AnimUtils.AnimListener() {
                    override fun onBegin(v: View?) {
                        layout_top?.translationY = 0f
                        visibility = View.VISIBLE
                    }
                })
            AnimUtils.startYTranslation(
                layout_buttom,
                0,
                height.toInt(),
                object : AnimUtils.AnimListener() {
                    override fun onBegin(v: View?) {
                        layout_buttom?.translationY = 0f
                        visibility = View.VISIBLE
                    }

                    override fun onOver(v: View?) {
                        super.onOver(v)
                        visibility = View.GONE
                    }
                })
        }
    }

    fun updateOperateViewState() {
        mCurrentSelected = mXFileListFragment?.getAllSelectedFiles()
        btn_file_select_info?.text =
            context.getString(com.hive.i8n.R.string.x_file_selected_msg, mCurrentSelected?.size)
        changeViewState(btn_file_rename, mCurrentSelected?.size == 1)
        changeViewState(btn_file_delete, mCurrentSelected?.size ?: 0 > 0)
        changeViewState(btn_file_copy, mCurrentSelected?.size ?: 0 > 0)
        changeViewState(btn_file_move, mCurrentSelected?.size ?: 0 > 0)
        changeViewState(btn_file_share, mCurrentSelected?.size ?: 0 > 0)
        changeViewState(btn_file_recover, mCurrentSelected?.size ?: 0 > 0)
        mMenuMoreView.onChangeViewState(mCurrentSelected)
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

    private fun clearViewVisibility() {
        layout_top?.visibility = View.GONE
        layout_buttom?.visibility = View.GONE
    }


    override fun onCardEvent(cardEvent: Int, args: Any?, itemView: AbsCardItemView?) {
        when (cardEvent) {
            XFileBaseCard.EVENT_SWITCH_MODE -> {
                setOperateViewState(OperateViewState.EDIT)
            }

            XFileBaseCard.EVENT_SELECTED -> {
                updateOperateViewState()
            }
        }
    }

    fun onBackPressed(): Boolean {
        return when (mViewState) {
            OperateViewState.HIDDEN -> false
            else -> {
                setOperateViewState(OperateViewState.HIDDEN)
                true
            }
        }
    }

    fun onDestroyView() {
        mXFileListFragment?.removeOnCardEventListener(this)
    }

    fun getOperateFragmentManager(): FragmentManager = mOpreateMenuImpl.getOperateFragmentManager()


    fun getFileSelect() = btn_file_select

    fun getFileClose() = btn_file_close

    fun getFileDelete() = btn_file_delete

    fun getFileMove() = btn_file_move

    fun getFileCopy() = btn_file_copy

    fun getFileRename() = btn_file_rename

    fun getFileNewFold() = btn_file_new_fold

    fun getFileShare() = btn_file_share

    fun getFileMore() = btn_file_more

    fun getFileRecover() = btn_file_recover
}
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.views

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.hive.anim.AnimUtils
import com.hive.base.BaseFragment
import com.hive.editor.R
import com.hive.files.XFileSelectorFolderDialog1
import com.hive.files.model.FileCardData
import com.hive.richeditor.ActivityEditor.Companion.FILE_KEY
import com.hive.richeditor.core.RichEditor
import com.hive.richeditor.editordb.service.EditHistoryService
import com.hive.richeditor.event.ChangeCharsetEvent
import com.hive.richeditor.views.EditInputDialog.OnConfirmListener
import com.hive.utils.encrypt.Md5Utils
import com.hive.utils.file.FileUtils
import com.hive.utils.thread.UIHandlerUtils
import com.hive.views.IBackListener
import com.hive.views.SampleDialog
import com.hive.views.popmenu.PopMenuManager
import com.hive.views.popmenu.PopMenuView
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.drawer.DrawerView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Created by Administrator on 2017/7/3.
 */
class EditFragment : BaseFragment(), IBackListener {
    private var mFragRecent: EditRecentFragment? = null
    private var mInitContentMd5: String? = null
    var mFile: File? = null
    private var hasSaved = false

    private var edit_menu_layout: EditLayout? = null
    private var editor: RichEditor? = null
    private var iv_more: View? = null
    private var iv_recent: View? = null
    private var iv_btn_back: View? = null
    private var tv_title: TextView? = null
    private var drawer_view: DrawerView?=null


    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        args?.getString(FILE_KEY)?.run {
            mFile = File(this)
        }
    }

    override fun initView() {
        mCharset = StandardCharsets.UTF_8.name()
        edit_menu_layout = view?.findViewById(R.id.edit_menu_layout)
        editor = view?.findViewById(R.id.editor)
        iv_more = view?.findViewById(R.id.iv_more)
        iv_recent = view?.findViewById(R.id.iv_recent)
        iv_btn_back = view?.findViewById(R.id.iv_btn_back)
        tv_title = view?.findViewById(R.id.tv_title)
        drawer_view = view?.findViewById(R.id.drawer_view)

        edit_menu_layout?.attachEditor(context as Activity?, editor)
        registerEvent()
        editor?.setOnTextChangeListener {
            hasSaved = false
        }
        loadFileText(mFile, mCharset)

        iv_more?.setOnClickListener { v ->
            AnimUtils.scaleAnim(v)
            openMoreMenu(v)
        }
        iv_recent?.setOnClickListener {
            AnimUtils.scaleAnim(it)
            mFragRecent?.updateList()
            drawer_view?.drawMenuLeft(null)
        }
        iv_btn_back?.setOnClickListener { v ->
            AnimUtils.scaleAnim(v)
            activity!!.finish()
        }
    }

    /**
     * 打开更多menu
     */
    private fun openMoreMenu(v: View) {

        PopMenuManager.instance.showMenu(v, arrayListOf<String>().apply {
            add(getStr(com.hive.i8n.R.string.editor_menu_recent))
            add(getStr(com.hive.i8n.R.string.editor_menu_save_to))
            add(getStr(com.hive.i8n.R.string.editor_menu_save))
        }, object : PopMenuView<String>(requireContext()) {
            override fun getLayoutId() = R.layout.editor_popmenu_layout
        }, object : PopMenuManager.OnItemClickListener<String> {
            override fun onItemClicked(view: View, data: String, pos: Int) {
                when (pos) {
                    0 -> {
                        mFragRecent?.updateList()
                        drawer_view?.drawMenuLeft(null)
                    }

                    1 -> {
                        saveTo()
                    }

                    2 -> {
                        if (mFile == null) {
                            saveTo()
                        } else {
                            trySaveToLocal(false)
                        }
                    }
                }
            }

        })
    }

    override fun onResume() {
        super.onResume()
        mFragRecent =
            childFragmentManager?.findFragmentById(R.id.frag_recent) as EditRecentFragment?
        mFragRecent?.mOnClickListener = object : EditRecentFragment.OnEditClickListener {
            override fun onDismiss() {
                drawer_view?.drawMenuRight(null)
            }

            override fun onEditorItemClick(file: FileCardData?) {
                loadFileText(file?.newFile(), mCharset)
            }
        }
    }

    @Throws(Exception::class)
    fun checkInput() {
        if (TextUtils.isEmpty(editor?.content)) throw Exception(getStr(com.hive.i8n.R.string.edit_tips_file_not_edit))
    }

    fun loadFileText(file: File?, charset: String) {
        mFile = file
        view?.postDelayed({
            tv_title?.text = mFile?.name ?: getStr(com.hive.i8n.R.string.default_file_name)
            try {
                Thread {
                    var content = ""
                    if (file != null) {
                        if (file.exists()) {
                            EditHistoryService.insertOrUpdate(mFile?.path)
                            val text = FileUtils.readFile(file.path, charset)
                            content = text.toString()
                            mInitContentMd5 = Md5Utils.string2md5(content)
                            UIHandlerUtils.getInstance().executeInMainThread {
                                editor?.setContent(content)
                                mCharset = charset
                            }
                        } else {
                            EditHistoryService.remove(file.path)
                            UIHandlerUtils.getInstance().executeInMainThread {
                                CommonToast.getInstance()
                                    .showToast(com.hive.i8n.R.string.edit_file_may_has_deleted)
                                mFragRecent?.mListHelper?.notifyData(true)
                            }
                        }
                    }

                }.start()
            } catch (e: Exception) {
                editor?.setContent(e.message)
            }
        }, 50)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChangeCharsetEvent(e: ChangeCharsetEvent) {
        mCharset = e.charset
        loadFileText(mFile, mCharset)
    }

    fun saveToLocal(closeActivity: Boolean) {
        if (mFile == null) {
            saveTo()
        } else {
            val dialog = SampleDialog(context!!)
            dialog.setDialogTitle(getString(com.hive.i8n.R.string.editor_dialog_save_title))
            dialog.setDialogContent(getString(com.hive.i8n.R.string.editor_dialog_save_content))
            dialog.setRightText(getStr(com.hive.i8n.R.string.editor_btn_save))
            dialog.setLeftText(getStr(if (closeActivity) com.hive.i8n.R.string.editor_btn_not_save else com.hive.i8n.R.string.editor_btn_cancel))
            dialog.setOnDialogListener { isRight ->
                dialog.dismiss()
                if (isRight) {
                    trySaveToLocal(closeActivity)
                } else {
                    if (closeActivity) {
                        activity!!.finish()
                    }
                }
            }
            dialog.show()
        }
    }

    /**
     * 另存为
     */
    private fun saveTo() {
        val dialog = EditInputDialog(activity!!)
        dialog.setEditText(getStr(com.hive.i8n.R.string.default_file_name))
        dialog.onConfirmListener = object : OnConfirmListener {
            override fun onConfirmed(text: String?) {
                XFileSelectorFolderDialog1.show(
                    fragmentManager!!,
                    getStr(com.hive.i8n.R.string.edit_save_to_btn),
                    object : XFileSelectorFolderDialog1.OnFileSelectedListener {
                        override fun onFileSelected(file: List<File>) {
                            val file = File(file[0].path + File.separator + text)
                            if (file.exists()) {
                                CommonToast.getInstance()
                                    .showToast(com.hive.i8n.R.string.save_error_name_duplicate_toast)
                                return
                            }
                            if (file.exists()) {
                                file.createNewFile()
                            }
                            mFile = file
                            trySaveToLocal(false)
                        }
                    })
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun trySaveToLocal(closeActivity: Boolean) {
        try {
            checkInput()
            EditHistoryService.insertOrUpdate(mFile?.path)
            FileUtils.writeFile(mFile, editor?.content, mCharset)
            hasSaved = true
            CommonToast.getInstance().showToast(com.hive.i8n.R.string.editor_save_success)
        } catch (e: Exception) {
            CommonToast.getInstance().showToast(e.message)
        }
        if (closeActivity) {
            activity!!.finish()
        }
    }

    val isHasChanged: Boolean
        get() {
            return if (editor?.content == null) {
                return false
            } else {
                !TextUtils.equals(mInitContentMd5, Md5Utils.string2md5(editor?.content ?: ""))
            }
        }

    private fun registerEvent() {
        EventBus.getDefault().register(this)
        editor?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            edit_menu_layout?.isClickable = hasFocus
            edit_menu_layout?.alpha = if (hasFocus) 1f else 0.4f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MenuHelper.destory()
        EventBus.getDefault().unregister(this)
    }


    override fun onBackPressed(): Boolean {
        //未被保存，且内容发生变化
        return if (drawer_view?.state == DrawerView.STATE.LEFT) {
            drawer_view?.drawMenuRight(null)
            true
        } else if (isHasChanged && !hasSaved) {
            saveToLocal(true)
            true
        } else {
            false
        }
    }

    public override fun getLayoutId() = R.layout.edit_fragment

    companion object {
        const val RICH_IMAGE_CODE = 10001

        @JvmField
        var mCharset = StandardCharsets.UTF_8.name()
    }
}
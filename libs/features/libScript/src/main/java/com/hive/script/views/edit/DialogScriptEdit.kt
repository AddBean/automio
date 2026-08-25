// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.edit

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.hive.anim.AnimUtils
import com.hive.extension.visibleOrGone
import com.hive.script.R
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.core.ScriptSaver
import com.hive.script.cmd.CmdLog
import com.hive.script.event.RefreshScriptEditEvent
import com.hive.script.extensions.findRootCommand
import com.hive.script.extensions.forEachAllCommand
import com.hive.script.extensions.isContainedUnReachable
import com.hive.script.extensions.updateAllParent
import com.hive.script.views.dialog.DialogParamsManager
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.edit.editor.ListScriptEditView
import com.hive.script.views.edit.xeditor.ScriptXEditorView
import com.hive.script.views.edit.xeditor.XCellLayout
import com.hive.script.views.edit.xeditor.utils.XEditorHelper
import com.hive.script.views.edit.xeditor.utils.XEditorSnapManager
import com.hive.script.views.manager.ScriptEditRunningManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.menu.ScriptControlView
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.script.views.widgets.ScriptMaxMinDialog
import com.hive.script.views.widgets.ScriptMiniEditView
import com.hive.script.views.widgets.ScriptNavigationBar
import com.hive.utils.GlobalApp
import com.hive.utils.OnClickFilteListener
import com.hive.utils.ShareUtils
import com.hive.utils.extends.isLandscape
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.utils.utils.ScreenUtils
import com.hive.utils.utils.ViewUtils
import com.hive.views.StatefulLayout
import com.hive.views.popmenu.PopMenuManager
import com.hive.views.widgets.CommonToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

/**
 *
 * @author jiadou
 * @date 6/10/21
 */
class DialogScriptEdit private constructor(context: Context?) : ScriptMaxMinDialog(context) {

    private val miniEditView = ScriptMiniEditView()

    private var mTitleName: String? = null

    private var curParentCmd: ScriptCommand? = null

    private var mCommand: ScriptCommand? = null

    private var mScripFilePath: String? = null

    private var mPaddingLoadPath: String? = null

    private var firstIn = true

    private var isSaveEnable = false

    private var bottomMenu: View? = null
    private var btnAutoLayout: View? = null
    private var btnFold: View? = null
    private var btnMenu: View? = null
    private var btnMini: View? = null
    private var btnPlay: View? = null
    private var btnRedo: View? = null
    private var btnSaveImage: View? = null
    private var btnShare: View? = null
    private var btnTempSave: View? = null
    private var btnUndo: View? = null
    private var btnUnfold: View? = null
    private var btn_save: View? = null
    private var edit_view: ListScriptEditView? = null
    private var iv_back: ImageView? = null
    private var iv_eye: View? = null
    private var layoutEdit1: View? = null
    private var layoutEdit2: View? = null
    private var layout_state: StatefulLayout? = null
    private var navigation_bar: ScriptNavigationBar? = null
    private var sideMenu: View? = null
    private var tv_title: TextView? = null
    private var x_edit_anchor_view: View? = null
    private var x_edit_view: ScriptXEditorView? = null
    private var x_empty_add: View? = null
    private var x_param_manger: View? = null

    private fun initViewHolder(){
        bottomMenu= findViewById(R.id.bottomMenu)
        btnAutoLayout = findViewById(R.id.btnAutoLayout)
        btnFold = findViewById(R.id.btnFold)
        btnMenu = findViewById(R.id.btnMenu)
        btnMini = findViewById(R.id.btnMini)
        btnPlay = findViewById(R.id.btnPlay)
        btnRedo = findViewById(R.id.btnRedo)
        btnSaveImage = findViewById(R.id.btnSaveImage)
        btnShare = findViewById(R.id.btnShare)
        btnTempSave = findViewById(R.id.btnTempSave)
        btnUndo = findViewById(R.id.btnUndo)
        btnUnfold = findViewById(R.id.btnUnfold)
        btn_save = findViewById(R.id.btn_save)
        edit_view = findViewById(R.id.edit_view)
        iv_back = findViewById(R.id.iv_back)
        iv_eye = findViewById(R.id.iv_eye)
        layoutEdit1 = findViewById(R.id.layoutEdit1)
        layoutEdit2 = findViewById(R.id.layoutEdit2)
        layout_state = findViewById(R.id.layout_state)
        navigation_bar = findViewById(R.id.navigation_bar)
        sideMenu = findViewById(R.id.sideMenu)
        tv_title = findViewById(R.id.tv_title)
        x_edit_anchor_view = findViewById(R.id.x_edit_anchor_view)
        x_edit_view = findViewById(R.id.x_edit_view)
        x_empty_add = findViewById(R.id.x_empty_add)
        x_param_manger = findViewById(R.id.x_param_manger)
    }

    inner class ClickListener : OnClickFilteListener() {
        override fun throttleClick(v: View?) {
            if (x_edit_view!!.isLoading) return
            AnimUtils.scaleAnim(v)
            when (v?.id) {

                R.id.iv_eye -> {
                    switchEditView(iv_eye?.isSelected == false)
                }

                R.id.btn_save -> {
                    tryShowWarningDialog {
                        saveScript(XEditorHelper.snapCellLayout()) {
                            CommonToast.show(com.hive.i8n.R.string.sc_edit_save_success)
                            updateControlMenu()
                            dismiss(null)
                        }
                    }
                }

                R.id.x_empty_add -> {
                    ScriptMenuEditHelper.showAddDialog(
                        context!!,
                        mCommand!!,
                        -1,
                        this@DialogScriptEdit
                    )
                }

                R.id.btnSaveImage -> {
                    x_edit_view?.saveShareImage(mCommand, null)
                }

                R.id.btnTempSave -> {
                    saveScript(XEditorHelper.snapCellLayout()) {
                        CommonToast.show(com.hive.i8n.R.string.sc_edit_save_success2)
                        setSaveEnable(false)
                    }
                }

                R.id.btnShare -> {
                    dismiss(null)
                    x_edit_view?.saveShareImage(mCommand) {
                        ShareUtils.getInstance(GlobalApp.getContext())
                            .shareImageToSystem(File(it))
                    }
                }

                R.id.btnUndo -> {
                    XEditorSnapManager.get().undo()
                    updateUndoRedoStatus()
                }

                R.id.btnRedo -> {
                    XEditorSnapManager.get().redo()
                    updateUndoRedoStatus()
                }

                R.id.btnUnfold -> {
                    if (x_edit_view?.unfoldAll() == false) {
                        CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.sc_unfold_fail))
                    }
                }

                R.id.btnFold -> {
                    if (x_edit_view?.foldAll() == false) {
                        CommonToast.show(GlobalApp.getString(com.hive.i8n.R.string.sc_fold_fail))
                    }
                }

                R.id.btnMenu -> {
                    showEditMenu(v)
                }

                R.id.btnMini -> {
                    miniView(x_edit_view!!)
                }

                R.id.btnPlay -> {
                    ScriptEditRunningManager.runCommand(
                        this@DialogScriptEdit,
                        mCommand?.commandQueue?.firstOrNull(),
                        true
                    )
                }

                R.id.btnAutoLayout -> {
                    x_edit_view?.autoLayout()
                }

                R.id.x_param_manger -> {
                    DialogParamsManager(context).show()
                }

                R.id.iv_back -> {
                    if (curParentCmd == null) {
                        if (isSaveEnable) {
                            tryShowConfirmDialog {
                                dismiss()
                            }
                        } else {
                            dismiss()
                        }
                    } else {
                        performBack()
                    }
                }
            }
        }

    }

    fun setSaveEnable(tempSaveEnable: Boolean) {
        isSaveEnable = tempSaveEnable
        btnTempSave?.isEnabled = isSaveEnable
        btnTempSave?.alpha = if (isSaveEnable) 1f else 0.3f
    }

    override fun isTouchOutsideDismissed() = false

    private fun showEditMenu(v: View) {
        val ls = GlobalApp.getResources().getStringArray(com.hive.i8n.R.array.sc_edit_menu_array).toList()
        val itemHeight = 40 * GlobalApp.DP
        var overHeight = ls.size * itemHeight + itemHeight + 32 * GlobalApp.DP
        if (overHeight < 0) overHeight = 0
        PopMenuManager.instance.showMenu(
            v,
            -40 * GlobalApp.DP,
            0,
            ls,
            object : PopMenuManager.OnItemClickListener<String> {
                override fun onItemClicked(view: View, title: String, pos: Int) {
                    showBatchDelayEditor()
                }
            })
    }

    private fun showBatchDelayEditor() {
        val tempCmd = CmdLog()
        tempCmd.startDelay = ScriptConst.Cmd_Delay_Default
        tempCmd.endDelay = ScriptConst.Cmd_Delay_Default
        val editView = ScriptEditFactory.createItemEditView(context, tempCmd, true)
        DialogScriptCardEdit(context)
            .setTitle(GlobalApp.getString(com.hive.i8n.R.string.sc_name_batch_delay))
            .setEdtView(editView)
            .setOnInflateFinished {
                editView.bindCommand(tempCmd)
            }
            .setOnConfirmClicked { dialog ->
                try {
                    editView.checkCommandOrThrowError()
                    mCommand?.forEachAllCommand {
                        if (it.isSupportDelay()) {
                            it.startDelay = tempCmd.startDelay
                            it.endDelay = tempCmd.endDelay
                        }
                    }
                    notifyData()
                    dialog.dismiss()
                    XEditorSnapManager.get().save(mCommand)
                    updateUndoRedoStatus()
                } catch (e: Exception) {
                    CommonToast.show(e.message)
                }
            }.show()
    }

    override fun initWindow() {
        initViewHolder()
        XEditorHelper.resetCellLayout()
        XEditorSnapManager.get().clear()
        val listener = ClickListener()
        ScriptMenuManager.hiddenMenuView()
        x_edit_view?.anchorView = x_edit_anchor_view
        edit_view?.dialogView = this
        layout_state?.showProgress()
        btn_save?.setOnClickListener(listener)
        iv_back?.setOnClickListener(listener)
        iv_eye?.setOnClickListener(listener)
        x_empty_add?.setOnClickListener(listener)
        x_param_manger?.setOnClickListener(listener)
        btnSaveImage?.setOnClickListener(listener)
        btnTempSave?.setOnClickListener(listener)
        btnUndo?.setOnClickListener(listener)
        btnRedo?.setOnClickListener(listener)
        btnFold?.setOnClickListener(listener)
        btnUnfold?.setOnClickListener(listener)
        btnShare?.setOnClickListener(listener)
        btnMenu?.setOnClickListener(listener)
        btnMini?.setOnClickListener(listener)
        btnPlay?.setOnClickListener(listener)
        btnAutoLayout?.setOnClickListener(listener)
        x_edit_view?.dialogView = this
        updateStatus()
        switchEditView(isXEditor = true, reload = false)
        updateLayout()
        navigation_bar?.mNavigationListener = object : ScriptNavigationBar.INavigationListener {
            override fun onNavigationClicked(cmd: ScriptCommand) {
                if (cmd is ScriptCommandRoot) {
                    mCommand?.getRootScript()?.run {
                        loadData(this) {
                            x_edit_view?.resetLocation()
                        }
                    }
                } else {
                    loadData(cmd) {
                        x_edit_view?.resetLocation()
                    }
                }
            }
        }
    }

    private fun updateLayout() {
        ViewUtils.setMargins(
            bottomMenu,
            0,
            0,
            0,
            if (ScreenUtils.isLandscape()) 12 * GlobalApp.DP else 48 * GlobalApp.DP
        )
    }


    private fun switchEditView(isXEditor: Boolean, reload: Boolean = true) {
        if (isXEditor && x_edit_view!!.isLoading) {
            return
        }
        iv_eye?.isSelected = isXEditor
        layoutEdit1?.visibleOrGone(!isXEditor)
        layoutEdit2?.visibleOrGone(isXEditor)
        if (reload) {
            if (isXEditor) {
                x_edit_view?.post {
                    x_edit_view?.notifyData()
                }
            } else {
                edit_view?.post {
                    edit_view?.notifyData()
                }
            }
        }
    }

    override fun onShow() {
        super.onShow()
        if (mPaddingLoadPath != null)
            loadPath(mPaddingLoadPath)
        mPaddingLoadPath = null
        ScriptManager.stopPlay()
        ScriptMenuManager.hiddenMenuView()
    }


    fun setTitleName(name: String): DialogScriptEdit {
        mTitleName = name
        return this
    }

    fun setScriptPath(path: String): DialogScriptEdit {
        mPaddingLoadPath = path
        return this
    }

    fun loadPath(path: String?): DialogScriptEdit {
        mScripFilePath = path
        path?.run {
            ScriptEditHelper.instance.loadPath(path) {
                mCommand = it
                XEditorSnapManager.get()
                    .setRevertCommand(it)
                updateData()
            }
        }

        return this
    }

    fun loadRoot(root: ScriptCommandRoot): DialogScriptEdit {
        post {
            ScriptEditHelper.instance.loadRoot(root) {
                mCommand = it
                updateData()
            }
        }
        return this
    }

    fun loadData(itemCmd: ScriptCommand, anim: Boolean = true, onFinish: (() -> Unit)? = null) {
        mCommand = itemCmd
        x_edit_view?.unfold(cmd = itemCmd)
        curParentCmd = mCommand?.parentCommand
        x_empty_add?.visibleOrGone(mCommand?.commandQueue?.isEmpty() == true)
        updateData(anim, onFinish)
    }

    fun resetLocation() {
        x_edit_view?.resetLocation()
    }

    private fun performBack() {
        curParentCmd?.run {
            mCommand = this
        }
        curParentCmd = mCommand?.parentCommand
        updateData()
    }

    fun updateData(anim: Boolean = true, onFinish: (() -> Unit)? = null) {
        mCommand?.run {
            edit_view?.submitData(this)
            x_edit_view?.loadData(this, anim, onFinish)
        }
        updateStatus()
        navigation_bar?.updateBar(mCommand)
        updateUndoRedoStatus()
        layout_state?.showContent()
        x_empty_add?.visibleOrGone(mCommand?.commandQueue?.isEmpty() == true)
        if (firstIn) {
            firstIn = false
            resetLocation()
            setSaveEnable(false)
        } else {
            setSaveEnable(true)
        }
    }

    fun notifyData() {
        edit_view?.notifyData()
        x_edit_view?.notifyData()
    }

    fun updateStatus() {
        if (curParentCmd == null) {
            iv_back?.setImageResource(com.hive.views.R.drawable.icon_close)
            btn_save?.visibility = View.VISIBLE
            tv_title?.text = mTitleName ?: getString(com.hive.i8n.R.string.sc_script_edit_title)
        } else {
            iv_back?.setImageResource(com.hive.views.R.drawable.icon_back)
            btn_save?.visibility = View.GONE
            tv_title?.setText(com.hive.i8n.R.string.sc_script_edit_title2)
        }
        sideMenu?.visibleOrGone(XEditorHelper.editMode)
        bottomMenu?.visibleOrGone(XEditorHelper.editMode)
        x_param_manger?.visibleOrGone(XEditorHelper.editMode)
        btn_save?.visibleOrGone(XEditorHelper.editMode)
//        navigation_bar?.visibleOrGone(XEditorHelper.editMode)
        updateUndoRedoStatus()
    }


    private fun updateControlMenu() {
        ScriptEditHelper.instance.scriptMain?.takeIf { it.scriptPath != null }?.run {
            ScriptRecordHelper.instance.reset(this)
            ScriptMenuManager.getMenuView()?.updateCurrentStatus()
        }
    }

    @Subscribe
    fun onRefresh(event: RefreshScriptEditEvent?) {
        val e = event?.snapData ?: return
        XEditorHelper.setCellLayout(e.layout)
        e.cmd?.updateAllParent()
        e.cmd?.findRootCommand().takeIf { it is ScriptCommandRoot }?.run {
            ScriptEditHelper.instance.scriptMain = this as ScriptCommandRoot
        }
        loadData(e.cmd ?: return, false)
        updateUndoRedoStatus()
    }

    private fun isRootCommand(): Boolean {
        return mCommand is ScriptCommandRoot
    }

    fun updateUndoRedoStatus() {
        btnUndo?.isEnabled = XEditorSnapManager.get().isUndoEnable()
        btnRedo?.isEnabled = XEditorSnapManager.get().isRedoEnable()
        btnRedo?.alpha = if (btnRedo?.isEnabled == true) 1f else 0.5f
        btnUndo?.alpha = if (btnUndo?.isEnabled == true) 1f else 0.5f
        btnPlay?.isEnabled = isRootCommand()
        btnPlay?.alpha = if (btnPlay?.isEnabled == true) 1f else 0.5f
        setSaveEnable(XEditorSnapManager.get().isAnyEnable())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        EventBus.getDefault().register(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EventBus.getDefault().unregister(this)
    }

    override fun dismiss(onDismissFun: (() -> Unit)?): BaseScriptDialog {
        restoreMenu()
        super.dismiss(onDismissFun)
        return this
    }

    override fun onDismiss() {
        super.onDismiss()
        XEditorHelper.stopParse()
    }

    private fun restoreMenu() {
        ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.MAIN_MENU)
        ScriptInterpreter.getDefault().stopExecute()
        ScriptRecordManager.hiddenRecordView()
        ScriptMenuManager.showMenuView()
        updateControlMenu()
        if (fromSource == ScriptConst.From.FROM_SCRIPT_LIST
            || fromSource == ScriptConst.From.FROM_SCRIPT_LIST_NEW
        ) {
            post {
//                DialogScriptList2(context).show()
            }
        }
    }


    private fun saveScript(layout: XCellLayout?, onSaved: (() -> Unit)?) {
        if (!XEditorHelper.editMode) {
            onSaved?.invoke()
            return
        }
        ScriptEditHelper.instance.scriptMain?.run {
            if (this.scriptPath == null) {
                ScriptManager.showSaveDialog(this, XEditorHelper.snapCellLayout()) {
                    onSaved?.invoke()
                }
            } else {
                ScriptSaver.saveToLocalWithLoading(File(this.scriptPath!!).name, this, layout) {
                    onSaved?.invoke()
                }
            }
        }
    }

    private fun tryShowWarningDialog(confirm: () -> Unit) {
        if (mCommand?.isContainedUnReachable() == true) {
            DialogScriptAlert(context)
                .setTitle(com.hive.i8n.R.string.sc_edit_untouchable_confirm_title)
                .setContent(com.hive.i8n.R.string.sc_edit_untouchable_confirm_msg)
                .setConfirmText(com.hive.i8n.R.string.sc_edit_untouchable_confirm_ok)
                .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                    override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                        dialog.dismiss()
                        if (!isCancel) {
                            confirm.invoke()
                        }
                    }
                }).show()
        } else {
            confirm.invoke()
        }
    }

    private fun tryShowConfirmDialog(confirm: () -> Unit) {
        if (!XEditorHelper.editMode) {
            confirm.invoke()
            return
        }
        DialogScriptAlert(context)
            .setTitle(com.hive.i8n.R.string.sc_edit_confirm_title)
            .setContent(com.hive.i8n.R.string.sc_edit_confirm_msg)
            .setConfirmText(com.hive.i8n.R.string.sc_edit_confirm_ok)
            .setCancelText(com.hive.i8n.R.string.sc_edit_confirm_no_save)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                    dialog.dismiss()
                    if (!isCancel) {
                        tryShowWarningDialog {
                            saveScript(XEditorHelper.snapCellLayout()) {
                                CommonToast.show(com.hive.i8n.R.string.sc_edit_save_success)
                                updateControlMenu()
                                dismiss(null)
                            }
                        }
                    }
                    confirm.invoke()
                }
            }).show()
    }

    override fun getMiniEditView(): ScriptMiniEditView {
        return miniEditView
    }

    override fun onMaxView() {
        ScriptMenuManager.hiddenMenuView()
    }

    override fun onMiniView() {
        ScriptMenuManager.hiddenMenuView()
    }

    override fun getMarginParams() =
        arrayOf(0, if (DeviceCompatHelper.isLandscape()) 0 else 60 * DP, 0, 0)

    override fun getWindowLayoutId() = R.layout.script_edit_view


    override fun getWidthByOrientation(): Int {
        return if (context.isLandscape())  620 * DP else FrameLayout.LayoutParams.MATCH_PARENT
    }

    companion object {

        fun create(scriptMate: ScriptMate?): DialogScriptEdit? {
            XEditorHelper.editMode = scriptMate?.hasControlEdit() ?: true
            if (scriptMate == null || scriptMate.hasControlEdit() || scriptMate.hasControlView()) {
                checkStack()
                if (isShowing()) {
                    getEditDialog()?.getMiniEditView()?.startWarningAnim()
                    CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_waring_edit_panel_other_open)
                    return null
                }
                if (!ScriptManager.checkAccessibility()) {
                    return DialogScriptEdit(GlobalApp.getContext())
                }
            } else {
                CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_no_permission_edit)
            }
            return null
        }

        fun isShowing(): Boolean {
            val isInStack = getDialogStack().any { it is DialogScriptEdit }
            if (isInStack) {
                val dialog = getEditDialog()
                dialog ?: return false
                return if (!dialog.isShown && !dialog.getMiniEditView().isShown) {
                    false
                } else {
                    true
                }
            }
            return false
        }

        private fun checkStack() {
            if (getDialogStack().any { it is DialogScriptEdit }) {
                val dialog = getEditDialog()
                dialog ?: return
                //如果小视图和大视图都没存在，则说明有问题，直接移除
                if (!dialog.isShown && !dialog.getMiniEditView().isShown) {
                    stackRemove(dialog)
                    dialog.getMiniEditView().removeToWindow()
                }
            }
        }

        fun getEditDialog(): DialogScriptEdit? {
            return getDialogStack().firstOrNull { it is DialogScriptEdit } as DialogScriptEdit?
        }

    }
}

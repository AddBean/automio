// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hive.anim.AnimUtils
import com.hive.base.BaseLayout
import com.hive.extension.visibleOrGone
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IScriptProvider
import com.hive.richeditor.EditorHelper
import com.hive.script.ActivitySelectorWrapper
import com.hive.script.ActivityWorkflowDetail
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptKeyStoreManager
import com.hive.script.base.ScriptRecordHelper
import com.hive.script.base.core.ScriptInterpreterObserver
import com.hive.script.base.core.ScriptThreadManager
import com.hive.script.event.OnMcpToolEvent
import com.hive.script.event.RefreshScriptListEvent
import com.hive.script.event.RefreshScriptListInitEvent
import com.hive.script.utils.DialogUtils
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.ScriptShareHelper
import com.hive.script.utils.bundle.BundleImportHelper
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.cards.ScriptItemView
import com.hive.script.views.dialog.DialogCommonSelector
import com.hive.script.views.dialog.DialogInputMessage
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.dialog.DialogScriptInfo
import com.hive.script.views.dialog.DialogScriptLoading
import com.hive.script.views.dialog.DialogShareConfirm
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.record.ScriptRecordViewManager
import com.hive.script.views.schedule.DialogScriptScheduleTimer
import com.hive.utils.GlobalApp
import com.hive.utils.encrypt.Md5Utils
import com.hive.utils.file.FileUtils
import com.hive.views.DialogAlertHelper
import com.hive.views.StatefulLayout
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.widgets.CommonToast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

/**
 *
 * @author jiadou
 * @date 6/11/21
 */
abstract class ScriptManagerLayout(context: Context, attributes: AttributeSet?) :
    BaseLayout(context, attributes), ListRecyclerItemView.OnItemEventListener,
    ScriptInterpreterObserver.InterpreterExecuteObserver {

    interface IDialogOptionInterface {

        fun onDialogDismiss()

        fun onDialogShow()
    }

    var dialogOptionHandler: IDialogOptionInterface? = null

    private var isEditModel = false

    constructor(context: Context) : this(context, null)

    override fun initView(p0: View?) {
        ScriptRecordManager.hiddenRecordView()
        getListRecyclerView().setItemViewFactory(object : IListRecyclerViewFactory {
            override fun createItemView(viewType: Int) =
                getListItemView().apply { onItemEventListener = this@ScriptManagerLayout }
        })
        post {
            initEvent()
            updateScriptList()
            ScriptInterpreterObserver.registerInterpreterObserver(this)
        }
    }

    private fun initEvent() {
        getCloseButtonView()?.setOnClickListener {
            dismissIfDialog()
        }
        getEditButtonView()?.setOnClickListener {
            onItemEvent(null, ScriptItemView.Opt.EVENT_SWITCH_MODE)
        }
        getImportButtonView()?.setOnClickListener {
            dismissIfDialog()
            importScript()
        }
        getLayoutStateView()?.setOnClickListener {
            dismissIfDialog()
        }
        getSelectButtonView()?.setOnClickListener {
            selectAllItems()
        }
        getDeleteButtonView()?.setOnClickListener {
            deleteItems()
        }
        getCancelButtonView()?.setOnClickListener {
            switchEditMode()
        }
        getLayoutTaskAddView()?.setOnClickListener {
            AnimUtils.scaleAnim(it)
            ScriptManager.createScriptDialog(context) { scriptPath ->
                dismissIfDialog()
                DialogScriptEdit.create(null)?.setScriptPath(scriptPath)
                    ?.setTitleName(File(scriptPath).name)
                    ?.setFromSource(ScriptConst.From.FROM_SCRIPT_LIST)?.show()
            }
        }
        getFilterButtonView()?.setOnClickListener {
            showTaskTypeDialog()
        }
        getImportButtonView()?.visibleOrGone(ScriptConst.supportImport)

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun showTaskTypeDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            val list = withContext(Dispatchers.IO) {
                ScriptHelper.listAllScripts()
            }
            val tagList =
                list?.map { it.scriptMate?.tag }?.mapNotNull { it }?.distinct()?.toMutableList()
            tagList?.add(0, GlobalApp.getString(com.hive.i8n.R.string.sc_filter_all))
            DialogCommonSelector(context).setTitle(com.hive.i8n.R.string.sc_filter_title)
                .setDataSet(
                    tagList?.mapIndexed { index, s -> Pair(index, s) }?.toMutableList()
                        ?: mutableListOf()
                ).setSelectListener(object : DialogCommonSelector.OnSelectListener {
                    override fun onSelected(
                        dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>
                    ) {
                        if (pos == 0) {
                            ScriptConst.Filter_Script_Tag = null
                        } else {
                            ScriptConst.Filter_Script_Tag = pair.second
                        }
                        EventBus.getDefault().post(RefreshScriptListEvent())
                        dialog.dismiss()
                    }

                    override fun onCancel() {
                    }
                }).show().show()
        }
    }


    private var lastRunningScriptPath: String? = null

    override fun onInterpreterStart(cmd: ScriptCommand) {
        post {
            notifyRunningStateChanged()
        }
    }

    override fun onInterpreterEnd(cmd: ScriptCommand) {
        post {
            notifyRunningStateChanged()
        }
    }

    override fun onInterpreterTryStop(cmd: ScriptCommand) {
        post {
            notifyRunningStateChanged()
        }
    }

    private fun notifyRunningStateChanged() {
        val currentPath = ScriptManager.getRunningScript()?.scriptPath
        val recyclerView = getListRecyclerView()
        val dataList = recyclerView.getDataSets() ?: return
        val positionsToNotify = mutableSetOf<Int>()
        dataList.forEachIndexed { index, pair ->
            val itemData = pair.second as? ScriptItemView.ItemData ?: return@forEachIndexed
            val itemPath = itemData.data?.scriptPath ?: return@forEachIndexed
            val itemPathNorm = itemPath.trimEnd('/')
            if (lastRunningScriptPath?.trimEnd('/') == itemPathNorm ||
                currentPath?.trimEnd('/') == itemPathNorm
            ) {
                positionsToNotify.add(index)
            }
        }
        lastRunningScriptPath = currentPath
        positionsToNotify.forEach { recyclerView.notifyItemChanged(it) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateScriptList() {
        GlobalScope.launch(Dispatchers.Main) {
            getFilterButtonView()?.text =
                ScriptConst.Filter_Script_Tag
                    ?: GlobalApp.getString(com.hive.i8n.R.string.sc_filter_all)
            var list = withContext(Dispatchers.IO) {
                ScriptHelper.listAllScripts()
                    ?.filter { ScriptConst.Filter_Script_Tag == null || it.scriptMate?.tag == ScriptConst.Filter_Script_Tag }
            }
            if (list.isNullOrEmpty()) {
                list = mutableListOf()
            }
            lastRunningScriptPath = ScriptManager.getRunningScript()?.scriptPath
            getListRecyclerView().submitDataSets(
                list.mapIndexed { index, model ->
                    ScriptItemView.ItemData(
                        isEditModel = isEditModel,
                        isSelected = false,
                        position = index,
                        data = model
                    )
                }.toMutableList()
            )
            if (list.isEmpty()) {
                getLayoutStateView()?.showEmpty()
            } else {
                getLayoutStateView()?.showContent()
            }
        }
    }

    private fun notifyScriptList() {
        getListRecyclerView().getDataSets()?.forEach {
            (it.second as ScriptItemView.ItemData).isEditModel = isEditModel
        }
        getListRecyclerView().notifyDataSetChanged()
    }

    override fun onItemEvent(itemData: Any?, eventData: Any?) {
        if (eventData is ScriptItemView.Opt) {
            when (eventData) {
                ScriptItemView.Opt.EVENT_SWITCH_MODE -> {
                    switchEditMode()
                }

                ScriptItemView.Opt.EVENT_SELECTED -> {
                    notifyScriptList()
                }
            }
        } else {
            if (eventData is ScriptItemView.Event) {
                handleItemMenuEvent(itemData as ScriptItemView.ItemData, eventData)
            }
        }
    }

    open fun switchEditMode() {
        isEditModel = !isEditModel
        getLayoutSelectView()?.visibleOrGone(isEditModel)
        getLayoutTitleView()?.visibleOrGone(!isEditModel)
        notifyScriptList()
    }

    fun dismissIfDialog() {
        dialogOptionHandler?.onDialogDismiss()
    }

    abstract fun getFilterButtonView(): TextView?

    abstract fun getCancelButtonView(): View?

    abstract fun getDeleteButtonView(): View?

    abstract fun getSelectButtonView(): TextView?

    abstract fun getEditButtonView(): View?

    abstract fun getImportButtonView(): View?

    abstract fun getCloseButtonView(): View?

    open fun getLayoutTaskAddView(): View? = null

    abstract fun getLayoutStateView(): StatefulLayout?

    abstract fun getLayoutTitleView(): View?

    abstract fun getLayoutSelectView(): View?

    abstract fun getListRecyclerView(): ListRecyclerView

    abstract fun getListItemView(): ScriptItemView

    abstract fun getManagerLayout(): Int

    override fun getLayoutId() = getManagerLayout()

    private fun checkControlState(
        infoModel: ScriptInfoModel, ignoreCheck: Boolean, onPassed: () -> Unit
    ) {
        if (ignoreCheck) {
            return onPassed.invoke()
        }
        if (infoModel.scriptMate?.isEncrypt() == true) {
            val localKey = ScriptKeyStoreManager.findKey(infoModel.scriptPath)
            if (localKey != null) {
                val localKeyMd5 = Md5Utils.string2md5(localKey)
                if (TextUtils.equals(localKeyMd5, infoModel.scriptMate?.passwordMd5)) {
                    if (ScriptThreadManager.isExpired(infoModel.scriptMate?.expireTime)) {
                        CommonToast.getInstance().showToast(com.hive.i8n.R.string.script_expired)
                        return
                    }
                    onPassed.invoke()
                    return
                }
            }

            DialogInputMessage(context,
                GlobalApp.getString(com.hive.i8n.R.string.sc_input_password_title),
                GlobalApp.getString(com.hive.i8n.R.string.sc_input_password_hint),
                null,
                0,
                {
                    if (it.text.isEmpty()) {
                        throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_password_empty))
                    }
                },
                { dialog, text ->
                    dialog.dismiss()
                    if (TextUtils.equals(
                            Md5Utils.string2md5(text), infoModel.scriptMate?.passwordMd5
                        )
                    ) {
                        ScriptKeyStoreManager.saveKey(infoModel.scriptPath, text)
                        updateScriptList()
                        if (ScriptThreadManager.isExpired(infoModel.scriptMate?.expireTime)) {
                            CommonToast.getInstance()
                                .showToast(com.hive.i8n.R.string.script_expired)
                            return@DialogInputMessage
                        }
                        onPassed.invoke()
                    } else {
                        CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_password_error)
                    }
                }).show()

        } else {
            onPassed.invoke()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    open fun handleItemMenuEvent(
        itemData: ScriptItemView.ItemData, eventData: ScriptItemView.Event
    ) {
        val infoModel = itemData.data ?: return

        val ignoreCheck = ScriptItemView.Event.MENU_DELETE == eventData
        checkControlState(infoModel, ignoreCheck) {
            when (eventData) {
                ScriptItemView.Event.MENU_TEXT_EDIT -> {
                    if (infoModel.scriptMate?.hasControlEdit() == true) {
                        infoModel.getMainFilePath().run {
                            dismissIfDialog()
                            EditorHelper.jumpTxtEditor(
                                ScriptProvider.getViewContext(), File(infoModel.getMainFilePath())
                            )
                        }
                    } else {
                        CommonToast.getInstance()
                            .showToast(com.hive.i8n.R.string.sc_no_permission_edit)
                    }
                }

                ScriptItemView.Event.REFRESH_LIST -> {
                    updateScriptList()
                }

                ScriptItemView.Event.STOP_EXECUTE -> {
                    ScriptManager.stopPlay()
                }

                ScriptItemView.Event.EXECUTE -> {
                    if (infoModel.scriptMate?.hasControlRun() == true) {
                        dismissIfDialog()
                        if (DialogScriptEdit.isShowing()) {
                            DialogScriptEdit.getEditDialog()?.getMiniEditView()?.startWarningAnim()
                            CommonToast.getInstance()
                                .showToast(com.hive.i8n.R.string.sc_waring_edit_panel_open)
                            return@checkControlState
                        }
                        showPermissionRequestDialog {
                            ScriptProvider().executeScript(infoModel.scriptPath, false)
                        }
                    } else {
                        CommonToast.getInstance()
                            .showToast(com.hive.i8n.R.string.sc_no_permission_run)
                    }
                }

                ScriptItemView.Event.MENU_EXPORT -> {
                    if (ScriptConst.supportImport) {
                        ActivitySelectorWrapper.startFolderSelector(getString(com.hive.i8n.R.string.script_export_btn_txt),
                            object : ActivitySelectorWrapper.OnFileSelectedListener {
                                override fun onFileSelected(file: List<File>) {
                                    val loading = DialogScriptLoading(context).show()
                                    val workflowDir = File(infoModel.scriptPath!!)
                                    val outputZip = File(file[0], "${infoModel.scriptName ?: workflowDir.name}${ScriptConst.Script_File_Suffix}")
                                    ScriptShareHelper.exportWorkflowBundleZipWithDialog(
                                        workflow = infoModel,
                                        workflowDir = workflowDir,
                                        outputZip = outputZip,
                                        context = context,
                                        onScanComplete = { loading.dismiss() },
                                        onSuccess = {
                                            CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_export_success)
                                            dismissIfDialog()
                                        },
                                        onCancel = { loading.dismiss(); dismissIfDialog() },
                                        onError = {
                                            loading.dismiss()
                                            CommonToast.getInstance().showToastLong(
                                                it.message ?: context.getString(com.hive.i8n.R.string.sc_bundle_export_failed)
                                            )
                                            dismissIfDialog()
                                        }
                                    )
                                }
                            })
                        dismissIfDialog()
                    } else {
                        CommonToast.getInstance()
                            .showToast(com.hive.i8n.R.string.sc_output_not_support)
                    }

                }

                ScriptItemView.Event.MENU_SHARE -> {
                    if (infoModel.scriptMate?.hasControlShare() == true) {
                        if (infoModel.scriptMate?.isEncrypt() == false) {
                            DialogShareConfirm(ScriptProvider.getViewContext()).setOnShareConfirmListener(
                                object : DialogShareConfirm.OnShareConfirmListener {
                                    override fun onShareConfirm(
                                        dialog: DialogShareConfirm,
                                        encrypt: Boolean,
                                        pwd: String?,
                                        ctrValue: String?,
                                        expireTime: Long
                                    ) {
                                        ScriptShareHelper.startShare(
                                            context, infoModel, pwd, ctrValue, expireTime
                                        )
                                        post { dismissIfDialog() }
                                        dialog.dismiss()
                                    }
                                }).show()
                        } else {
                            ScriptShareHelper.startShare(
                                context, infoModel, null, null, -1
                            )
                            post { dismissIfDialog() }
                        }

                    } else {
                        CommonToast.getInstance()
                            .showToast(com.hive.i8n.R.string.sc_no_permission_share)
                    }
                }

                ScriptItemView.Event.MENU_COPY -> {
                    ScriptHelper.runInIO {
                        ScriptHelper.copyScript(infoModel.scriptPath!!)
                        post { updateScriptList() }
                    }
                }

                ScriptItemView.Event.MENU_DELETE -> {
                    DialogScriptAlert(context).setTitle(com.hive.i8n.R.string.sc_delete_title)
                        .setContent(com.hive.i8n.R.string.sc_delete_content)
                        .setConfirmText(com.hive.i8n.R.string.sc_delete_confirm)
                        .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                            override fun onClickEvent(
                                dialog: DialogScriptAlert, isCancel: Boolean
                            ) {
                                dialog.dismiss()
                                if (!isCancel) {
                                    deleteScriptList(listOf(infoModel)) {
                                        updateScriptList()
                                        CommonToast.show(com.hive.i8n.R.string.sc_delete_success)
                                    }
                                }
                            }
                        }).show()
                }

                ScriptItemView.Event.MENU_EDIT -> {
                    dismissIfDialog()
                    ScriptManager.stopPlay()
                    DialogScriptEdit.create(infoModel.scriptMate)
                        ?.setScriptPath(infoModel.scriptPath!!)
                        ?.setTitleName(File(infoModel.scriptPath!!).name)
                        ?.setFromSource(ScriptConst.From.FROM_SCRIPT_LIST)?.show()
                }

                ScriptItemView.Event.MENU_DETAIL -> {
                    dismissIfDialog()
                    infoModel.scriptPath?.let { ActivityWorkflowDetail.start(context, it) }
                }

                ScriptItemView.Event.MENU_TIMING -> {
                    DialogScriptScheduleTimer(context).show()
                    dismissIfDialog()
                }

                ScriptItemView.Event.MENU_SHORTCUT -> {
                    DialogUtils.tryCreateShortcut(context, infoModel)
                }

                ScriptItemView.Event.MENU_SHOW_TRACK -> {
                    val root = ScriptCommandRoot()
                    GlobalScope.launch(Dispatchers.Main) {
                        ScriptCommandRoot.loadScript(infoModel.scriptPath!!, root)
                        ScriptRecordHelper.instance.reset(root)
                        ScriptRecordManager.updateRecordView(
                            ScriptRecordViewManager.ViewState.default().ofTrue(
                                ScriptRecordViewManager.RecordViewType.PREVIEW_DRAW
                            )
                        )
                        dismissIfDialog()
                    }
                }

                ScriptItemView.Event.MENU_INFO -> {
                    DialogScriptInfo(context).loadScript(infoModel).show()
                }

                ScriptItemView.Event.MENU_PUBLISH -> {
                    val provider = ComponentManager.getInstance()
                        .getProvider(IScriptProvider::class.java) as? IScriptProvider
                    provider?.startRegisterCustomTools(infoModel.scriptPath) {
                        EventBus.getDefault().post(OnMcpToolEvent(1))
                    }
                }

                ScriptItemView.Event.MENU_CHANGE_NAME -> {
                    showEditName(infoModel, this)
                }

                ScriptItemView.Event.MENU_TAG -> {
                    showEditTag(infoModel, this)
                }

                ScriptItemView.Event.MENU_PUBLISH_TO_MCP_TOOL -> {
                    showPublishMcpTool(infoModel, this)
                }

            }
        }
    }

    private fun showPublishMcpTool(infoModel: ScriptInfoModel, v: View) {
        val provider = ComponentManager.getInstance()
            .getProvider(IScriptProvider::class.java) as? IScriptProvider
        provider?.startRegisterCustomTools(infoModel.scriptPath) {
            EventBus.getDefault().post(OnMcpToolEvent(1))
        }
    }

    private fun showEditTag(infoModel: ScriptInfoModel, v: View) {
        val menuView = ScriptMenuManager.getMenuView()
        DialogInputMessage(menuView?.getWindowContext() ?: v.context,
            title = GlobalApp.getString(com.hive.i8n.R.string.sc_input_tag_title),
            hint = GlobalApp.getString(com.hive.i8n.R.string.sc_input_tag_hint),
            txtHold = infoModel.scriptMate?.tag ?: "",
            0,
            checkInputFun = { edit_text ->
                val name = edit_text.text.toString()
                if (TextUtils.isEmpty(name)) {
                    throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_check_input_check_empty))
                }
            },
            { dialog, name ->
                infoModel.scriptMate?.tag = name
                infoModel.saveMate()
                dialog.dismiss()
                EventBus.getDefault().post(RefreshScriptListEvent())
            }).show()
    }

    private fun showEditName(infoModel: ScriptInfoModel, v: View) {
        val menuView = ScriptMenuManager.getMenuView()
        DialogInputMessage(menuView?.getWindowContext() ?: v.context,
            title = GlobalApp.getString(com.hive.i8n.R.string.str_task_name),
            hint = GlobalApp.getString(com.hive.i8n.R.string.sc_dialog_name_hint),
            txtHold = infoModel.scriptName,
            0,
            checkInputFun = { edit_text ->
                val name = edit_text.text.toString()
                if (TextUtils.isEmpty(name)) {
                    throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_check_input_check_empty))
                }
                if (name.length > 50) {
                    throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_check_input_check_empty_3))
                }
                if (name != infoModel.scriptName) {
                    if (File("${ScriptConst.Save_Script_Path}/${name}/").exists()) {
                        throw Exception(GlobalApp.getString(com.hive.i8n.R.string.sc_check_input_check_empty_4))
                    }
                }
            },
            { dialog, name ->
                File("${ScriptConst.Save_Script_Path}/${infoModel.scriptName}/").renameTo(
                    File(
                        "${ScriptConst.Save_Script_Path}/${name}/"
                    )
                )
                dialog.dismiss()
                EventBus.getDefault().post(RefreshScriptListEvent())
            }).show()
    }

    private fun showPermissionRequestDialog(callback: () -> Unit) {
        if (ScriptConst.runningDialogShow) {
            DialogScriptAlert(context).setTitle(com.hive.i8n.R.string.sc_running_request_permission_title)
                .setContent(com.hive.i8n.R.string.sc_running_request_permission_content)
                .setConfirmText(com.hive.i8n.R.string.sc_running_request_permission_confirm)
                .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                    override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                        dialog.dismiss()
                        if (!isCancel) {
                            callback.invoke()
                        }
                    }
                }).show()
        } else {
            callback.invoke()
        }

    }

    /**
     * 导入脚本（.zip 资源包）
     */
    protected fun importScript() {
        BundleImportHelper.startImportFromFilePicker(context)
    }

    /**
     * 选择逻辑
     */
    private fun selectAllItems() {
        getSelectButtonView()?.isSelected = getSelectButtonView()?.isSelected == false
        getListRecyclerView().getDataSets()?.forEach {
            val itemData = (it.second as ScriptItemView.ItemData)
            itemData.isSelected = getSelectButtonView()?.isSelected == true
        }
        getSelectButtonView()?.text =
            getString(if (getSelectButtonView()?.isSelected == true) com.hive.i8n.R.string.btn_file_open_unselect else com.hive.i8n.R.string.btn_file_open_select)
        notifyScriptList()
    }

    /**
     * 删除逻辑
     */
    private fun deleteItems() {
        val targetList =
            getListRecyclerView().getDataSets()?.map { it.second as ScriptItemView.ItemData }
                ?.filter { it.isSelected }?.mapNotNull { it.data }

        if (targetList?.isNotEmpty() == true) {
            DialogScriptAlert(context).setTitle(com.hive.i8n.R.string.sc_delete_selected_title)
                .setContent(com.hive.i8n.R.string.sc_delete_content)
                .setConfirmText(com.hive.i8n.R.string.sc_delete_confirm)
                .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                    override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                        dialog.dismiss()
                        if (!isCancel) {
                            deleteScriptList(targetList) {
                                updateScriptList()
                                CommonToast.show(com.hive.i8n.R.string.sc_delete_success)
                            }
                        }
                    }
                }).show()
        } else {
            CommonToast.show(com.hive.i8n.R.string.sc_delete_list_empty_error)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun deleteScriptList(list: List<ScriptInfoModel>, onFinished: (() -> Unit)) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                list.forEach {
                    it.delete()
                }
            }
            withContext(Dispatchers.Main) {
                onFinished.invoke()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshScriptListEvent(event: RefreshScriptListEvent) {
        updateScriptList()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshScriptListInitEvent(event: RefreshScriptListInitEvent) {
        ScriptConst.Filter_Script_Tag = null
        updateScriptList()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        EventBus.getDefault().register(this)
        updateScriptList()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EventBus.getDefault().unregister(this)

    }

//    private fun showShortcutPermission() {
//        DialogAlert(GlobalApp.getContext())
//            .setTitle(com.hive.i8n.R.string.sc_dialog_lock_short_title)
//            .setContent(com.hive.i8n.R.string.sc_dialog_lock_short_content)
//            .setConfirmText(com.hive.i8n.R.string.sc_dialog_lock_short_confirm)
//            .setOnDialogEventListener(object : DialogAlert.OnDialogEventListener {
//                override fun onClickEvent(dialog: DialogAlert, isCancel: Boolean) {
//                    dialog.dismiss()
//                    if (!isCancel) {
//                        dismissIfDialog()
//                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                        intent.data =
//                            Uri.parse("package:" + GlobalApp.getAvailableActivity().packageName)
//                        GlobalApp.getAvailableActivity().startActivityForResult(intent, 1000)
//                    }
//                }
//            }).show()
//        CommonToast.show(getString(com.hive.i8n.R.string.sc_permission_lock_install_shortcut))
//    }


}

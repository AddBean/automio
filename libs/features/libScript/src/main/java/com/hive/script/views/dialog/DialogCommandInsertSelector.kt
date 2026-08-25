// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.hive.anim.AnimUtils
import com.hive.extension.visibleOrGone
import com.hive.files.utils.XAppInfoParser
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptClickActionHelper
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptCommandRoot
import com.hive.script.base.ScriptConst
import com.hive.script.base.core.ScriptInterpreter
import com.hive.script.base.params.ScriptParamEnv
import com.hive.script.cmd.CmdActionBack
import com.hive.script.cmd.CmdActionHome
import com.hive.script.cmd.CmdActionOpenNotifications
import com.hive.script.cmd.CmdActionRecent
import com.hive.script.cmd.CmdActionScreenLock
import com.hive.script.cmd.CmdActionScreenShot
import com.hive.script.cmd.CmdActionUnlock
import com.hive.script.cmd.CmdAiRequest
import com.hive.script.cmd.CmdBreak
import com.hive.script.cmd.CmdClickText
import com.hive.script.cmd.CmdClickText.Companion.TEXT_FIND_CONTAINS
import com.hive.script.cmd.CmdCopyToClipboard
import com.hive.script.cmd.CmdCurl
import com.hive.script.cmd.CmdDelay
import com.hive.script.cmd.CmdDownload
import com.hive.script.cmd.CmdExit
import com.hive.script.cmd.CmdFor
import com.hive.script.cmd.CmdJump
import com.hive.script.cmd.CmdJumpPoint
import com.hive.script.cmd.CmdCallScript
import com.hive.script.cmd.CmdOpenApp
import com.hive.script.cmd.CmdRunSkill
import com.hive.script.cmd.CmdOpenUrl
import com.hive.script.cmd.CmdPlayAudio
import com.hive.script.cmd.CmdPythonExecutor
import com.hive.script.cmd.CmdReadScreenText
import com.hive.script.cmd.CmdVoiceInteract
import com.hive.script.cmd.CmdAlignToSecond
import com.hive.script.cmd.CmdToast
import com.hive.script.cmd.CmdCategory
import com.hive.script.cmd.CmdInsertType
import com.hive.script.cmd.CommandCategoryRegistry
import com.hive.script.condition.ConditionIDS
import com.hive.script.extensions.submitDataSetsWithType
import com.hive.script.extensions.updateChildParent
import com.hive.script.extensions.updateParent
import com.hive.script.utils.ScriptHelper
import com.hive.plugin.agent.model.SkillSpec
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogCommandInsertSelector.CardType.CARD_ITEM
import com.hive.script.views.dialog.DialogCommandInsertSelector.CardType.CARD_TITLE
import com.hive.script.views.edit.DialogScriptCardEdit
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.edit.ScriptEditFactory
import com.hive.script.views.edit.ScriptMenuEditHelper
import com.hive.script.views.manager.ScriptInsertManager
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptMenuManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.script.views.menu.ScriptControlView
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.CommomListener
import com.hive.utils.GlobalApp
import com.hive.views.list_view.ListRecyclerItemView
import com.hive.views.list_view.IListRecyclerViewFactory
import com.hive.views.list_view.ListRecyclerView
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.TextDrawableView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 *
 * @author jiadou
 * @date 6/19/21
 */
@SuppressLint("ViewConstructor")
class DialogCommandInsertSelector(
    context: Context, var rootCmd: ScriptCommand, var index: Int, var dialogView: DialogScriptEdit?
) : BaseScriptDialog(context), IListRecyclerViewFactory {

    object CardType {
        const val CARD_TITLE = 0
        const val CARD_ITEM = 1
    }

    private val itemLists: List<Pair<Int, ItemData>> by lazy {
        buildItemListFromRegistry()
    }

    private fun buildItemListFromRegistry(): List<Pair<Int, ItemData>> {
        val result = mutableListOf<Pair<Int, ItemData>>()
        CommandCategoryRegistry.orderedCategoryEntries.forEach { (categoryId, items) ->
            result += CARD_TITLE to ItemData(
                CmdInsertType.TYPE_TITLE,
                GlobalApp.getString(CmdCategory.getTitleResId(categoryId)),
                -1,
                null
            )
            items.forEach { meta ->
                val isEnable = when (meta.addType) {
                    CmdInsertType.TYPE_INSERT_UNLOCK -> ScriptManager.isUnlockScriptExist()
                    else -> true
                }
                result += CARD_ITEM to ItemData(
                    meta.addType,
                    GlobalApp.getString(meta.stringResId),
                    meta.drawableResId,
                    meta.cmdId,
                    isEnable
                )
            }
        }
        return result
    }

    override fun initWindow() {
        val tv_btn_cancel= findViewById<View>(R.id.tv_btn_cancel)
        val listRecyclerView = findViewById<ListRecyclerView>(R.id.listRecyclerView)
        tv_btn_cancel?.setOnClickListener {
            dismiss()
        }
        listRecyclerView?.setItemViewFactory(this)
        post {
            val filteredList = itemLists
            listRecyclerView?.layoutManager = GridLayoutManager(context, 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int =
                        if (filteredList.getOrNull(position)?.first == CARD_TITLE) 2 else 1
                }
            }
            listRecyclerView?.submitDataSetsWithType(filteredList)
            listRecyclerView?.notifyDataSetChanged()
        }

    }


    override fun enableFadeAnimation() = true

    override fun createItemView(viewType: Int): ListRecyclerItemView {
        return when (viewType) {
            CARD_TITLE -> ItemTitleView()
            else -> ItemView()
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    private fun onItemClickEvent(item: ItemData, itemView: View) {
        AnimUtils.scaleAnim(itemView)
        when (item.type) {
            CmdInsertType.TYPE_INSERT_RECORD -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptRecordManager.startRecord(object : ScriptRecordManager.OnRecordEventListener {
                    override fun onRecordFinished(script: ScriptCommandRoot): Boolean {
                        ScriptInterpreter.getDefault().stopExecute()
                        ScriptRecordManager.hiddenRecordView()
                        ScriptMenuManager.hiddenMenuView()
                        ScriptMenuEditHelper.confirmInsertType(
                            context, rootCmd, index, script.commandQueue, dialogView
                        )
                        return true
                    }
                })
                ScriptManager.pauseOrResumePlay(false)
            }

            CmdInsertType.TYPE_INSERT_SCRIPT -> {
                DialogScriptListSelector(context, true).setOnScriptSelectListener(object :
                    DialogScriptListSelector.OnScriptSelectListener {
                    override fun onSelected(
                        dialog: DialogScriptListSelector, model: ScriptInfoModel
                    ) {
                        GlobalScope.launch(Dispatchers.Main) {
                            dialog.dismiss()
                            val script = ScriptCommandRoot()
                            ScriptCommandRoot.loadScript(model.scriptPath!!, script)
                            ScriptMenuEditHelper.inertGroupCommand(
                                rootCmd,
                                rootCmd.commandQueue,
                                index,
                                script.commandQueue,
                                model.scriptName
                            )
                            rootCmd.updateChildParent()
                            dialogView?.updateStatus()
                            dialogView?.updateData()
                            ScriptMenuManager.switchMenuMode(ScriptControlView.MenuMode.MAIN_MENU)
                            dialogView?.show()
                        }
                    }

                    override fun onDismissed() {
                        dialogView?.show()
                    }
                }).show()
            }

            CmdInsertType.TYPE_INSERT_EXIT -> {
                val cmd = CmdExit.createCommand()
                insertCommandIfNotAdded(cmd)
            }

            CmdInsertType.TYPE_INSERT_CLICK_OR_SCROLL -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertClickOrScroll(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }
                })
            }

            CmdInsertType.TYPE_INSERT_FAST_CLICK -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertFastClick(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }
                })
            }

            CmdInsertType.TYPE_INSERT_CLICK_IMAGE -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertClickImage(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }

                })
            }

            CmdInsertType.TYPE_INSERT_SCALE_IN_OUT -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertScaleInOut(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }

                })
            }

            CmdInsertType.TYPE_INSERT_SCROLL_MULTIPLE -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertScrollMultiple(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }

                })
            }

            CmdInsertType.TYPE_INSERT_CLICK_VIEW -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertClickView(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }

                })
            }

            CmdInsertType.TYPE_INSERT_CLICK_COLOR -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertClickColor(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }

                })
            }

            CmdInsertType.TYPE_INSERT_CLICK_TEXT -> {
                if (ScriptManager.checkAccessibility()) return
                val cmdInsert = CmdClickText.createCommand(
                    ScriptClickActionHelper.ACTION_CLICK,
                    5,
                    ScriptConst.Cmd_Fast_Click_Gap_Default,
                    ScriptConst.Cmd_Long_Click_Default,
                    "",
                    TEXT_FIND_CONTAINS
                )
                val editView = ScriptEditFactory.createItemEditView(
                    ScriptProvider.getViewContext(), cmdInsert, false
                )
                DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                    cmdInsert.getCommandName() ?: ""
                ).setEdtView(editView).setOnInflateFinished {
                    editView.bindCommand(cmdInsert)
                }.setOnConfirmClicked { dialog ->
                    try {
                        editView.checkCommandOrThrowError()
                        insertCommandIfNotAdded(cmdInsert)
                        dialog.dismiss()
                    } catch (e: Exception) {
                        CommonToast.show(e.message)
                    }
                }.show()
            }

            CmdInsertType.TYPE_INSERT_READ_SCREEN_TEXT -> {
                if (ScriptManager.checkAccessibility()) return
                val cmdInsert = CmdReadScreenText.createCommand(
                    ScriptParamEnv.getDefaultParam()?.getFullId()
                )
                val editView = ScriptEditFactory.createItemEditView(
                    ScriptProvider.getViewContext(), cmdInsert, false
                )
                DialogScriptCardEdit(ScriptProvider.getViewContext()).setTitle(
                    cmdInsert.getCommandName() ?: ""
                ).setEdtView(editView).setOnInflateFinished {
                    editView.bindCommand(cmdInsert)
                }.setOnConfirmClicked { dialog ->
                    try {
                        editView.checkCommandOrThrowError()
                        insertCommandIfNotAdded(cmdInsert)
                        dialog.dismiss()
                    } catch (e: Exception) {
                        CommonToast.show(e.message)
                    }
                }.show()
            }

            CmdInsertType.TYPE_INSERT_INPUT -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertInput(object : ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }

                })
            }


            CmdInsertType.TYPE_INSERT_BATCH_CLICK -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertBatchClick(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }

                })
            }

            CmdInsertType.TYPE_INSERT_DELAY -> {
                val cmd = CmdDelay.createCommand(ScriptConst.Cmd_Delay_Default)
                val editView = ScriptEditFactory.createItemEditView(context, cmd, true)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }


            CmdInsertType.TYPE_INSERT_BREAK -> {
                val cmd = CmdBreak.createCommand()
                insertCommandIfNotAdded(cmd)
            }

            CmdInsertType.TYPE_INSERT_JUMP -> {
                val points = ScriptHelper.getJumpPoints(rootCmd)
                if (points.isEmpty()) {
                    CommonToast.show(com.hive.i8n.R.string.sc_jump_point_empty)
                    return
                }
                DialogCommonSelector(context).setTitle(GlobalApp.getString(com.hive.i8n.R.string.sc_jump_menu_title))
                    .setDataSet(
                        points.map {
                            it.id to it.getCommandName()
                        }.toMutableList()
                    ).setSelectListener(object : DialogCommonSelector.OnSelectListener {
                        override fun onSelected(
                            dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>
                        ) {
                            val cmd = CmdJump.createCommand(points[pos].id)
                            insertCommandIfNotAdded(cmd)
                            dialog.dismiss()
                        }

                        override fun onCancel() {
                        }
                    }).show()
            }

            CmdInsertType.TYPE_INSERT_JUMP_POINT -> {
                val cmd = CmdJumpPoint.createCommand(rootCmd)
                insertCommandIfNotAdded(cmd)
            }

            CmdInsertType.TYPE_INSERT_LOAD_SCRIPT -> {
                DialogScriptListSelector(context, true).setOnScriptSelectListener(object :
                    DialogScriptListSelector.OnScriptSelectListener {
                    override fun onSelected(
                        dialog: DialogScriptListSelector, model: ScriptInfoModel
                    ) {
                        GlobalScope.launch(Dispatchers.Main) {
                            dialog.dismiss()
                            val cmd = CmdCallScript.createCommand(
                                model.scriptPath!!, model.scriptName!!
                            )
                            insertCommandIfNotAdded(cmd)
                        }
                    }

                    override fun onDismissed() {
                        dialogView?.show()
                    }
                }).show()
            }

            CmdInsertType.TYPE_INSERT_RUN_SKILL -> {
                DialogSkillSelector(context).setOnSkillSelectListener(object :
                    DialogSkillSelector.OnSkillSelectListener {
                    override fun onSelected(
                        dialog: DialogSkillSelector,
                        spec: SkillSpec
                    ) {
                        dialog.dismiss()
                        val cmd = CmdRunSkill.createCommand(
                            spec.id,
                            spec.name,
                            null
                        )
                        val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                        DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                            .setEdtView(editView).setOnInflateFinished {
                                editView.bindCommand(cmd)
                            }.setOnConfirmClicked { d ->
                                try {
                                    editView.checkCommandOrThrowError()
                                    insertCommandIfNotAdded(cmd)
                                    d.dismiss()
                                } catch (e: Exception) {
                                    CommonToast.show(e.message)
                                }
                            }.show()
                    }

                    override fun onDismissed() {
                        dialogView?.show()
                    }
                }).show()
            }

            CmdInsertType.TYPE_INSERT_VOICE_TTS -> {
                val cmd = CmdVoiceInteract().apply {
                    mode = CmdVoiceInteract.MODE_TTS
                    ttsText = ""
                }
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { d ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            d.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }

            CmdInsertType.TYPE_INSERT_VOICE_ASR -> {
                val cmd = CmdVoiceInteract().apply {
                    mode = CmdVoiceInteract.MODE_ASR
                    targetParamId = ScriptParamEnv.getDefaultParam()?.getFullId()
                }
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { d ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            d.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }

            CmdInsertType.TYPE_INSERT_PLAY_AUDIO -> {
                val cmd = CmdPlayAudio.createCommand()
                insertCommandIfNotAdded(cmd)
            }

            CmdInsertType.TYPE_INSERT_TOAST -> {
                val cmd = CmdToast.createCommand()
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }

            CmdInsertType.TYPE_INSERT_TIMER_CALIBRATOR -> {
                val cmd = CmdAlignToSecond.createCommand(60)
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }

            CmdInsertType.TYPE_INSERT_OPEN_APP -> {
                DialogAppSelector(context).setOnAppSelectedListener(object :
                    DialogAppSelector.OnAppSelectedListener {
                    override fun onSelected(
                        dialog: DialogAppSelector, appInfo: XAppInfoParser.AppInfo?
                    ) {
                        val launchIntent =
                            GlobalApp.getApp().packageManager.getLaunchIntentForPackage(
                                appInfo?.packageName!!
                            )
                        val cmd = CmdOpenApp.createCommand(
                            appInfo.packageName,
                            launchIntent?.component?.className,
                            appInfo.appName,
                            "reopen"
                        )
                        insertCommandIfNotAdded(cmd)
                        dialog.dismiss()
                    }
                }).setOnDismissListener(object : OnDismissListener {
                    override fun onDismiss() {
                        dialogView?.show()
                    }
                }).show()
            }

            CmdInsertType.TYPE_INSERT_OPEN_LINK -> {
                DialogOpenScheme(context).apply {
                    mCallback = CommomListener.Callback { _, scheme ->
                        val cmd = CmdOpenUrl.createCommand(scheme as String)
                        insertCommandIfNotAdded(cmd)
                        this.dismiss()
                    }
                }.setOnDismissListener(object : OnDismissListener {
                    override fun onDismiss() {
                        dialogView?.show()
                    }
                }).show()
            }

            CmdInsertType.TYPE_INSERT_COPY -> {
                DialogCopyInput(context).apply {
                    mCallback = CommomListener.Callback { _, content ->
                        val cmd = CmdCopyToClipboard.createCommand(content as String)
                        insertCommandIfNotAdded(cmd)
                        this.dismiss()
                    }
                }.setOnDismissListener(object : OnDismissListener {
                    override fun onDismiss() {
                        dialogView?.show()
                    }
                }).show()
            }

            CmdInsertType.TYPE_INSERT_UNLOCK -> {
                if (ScriptManager.isUnlockScriptExist()) {
                    val cmd = CmdActionUnlock.createCommand()
                    this.dismiss()
                    insertCommandIfNotAdded(cmd)
                } else {
                    CommonToast.getInstance().showToast(com.hive.i8n.R.string.sc_cmd_unlock_add_not_setting)
                }
            }

            CmdInsertType.TYPE_INSERT_LOCK -> {
                val cmd = CmdActionScreenLock.createCommand()
                insertCommandIfNotAdded(cmd)
                this.dismiss()
            }

            CmdInsertType.TYPE_INSERT_ACTION_NOTIFICATION -> {
                val cmd = CmdActionOpenNotifications.createCommand()
                insertCommandIfNotAdded(cmd)
                this.dismiss()
            }

            CmdInsertType.TYPE_INSERT_LOOP -> {
                DialogCycleSetConfirm(context).apply {
                    confirmFun = { _, loopCount ->
                        val cmd = CmdFor.createCommand(loopCount, mutableListOf())
                        insertCommandIfNotAdded(cmd)
                        if (cmd.loopCount == 0 || cmd.loopCount == -1) {
                            CommonToast.show(com.hive.i8n.R.string.sc_edit_untouch_wraning_info)
                        }
                    }
                }.setOnDismissListener(object : OnDismissListener {
                    override fun onDismiss() {
                        dialogView?.show()
                    }
                }).show()
            }

            CmdInsertType.TYPE_INSERT_IF -> {

                DialogCommonSelector(context).setTitle(GlobalApp.getString(com.hive.i8n.R.string.sc_condition_edit_text_menu_title))
                    .setDataSet(
                        mutableListOf(
                            ConditionIDS.ConditionIdNotification to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_notification_edit_text_menu),
                            ConditionIDS.ConditionIdView to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_notification_edit_text_view),
                            ConditionIDS.ConditionIdImage to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_notification_edit_text_image),
                            ConditionIDS.ConditionIdColor to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_notification_edit_text_color),
                            ConditionIDS.ConditionIdParam to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_notification_edit_text_param),
                            ConditionIDS.ConditionIdPermission to GlobalApp.getString(com.hive.i8n.R.string.sc_condition_notification_edit_text_permission)
                        )
                    ).setSelectListener(object : DialogCommonSelector.OnSelectListener {
                        override fun onSelected(
                            dialog: DialogCommonSelector, pos: Int, pair: Pair<Int, String>
                        ) {
                            showIfEditDialog(pair.first)
                            dialog.dismiss()
                        }

                        override fun onCancel() {
                        }

                    }).show()
            }

            CmdInsertType.TYPE_INSERT_ACTION_SNAPSHOT -> {
                val cmd = CmdActionScreenShot.createCommand()
                insertCommandIfNotAdded(cmd)
            }

            CmdInsertType.TYPE_INSERT_ACTION_BACK -> {
                val cmd = CmdActionBack.createCommand()
                insertCommandIfNotAdded(cmd)
            }

            CmdInsertType.TYPE_INSERT_ACTION_HOME -> {
                val cmd = CmdActionHome.createCommand()
                insertCommandIfNotAdded(cmd)
            }

            CmdInsertType.TYPE_INSERT_ACTION_RECENT -> {
                val cmd = CmdActionRecent.createCommand()
                insertCommandIfNotAdded(cmd)
            }

            CmdInsertType.TYPE_INSERT_CURL -> {
                val cmd = CmdCurl.createCommand()
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }

            CmdInsertType.TYPE_INSERT_DOWNLOAD -> {
                val cmd = CmdDownload.createCommand()
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }

            CmdInsertType.TYPE_INSERT_PYTHON_EXECUTOR -> {
                val cmd = CmdPythonExecutor.createCommand()
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }

            CmdInsertType.TYPE_INSERT_AI_REQUEST -> {
                val cmd = CmdAiRequest.createCommand("")
                val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
                DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "")
                    .setEdtView(editView).setOnInflateFinished {
                        editView.bindCommand(cmd)
                    }.setOnConfirmClicked { dialog ->
                        try {
                            editView.checkCommandOrThrowError()
                            insertCommandIfNotAdded(cmd)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            CommonToast.show(e.message)
                        }
                    }.show()
            }


            CmdInsertType.TYPE_INSERT_READ_VIEW_TEXT -> {
                if (ScriptManager.checkAccessibility()) return
                dialogView?.hidden()
                ScriptInsertManager.startInsertReadViewText(object :
                    ScriptInsertManager.OnInsertListener {
                    override fun onInsertCommand(cmdInsert: ScriptCommand?) {
                        dialogView?.show()
                        cmdInsert ?: return
                        insertCommandIfNotAdded(cmdInsert)
                    }

                    override fun onInsertDismiss() {
                        dialogView?.show()
                    }

                })
            }

            CmdInsertType.TYPE_INSERT_SET_PARAM -> {
                ScriptInsertManager.startInsertSetCmd(context) {
                    showSetEditDialog(it)
                }
            }
        }
        post {
            dismiss()
        }
    }

    private fun showIfEditDialog(type: Int) {
        DialogCommendAddIf(context, type).apply {
            mCallback = { cmd ->
                insertCommandIfNotAdded(cmd)
                this.dismiss()
            }
        }.setOnDismissListener(object : OnDismissListener {
            override fun onDismiss() {
                dialogView?.show()
            }
        }).show()
    }

    private fun showSetEditDialog(cmd: ScriptCommand) {
        val editView = ScriptEditFactory.createItemEditView(context, cmd, false)
        DialogScriptCardEdit(context).setTitle(cmd.getCommandName() ?: "").setEdtView(editView)
            .setOnInflateFinished {
                editView.bindCommand(cmd)
            }.setOnConfirmClicked { dialog ->
                try {
                    editView.checkCommandOrThrowError()
                    insertCommandIfNotAdded(cmd)
                    dialog.dismiss()
                } catch (e: Exception) {
                    CommonToast.show(e.message)
                }
            }.show()
    }

    private fun insertCommandIfNotAdded(cmdInsert: ScriptCommand) {
        if (rootCmd.commandQueue.contains(cmdInsert).not()) {
            ScriptMenuEditHelper.insertCommand(
                rootCmd, rootCmd.commandQueue, index, arrayListOf(cmdInsert).toMutableList()
            )
        }
        rootCmd.updateParent()
        dialogView?.updateData()
    }

    inner class ItemTitleView : ListRecyclerItemView(context) {

        private var itemView =
            LayoutInflater.from(context).inflate(R.layout.dialog_common_selector_item_title, this)

        override fun bindData(data: Any?) {
            val itemData = data as ItemData
            itemView.findViewById<TextView>(R.id.btn_tv).text = itemData.txt
        }
    }

    inner class ItemView : ListRecyclerItemView(context) {
        private var item: ItemData? = null

        private var itemView =
            LayoutInflater.from(context).inflate(R.layout.dialog_cdm_insert_selector_item, this)

        override fun bindData(data: Any?) {
            this.item = data as ItemData
            val tv = itemView.findViewById<TextDrawableView>(R.id.btn_tv)

            tv.text = item?.txt
            item?.resId?.takeIf { it != -1 }?.let {
                tv.setDrawableLeft(GlobalApp.getDrawable(it))
            }
            tv.alpha = if (item?.isEnable == false) 0.4f else 1f
            itemView.setOnClickListener {
                onItemClickEvent(item!!, this)
            }
        }
    }


    data class ItemData(
        val type: Int,
        val txt: String,
        val resId: Int,
        val cmdType: Int?,
        var isEnable: Boolean = true
    )

    override fun getWindowLayoutId() = R.layout.dialog_script_cmd_insert_selector
}

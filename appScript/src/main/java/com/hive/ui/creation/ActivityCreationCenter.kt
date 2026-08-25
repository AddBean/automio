// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.creation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.hive.app.script.R
import com.hive.base.BaseFragmentActivity
import com.hive.plugin.ComponentManager
import com.hive.plugin.provider.IScriptProvider
import com.hive.script.base.ScriptCommand
import com.hive.script.base.ScriptConst
import com.hive.script.driver.ScriptEventHelper
import com.hive.script.event.RefreshScriptListEvent
import com.hive.script.utils.ScriptHelper.PERMISSION_BIND_ACCESSIBILITY_SERVICE
import com.hive.script.utils.ScriptPermissionManager
import com.hive.script.utils.bundle.BundleImportHelper
import com.hive.script.views.dialog.DialogChooseScriptStart
import com.hive.script.views.dialog.DialogPermissionAggregate
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.views.manager.ScriptManager
import com.hive.script.views.manager.ScriptRecordManager
import com.hive.ui.skill.DialogSkillCreateMode
import com.hive.ui.skill.DialogSkillCreate
import com.hive.ui.skill.ActivitySkillAiCreate
import com.hive.ui.skill.getSkillDraft
import com.hive.ui.workflow.DialogWorkflowCreateMode
import com.hive.utils.CommomListener
import com.hive.utils.ResultActivityAdaptor
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

class ActivityCreationCenter : BaseFragmentActivity(), View.OnClickListener {

    private val scriptProvider: IScriptProvider? by lazy {
        ComponentManager.getInstance().getProvider(IScriptProvider::class.java) as? IScriptProvider
    }

    override fun doOnCreate(savedState: Bundle?) {
        EventBus.getDefault().register(this)
        findViewById<View>(R.id.card_create_workflow)?.setOnClickListener(this)
        findViewById<View>(R.id.card_create_skill)?.setOnClickListener(this)
        findViewById<View>(R.id.card_create_tool)?.setOnClickListener(this)
        findViewById<View>(R.id.card_import_bundle)?.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun getLayoutId(): Int = R.layout.activity_creation_center

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.card_create_workflow -> {
                DialogWorkflowCreateMode.show(
                    fragmentManager = supportFragmentManager,
                    onRecordCreate = {
                        // 检查无障碍服务权限
                        val missedPermissions = ScriptPermissionManager.checkMissedPermissions(
                            listOf(PERMISSION_BIND_ACCESSIBILITY_SERVICE)
                        )

                        if (missedPermissions.isNotEmpty()) {
                            // 无障碍服务未启用，显示权限聚合弹窗
                            DialogPermissionAggregate(
                                context = this,
                                missed = missedPermissions
                            ).show()
                        } else {
                            // 无障碍服务已启用，显示录制启动方式选择对话框
                            DialogChooseScriptStart(this).apply {
                                mCallback = CommomListener.Callback { _, cmd ->
                                    ScriptRecordManager.startRecord()
                                    ScriptManager.pauseOrResumePlay(false)
                                    if (cmd != null) {
                                        ScriptManager.addAndExecuteCommand(cmd as ScriptCommand)
                                    }
                                }
                            }.show()
                        }
                    },
                    onManualCreate = {
                        // 手动创作：执行原有逻辑
                        ScriptManager.createScriptDialog(this) { scriptPath ->
                            DialogScriptEdit.create(null)
                                ?.setScriptPath(scriptPath)
                                ?.setTitleName(File(scriptPath).name)
                                ?.setFromSource(ScriptConst.From.FROM_SCRIPT_LIST)
                                ?.show()
                            finish()
                        }
                    }
                )
            }

            R.id.card_create_skill -> {
                DialogSkillCreateMode.show(
                    fragmentManager = supportFragmentManager,
                    onAiCreate = {
                        startActivityWithCallback(
                            ActivitySkillAiCreate.createIntent(this@ActivityCreationCenter),
                            object : ResultActivityAdaptor.ResultActivityListener {
                                override fun onResult(requestCode: Int, resultCode: Int, data: Intent?) {
                                    if (resultCode != Activity.RESULT_OK) return
                                    val draft = data?.getSkillDraft() ?: return
                                    DialogSkillCreate.show(
                                        fragmentManager = supportFragmentManager,
                                        initial = null,
                                        onSaved = { finish() },
                                        prefill = draft
                                    )
                                }
                            }
                        )
                    },
                    onManualCreate = {
                        DialogSkillCreate.show(
                            supportFragmentManager,
                            initial = null,
                            onSaved = { finish() }
                        )
                    }
                )
            }

            R.id.card_create_tool -> {
                val provider = scriptProvider
                if (provider == null) {
                    CommonToast.getInstance().showToast(com.hive.i8n.R.string.mcp_tool_list_empty)
                    return
                }
                provider.startRegisterCustomTools(null)
            }

            R.id.card_import_bundle -> {
                BundleImportHelper.startImportFromFilePicker(this)
            }
        }
    }

    /**
     * 监听工具创建成功事件，自动关闭创作中心页面
     */
    @Subscribe
    fun onRefreshScriptListEvent(event: RefreshScriptListEvent) {
        // 当收到刷新事件时，说明工具创建成功，关闭创作中心
        finish()
    }

    companion object {
        fun start(context: Context) {
            IntentUtils.safeStartActivity(context, Intent(context, ActivityCreationCenter::class.java))
        }
    }
}

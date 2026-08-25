// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hive.base.BaseFragmentActivity
import com.hive.markdown.MarkdownTextView
import com.hive.net.image.ImageLoader
import com.hive.plugin.ComponentManager
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.provider.IDependencyNavigationProvider
import com.hive.plugin.provider.IScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.base.ScriptMate
import com.hive.script.event.RefreshScriptListEvent
import com.hive.script.scope.ScriptScopeRepository
import com.hive.script.scope.ScriptScopeSnapshot
import com.hive.script.utils.ScriptHelper
import com.hive.script.utils.ScriptShareHelper
import com.hive.script.views.beans.ScriptInfoModel
import com.hive.script.views.dialog.DialogScriptAlert
import com.hive.script.views.dialog.DialogInputMessage
import com.hive.script.views.dialog.DialogScriptInfo
import com.hive.script.views.dialog.DialogScriptScopeManager
import com.hive.script.views.dialog.DialogShareConfirm
import com.hive.script.views.edit.DialogScriptEdit
import com.hive.script.scope.SkillFileHelper
import com.hive.script.views.manager.ScriptManager
import com.hive.utils.file.FileUtils
import com.hive.utils.utils.IntentUtils
import com.hive.utils.utils.StringUtils
import com.hive.views.resource.ResourceDetailActionStyle
import com.hive.views.resource.ResourceDetailBadgeVariant
import com.hive.views.resource.ResourceDetailType
import com.hive.views.resource.ResourceDetailTypeStyleResolver
import com.hive.views.resource.ResourceDetailViewFactory
import com.hive.views.resource.ResourceOverflowAction
import com.hive.views.resource.ResourceOverflowMenuHelper
import com.hive.views.widgets.CommonToast
import com.hive.views.widgets.FlowLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.Date

class ActivityWorkflowDetail : BaseFragmentActivity() {

    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)

    private var currentData: ScriptInfoModel? = null
    private var moreMenuPopup: PopupWindow? = null

    private var tvName: TextView? = null
    private var tvId: TextView? = null
    private var tvTypeBadge: TextView? = null
    private var layoutActions: View? = null
    private var ivIcon: com.hive.views.widgets.UIResourceIconView? = null
    private var btnEdit: TextView? = null
    private var btnRun: TextView? = null
    private var tvVersion: TextView? = null
    private var tvMeta: TextView? = null
    private var tvIntro: MarkdownTextView? = null
    private var tvDeviceSummary: TextView? = null
    private var tvDependencySummary: TextView? = null
    private var tvPermissionSummary: TextView? = null
    private var layoutDevices: FlowLayout? = null
    private var layoutDependencies: FlowLayout? = null
    private var layoutPermissions: FlowLayout? = null

    private var currentScopeSnapshot: ScriptScopeSnapshot? = null

    private val scriptProvider: IScriptProvider? by lazy {
        ComponentManager.getInstance().getProvider(IScriptProvider::class.java) as? IScriptProvider
    }

    private val dependencyNavigationProvider: IDependencyNavigationProvider? by lazy {
        ComponentManager.getInstance().getProvider(IDependencyNavigationProvider::class.java) as? IDependencyNavigationProvider
    }

    override fun getLayoutId(): Int = R.layout.activity_workflow_detail

    override fun doOnCreate(savedState: Bundle?) {
        currentData = loadInfoFromIntent()
        if (currentData == null) {
            finish()
            return
        }
        currentScopeSnapshot = loadScope(currentData!!)
        bindViews()
        render(currentData!!)
        bindActions()
    }

    override fun onResume() {
        super.onResume()
        reloadData()
    }

    override fun onDestroy() {
        moreMenuPopup?.dismiss()
        activityScope.cancel()
        super.onDestroy()
    }

    private fun loadInfoFromIntent(): ScriptInfoModel? {
        val scriptPath = intent.getStringExtra(EXTRA_SCRIPT_PATH)?.takeIf { it.isNotBlank() } ?: return null
        val scriptDir = File(scriptPath)
        if (!scriptDir.exists() || !scriptDir.isDirectory) return null
        val infoByInfoFile = ScriptInfoModel().parseInfoFile(scriptDir)
        return if (infoByInfoFile.scriptMate != null) infoByInfoFile else ScriptInfoModel().parseMainFile(scriptDir)
    }

    private fun loadScope(info: ScriptInfoModel): ScriptScopeSnapshot? {
        val path = info.scriptPath ?: return null
        return runCatching { ScriptScopeRepository.load(File(path), validate = false) }.getOrNull()
    }

    private fun bindViews() {
        ivIcon = findViewById(R.id.iv_icon)
        tvName = findViewById(R.id.tv_name)
        tvId = findViewById(R.id.tv_id)
        tvTypeBadge = findViewById(R.id.tv_type_badge)
        layoutActions = findViewById(R.id.layout_actions)
        btnEdit = findViewById(R.id.btn_edit)
        btnRun = findViewById(R.id.btn_run)
        tvVersion = findViewById(R.id.tv_version)
        tvMeta = findViewById(R.id.tv_meta)
        tvIntro = findViewById(R.id.tv_intro)
        tvDeviceSummary = findViewById(R.id.tv_device_summary)
        tvDependencySummary = findViewById(R.id.tv_dependency_summary)
        tvPermissionSummary = findViewById(R.id.tv_permission_summary)
        layoutDevices = findViewById(R.id.layout_device_chips)
        layoutDependencies = findViewById(R.id.layout_dependency_chips)
        layoutPermissions = findViewById(R.id.layout_permission_chips)
        ResourceDetailViewFactory.styleActionButton(btnEdit as? TextView, ResourceDetailActionStyle.ACCENT)
        // 工作流始终显示底部栏和更多按钮
        layoutActions?.visibility = View.VISIBLE
    }

    private fun bindActions() {
        btnEdit?.setOnClickListener { editWorkflow() }
        btnRun?.setOnClickListener { runWorkflow() }
        val header = findViewById<com.carlos.ui.header.CommonHeader>(R.id.header_view)
        header.setRightClickListener { showMoreMenu(header.getRightLayout()) }
    }

    private fun render(info: ScriptInfoModel) {
        val mate = info.scriptMate
        tvName?.text = info.scriptName ?: File(info.scriptPath ?: "").name
        tvId?.text = mate?.scriptUid?.takeIf { it.isNotBlank() }
            ?: info.scriptPath?.takeIf { it.isNotBlank() }
            ?: File(info.scriptPath ?: "").name
        tvTypeBadge?.text = getString(com.hive.i8n.R.string.rp_type_workflow)
        ResourceDetailTypeStyleResolver.applyBadge(
            tvTypeBadge,
            ResourceDetailType.WORKFLOW,
            ResourceDetailBadgeVariant.FILLED
        )
        tvVersion?.text = "v${mate?.version ?: 1}"
        ResourceDetailTypeStyleResolver.applyBadge(
            tvVersion,
            ResourceDetailType.WORKFLOW,
            ResourceDetailBadgeVariant.SUBTLE
        )
        tvMeta?.text = buildMetaLine(info)

        val introText = buildIntroMarkdown(info)
        val isEmptyIntro = introText == getString(com.hive.i8n.R.string.workflow_detail_intro_empty)
        tvIntro?.loadMarkdown(introText)
        tvIntro?.setTextColor(
            if (isEmptyIntro) {
                ContextCompat.getColor(this, com.hive.i8n.R.color.design_text_muted)
            } else {
                ContextCompat.getColor(this, android.R.color.white)
            }
        )

        bindIcon(info)
        bindDevices(mate)
        bindDependencies(currentScopeSnapshot)
        bindPermissions(mate?.permission.orEmpty())
    }

    private fun bindIcon(info: ScriptInfoModel) {
        val mate = info.scriptMate
        val iconView = ivIcon ?: return
        val iconName = mate?.icon?.takeIf { it.isNotBlank() }
        if (iconName != null) {
            ImageLoader.getInstance().loadImageNoCache(this, iconView.getIconImageView(), "${info.scriptPath}/$iconName")
            iconView.markRemoteIconLoaded()
            return
        }
        iconView.setResourceType("workflow")
    }

    private fun bindDevices(mate: ScriptMate?) {
        val devices = mate?.device
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        if (devices.isEmpty()) {
            tvDeviceSummary?.visibility = View.VISIBLE
            tvDeviceSummary?.text = getString(com.hive.i8n.R.string.rp_all_devices_compatible)
            layoutDevices?.visibility = View.GONE
            layoutDevices?.removeAllViews()
            return
        }
        tvDeviceSummary?.visibility = View.GONE
        layoutDevices?.visibility = View.VISIBLE
        fillChipGroup(layoutDevices, devices)
    }

    private fun bindDependencies(snapshot: ScriptScopeSnapshot?) {
        val dependencies = buildList {
            snapshot?.scripts?.forEach { script ->
                add(DependencyItem(DependencyType.WORKFLOW, script.name, script.scriptUid, script.scriptDir))
            }
            snapshot?.skills?.forEach { skill ->
                add(DependencyItem(DependencyType.SKILL, skill.name, skill.id, skill.skillDir))
            }
            snapshot?.tools?.forEach { tool ->
                add(
                    DependencyItem(
                        type = DependencyType.TOOL,
                        name = tool.name,
                        identifier = tool.functionName,
                        scriptDir = tool.scriptDir,
                        description = tool.description
                    )
                )
            }
        }.distinctBy { it.name }

        if (dependencies.isEmpty()) {
            tvDependencySummary?.visibility = View.VISIBLE
            tvDependencySummary?.text = getString(com.hive.i8n.R.string.rp_detail_no_dependencies)
            layoutDependencies?.visibility = View.GONE
            layoutDependencies?.removeAllViews()
            return
        }
        tvDependencySummary?.visibility = View.GONE
        layoutDependencies?.visibility = View.VISIBLE
        fillChipGroupWithClick(layoutDependencies, dependencies)
    }

    private fun bindPermissions(rawPermissions: List<String>) {
        val permissions = rawPermissions
            .map { ScriptHelper.mPermissionMap[it] ?: it }
            .filter { it.isNotBlank() }
            .distinct()
        if (permissions.isEmpty()) {
            tvPermissionSummary?.visibility = View.VISIBLE
            tvPermissionSummary?.text = getString(com.hive.i8n.R.string.rp_detail_no_special_permissions)
            layoutPermissions?.visibility = View.GONE
            layoutPermissions?.removeAllViews()
            return
        }
        tvPermissionSummary?.visibility = View.GONE
        layoutPermissions?.visibility = View.VISIBLE
        fillChipGroup(layoutPermissions, permissions)
    }

    private fun fillChipGroup(container: FlowLayout?, values: List<String>) {
        val target = container ?: return
        target.removeAllViews()
        values.forEach { value ->
            target.addView(ResourceDetailViewFactory.createChip(this, value))
        }
    }

    private fun fillChipGroupWithClick(container: FlowLayout?, items: List<DependencyItem>) {
        val target = container ?: return
        target.removeAllViews()
        val clickableIcon = ContextCompat.getDrawable(this, com.hive.views.R.drawable.ic_dependency_clickable)
        val iconSize = resources.getDimensionPixelSize(com.hive.i8n.R.dimen.design_spacing_4)
        clickableIcon?.setBounds(0, 0, iconSize, iconSize)
        items.forEach { item ->
            val chip = ResourceDetailViewFactory.createChip(
                context = this,
                text = item.name,
                clickable = true,
                onClick = { onDependencyClick(item) }
            ).apply {
                setCompoundDrawablesRelative(null, null, clickableIcon, null)
                compoundDrawablePadding = resources.getDimensionPixelSize(com.hive.i8n.R.dimen.design_spacing_1)
            }
            target.addView(chip)
        }
    }

    private fun onDependencyClick(item: DependencyItem) {
        val scriptPath = currentData?.scriptPath ?: return

        when (item.type) {
            DependencyType.WORKFLOW -> {
                val scriptDir = item.scriptDir
                val dependencyPath = File(ScriptScopeRepository.getScriptsDir(File(scriptPath)), scriptDir)
                if (dependencyPath.exists() && dependencyPath.isDirectory) {
                    start(this, dependencyPath.absolutePath)
                } else {
                    CommonToast.show(com.hive.i8n.R.string.sc_error_file_not_exist)
                }
            }
            DependencyType.SKILL -> {
                val skillDir = item.scriptDir
                val skillsDir = ScriptScopeRepository.getSkillsDir(File(scriptPath))
                val skillFile = File(skillsDir, skillDir)

                val skillSpec = SkillFileHelper.readSkillFileFromDir(skillsDir, skillDir)
                if (skillSpec != null) {
                    dependencyNavigationProvider?.openSkillDetail(this, skillSpec)
                } else {
                    CommonToast.show("Skill 文件不存在或格式错误: ${skillFile.absolutePath}")
                }
            }
            DependencyType.TOOL -> {
                val scriptDir = item.scriptDir
                val toolPath = File(ScriptScopeRepository.getToolsDir(File(scriptPath)), scriptDir)
                if (toolPath.exists() && toolPath.isDirectory) {
                    dependencyNavigationProvider?.openToolDetail(
                        this,
                        item.identifier.orEmpty(),
                        item.name,
                        item.description.orEmpty(),
                        McpConst.Tool_Type_Custom,
                        "{}",
                        toolPath.absolutePath
                    )
                } else {
                    CommonToast.show(com.hive.i8n.R.string.sc_error_file_not_exist)
                }
            }
        }
    }

    private fun buildMetaLine(info: ScriptInfoModel): String {
        val mate = info.scriptMate
        if ((mate?.updateTime ?: 0L) > 0L) {
            return getString(
                com.hive.i8n.R.string.workflow_detail_updated_at,
                StringUtils.dateFormat(Date(mate?.updateTime ?: 0L), "yyyy-MM-dd HH:mm")
            )
        }
        return ""
    }

    private fun buildIntroMarkdown(info: ScriptInfoModel): String {
        val path = info.scriptPath ?: return getString(com.hive.i8n.R.string.workflow_detail_intro_empty)
        val introFile = listOf("README.md", "readme.md", "intro.md", "desc.md", "description.md")
            .map { File(path, it) }
            .firstOrNull { it.exists() && it.isFile }
        val introFromFile = introFile?.readText()?.trim().orEmpty()
        if (introFromFile.isNotBlank()) return introFromFile
        val tag = info.scriptMate?.tag?.trim().orEmpty()
        if (tag.isNotBlank()) return tag
        return getString(com.hive.i8n.R.string.workflow_detail_intro_empty)
    }

    private fun editWorkflow() {
        val info = currentData ?: return
        if (info.scriptMate?.hasControlEdit() != true) {
            CommonToast.show(com.hive.i8n.R.string.sc_no_permission_edit)
            return
        }
        DialogScriptEdit.create(info.scriptMate)
            ?.setScriptPath(info.scriptPath!!)
            ?.setTitleName(File(info.scriptPath!!).name)
            ?.show()
    }

    private fun shareWorkflow() {
        val info = currentData ?: return
        if (info.scriptMate?.hasControlShare() != true) {
            CommonToast.show(com.hive.i8n.R.string.sc_no_permission_share)
            return
        }
        if (info.scriptMate?.isEncrypt() == true) {
            ScriptShareHelper.startShare(this, info, null, null, -1)
            return
        }
        DialogShareConfirm(this)
            .setOnShareConfirmListener(object : DialogShareConfirm.OnShareConfirmListener {
                override fun onShareConfirm(
                    dialog: DialogShareConfirm,
                    encrypt: Boolean,
                    pwd: String?,
                    ctrValue: String?,
                    expireTime: Long
                ) {
                    dialog.dismiss()
                    ScriptShareHelper.startShare(
                        context = this@ActivityWorkflowDetail,
                        info = info,
                        pwd = if (encrypt) pwd else null,
                        ctrValue = ctrValue,
                        expireTime = expireTime
                    )
                }
            })
            .show()
    }

    private fun confirmDelete() {
        val info = currentData ?: return
        DialogScriptAlert(this)
            .setTitle(com.hive.i8n.R.string.sc_delete_title)
            .setContent(com.hive.i8n.R.string.sc_delete_content)
            .setConfirmText(com.hive.i8n.R.string.sc_delete_confirm)
            .setOnDialogEventListener(object : DialogScriptAlert.OnDialogEventListener {
                override fun onClickEvent(dialog: DialogScriptAlert, isCancel: Boolean) {
                    dialog.dismiss()
                    if (!isCancel) {
                        deleteWorkflow(info)
                    }
                }
            })
            .show()
    }

    private fun deleteWorkflow(info: ScriptInfoModel) {
        activityScope.launch {
            withContext(Dispatchers.IO) {
                info.delete()
            }
            EventBus.getDefault().post(RefreshScriptListEvent())
            CommonToast.show(com.hive.i8n.R.string.sc_delete_success)
            finish()
        }
    }

    private fun runWorkflow() {
        val info = currentData ?: return
        if (info.scriptMate?.hasControlRun() != true) {
            CommonToast.show(com.hive.i8n.R.string.sc_no_permission_run)
            return
        }

        // 2026-04: 运行前检查辅助功能权限，未开启时提示用户
        if (ScriptManager.checkAccessibility()) {
            return  // checkAccessibility 内部已显示引导对话框
        }

        scriptProvider?.executeScript(info.scriptPath, false) ?: ScriptProvider().executeScript(info.scriptPath, false)
    }

    private fun renameWorkflow() {
        val info = currentData ?: return
        DialogInputMessage(
            this,
            title = getString(com.hive.i8n.R.string.str_task_name),
            hint = getString(com.hive.i8n.R.string.sc_dialog_name_hint),
            txtHold = info.scriptName,
            inputType = 0,
            checkInputFun = { editText ->
                val name = editText.text.toString().trim()
                if (TextUtils.isEmpty(name)) {
                    throw Exception(getString(com.hive.i8n.R.string.sc_check_input_check_empty))
                }
                if (name.length > 50) {
                    throw Exception(getString(com.hive.i8n.R.string.sc_check_input_check_empty_3))
                }
                if (name != info.scriptName && File("${ScriptConst.Save_Script_Path}/$name/").exists()) {
                    throw Exception(getString(com.hive.i8n.R.string.sc_check_input_check_empty_4))
                }
            },
            confirmFun = { dialog, name ->
                val oldName = info.scriptName ?: return@DialogInputMessage
                val renamed = File("${ScriptConst.Save_Script_Path}/$oldName/").renameTo(
                    File("${ScriptConst.Save_Script_Path}/$name/")
                )
                dialog.dismiss()
                if (!renamed) {
                    CommonToast.show(com.hive.i8n.R.string.sc_error)
                    return@DialogInputMessage
                }
                intent.putExtra(EXTRA_SCRIPT_PATH, "${ScriptConst.Save_Script_Path}/$name/")
                reloadData()
                EventBus.getDefault().post(RefreshScriptListEvent())
            }
        ).show()
    }

    private fun copyWorkflow() {
        val info = currentData ?: return
        val sourcePath = info.scriptPath ?: return
        val sourceDir = File(sourcePath)
        if (!sourceDir.exists()) {
            CommonToast.show(com.hive.i8n.R.string.sc_error_file_not_exist)
            return
        }
        val targetName = generateCopyName(info.scriptName ?: sourceDir.name) { name ->
            File("${ScriptConst.Save_Script_Path}/$name/").exists()
        }
        val targetDir = File("${ScriptConst.Save_Script_Path}/$targetName/")
        runCatching {
            FileUtils.makeDirs(targetDir.absolutePath)
            FileUtils.copyFolderTo(sourceDir.absolutePath, targetDir.absolutePath)
        }.onSuccess {
            CommonToast.show(com.hive.i8n.R.string.sc_copy_success)
            EventBus.getDefault().post(RefreshScriptListEvent())
            finish()
        }.onFailure {
            CommonToast.show(com.hive.i8n.R.string.sc_copy_fail)
        }
    }

    private fun reloadData() {
        val info = loadInfoFromIntent()
        if (info == null) {
            finish()
            return
        }
        currentData = info
        currentScopeSnapshot = loadScope(info)
        render(info)
    }

    private fun showMoreMenu(anchor: View) {
        moreMenuPopup?.dismiss()
        moreMenuPopup = ResourceOverflowMenuHelper.show(
            anchor = anchor,
            actions = listOf(
                ResourceOverflowAction(getString(com.hive.i8n.R.string.btn_file_rename)) { renameWorkflow() },
                ResourceOverflowAction(getString(com.hive.i8n.R.string.btn_file_copy)) { copyWorkflow() },
                ResourceOverflowAction(getString(com.hive.i8n.R.string.workflow_detail_share_export)) { shareWorkflow() },
                ResourceOverflowAction(getString(com.hive.i8n.R.string.delete), danger = true) { confirmDelete() }
            )
        )
    }

    private fun showDeleteConfirm(resourceName: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(com.hive.i8n.R.string.agent_tool_delete_title))
            .setMessage(getString(com.hive.i8n.R.string.agent_tool_delete_message, resourceName))
            .setNegativeButton(getString(com.hive.i8n.R.string.cancel), null)
            .setPositiveButton(getString(com.hive.i8n.R.string.delete)) { _, _ ->
                val info = currentData ?: return@setPositiveButton
                deleteWorkflow(info)
            }
            .show()
    }

    private fun generateCopyName(baseName: String, exists: (String) -> Boolean): String {
        var candidate = "$baseName (Copy)"
        var suffix = 2
        while (exists(candidate)) {
            candidate = "$baseName (Copy $suffix)"
            suffix++
        }
        return candidate
    }

    companion object {
        private const val EXTRA_SCRIPT_PATH = "script_path"

        fun start(context: Context, scriptPath: String) {
            val intent = Intent(context, ActivityWorkflowDetail::class.java).apply {
                putExtra(EXTRA_SCRIPT_PATH, scriptPath)
            }
            IntentUtils.safeStartActivity(context, intent)
        }
    }

    private enum class DependencyType {
        WORKFLOW,
        SKILL,
        TOOL
    }

    private data class DependencyItem(
        val type: DependencyType,
        val name: String,
        val identifier: String?,  // scriptUid for Workflow, skillId for Skill, toolUid for Tool
        val scriptDir: String,    // dependence 下的目录名
        val description: String? = null
    )
}

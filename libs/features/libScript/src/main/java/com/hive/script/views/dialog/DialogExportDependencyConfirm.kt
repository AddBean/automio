// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.dialog

import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hive.plugin.agent.model.SkillSpec
import com.hive.script.R
import com.hive.script.scope.ExportScanResult
import com.hive.script.scope.ExportToolItem
import com.hive.script.views.widgets.BaseScriptDialog
import com.hive.utils.GlobalApp

/**
 * 依赖选择器对话框（对齐 script-design DependencySelectSheet.tsx）
 * - 双列网格布局（gap-2 = 8dp）
 * - Tab 切换（依赖资源 / 所需权限）
 * - 选中状态标记
 */
class DialogExportDependencyConfirm(context: Context?) : BaseScriptDialog(context) {

    // ========== Properties ==========

    private var scanResult: ExportScanResult? = null
    private var onConfirmListener: OnConfirmListener? = null

    private var layoutTabContainer: View? = null
    private var tabDependency: TextView? = null
    private var tabPermission: TextView? = null
    private var tvPermissionHint: View? = null
    private var recyclerView: RecyclerView? = null
    private var btnCancel: TextView? = null
    private var btnConfirm: TextView? = null
    private var layoutContent: View? = null

    private var currentTab = TAB_DEPENDENCY

    private val selectedSkillIds = mutableSetOf<String>()
    private val selectedToolPaths = mutableSetOf<String>()
    private val selectedPermissions = mutableSetOf<String>()

    private lateinit var adapter: DependencyAdapter

    // 缓存 section 信息，避免重复计算
    private data class SectionInfo(
        val type: Int,
        val title: String,
        val iconRes: Int,
        val iconBgRes: Int,
        val items: List<Any>
    )
    private var cachedSections: List<SectionInfo> = emptyList()

    // ========== Lifecycle ==========

    override fun initWindow() {
        initViews()
        setupRecyclerView()
        setupListeners()
        scanResult?.let { refreshData(it) }
    }

    private fun initViews() {
        layoutContent = findViewById(R.id.layout_content)
        layoutTabContainer = findViewById(R.id.layout_tab_container)
        tabDependency = findViewById(R.id.tab_dependency)
        tabPermission = findViewById(R.id.tab_permission)
        tvPermissionHint = findViewById(R.id.tv_permission_hint)
        recyclerView = findViewById(R.id.recycler_view)
        btnCancel = findViewById(R.id.btn_cancel)
        btnConfirm = findViewById(R.id.btn_confirm)

        // 设置内容容器高度为屏幕高度的 75%（对齐前端 h-3/4）
        layoutContent?.post {
            val screenHeight = context.resources.displayMetrics.heightPixels
            val targetHeight = (screenHeight * 0.75).toInt()
            layoutContent?.layoutParams?.height = targetHeight
            layoutContent?.requestLayout()
        }
    }

    private fun setupRecyclerView() {
        adapter = DependencyAdapter()
        recyclerView?.apply {
            val gridLayoutManager = GridLayoutManager(context, 2)
            // 设置 SpanSizeLookup：section header 占满一行（2 span），卡片占半行（1 span）
            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (adapter?.getItemViewType(position) == TYPE_SECTION_HEADER) {
                        2 // section header 占满整行
                    } else {
                        1 // 卡片占半行（双列）
                    }
                }
            }
            layoutManager = gridLayoutManager
            adapter = this@DialogExportDependencyConfirm.adapter
            // Grid 间距：gap-2 = 8dp（仅对卡片生效）
            addItemDecoration(GridSpacingItemDecoration(2, 8))
            // Section 间距：space-y-3 = 12dp
            addItemDecoration(SectionSpacingItemDecoration(12))
        }
    }

    private fun setupListeners() {
        tabDependency?.setOnClickListener { switchTab(TAB_DEPENDENCY) }
        tabPermission?.setOnClickListener { switchTab(TAB_PERMISSION) }

        btnCancel?.setOnClickListener {
            dismiss()
            onConfirmListener?.onCancel()
        }

        btnConfirm?.setOnClickListener {
            dismiss()
            onConfirmListener?.onConfirm(
                selectedSkillIds.toSet(),
                selectedToolPaths.toSet(),
                selectedPermissions.toSet()
            )
        }
    }

    // ========== Public API ==========

    fun setScanResult(result: ExportScanResult): DialogExportDependencyConfirm {
        scanResult = result
        resetSelections(result)
        if (::adapter.isInitialized) {
            refreshData(result)
        }
        return this
    }

    fun setOnConfirmListener(listener: OnConfirmListener): DialogExportDependencyConfirm {
        onConfirmListener = listener
        return this
    }

    // ========== Private Methods ==========

    private fun resetSelections(result: ExportScanResult) {
        selectedSkillIds.clear()
        selectedToolPaths.clear()
        selectedPermissions.clear()
        selectedSkillIds.addAll(result.scannedSkillIds)
        selectedToolPaths.addAll(result.scannedToolPaths)
        selectedToolPaths.addAll(result.scannedScriptPaths)
        selectedPermissions.addAll(result.scannedPermissions)
    }

    private fun refreshData(result: ExportScanResult) {
        // 构建 section 缓存
        cachedSections = buildSections(result)

        // 判断是否有依赖资源
        val hasDependencies = cachedSections.isNotEmpty()

        if (!hasDependencies && currentTab == TAB_DEPENDENCY) {
            layoutTabContainer?.visibility = View.GONE
            tvPermissionHint?.visibility = View.VISIBLE
            currentTab = TAB_PERMISSION
        } else {
            layoutTabContainer?.visibility = View.VISIBLE
            tvPermissionHint?.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
        updateTabUi()
    }

    private fun buildSections(result: ExportScanResult): List<SectionInfo> {
        val sections = mutableListOf<SectionInfo>()

        // Workflow section
        val workflows = result.allTools.filter { !it.isTool }.sortedBy { it.displayName }
        if (workflows.isNotEmpty()) {
            sections.add(SectionInfo(
                type = TYPE_SECTION_WORKFLOW,
                title = GlobalApp.getString(com.hive.i8n.R.string.sc_export_dependency_workflows),
                iconRes = com.hive.i8n.R.drawable.ic_activity,
                iconBgRes = R.drawable.bg_selector_icon_container,
                items = workflows
            ))
        }

        // Skill section
        val skills = result.allSkills.sortedBy { it.name }
        if (skills.isNotEmpty()) {
            sections.add(SectionInfo(
                type = TYPE_SECTION_SKILL,
                title = GlobalApp.getString(com.hive.i8n.R.string.sc_export_dependency_skills),
                iconRes = com.hive.i8n.R.drawable.ic_sparkles,
                iconBgRes = R.drawable.bg_skill_selector_icon_container,
                items = skills
            ))
        }

        // Tool section
        val tools = result.allTools.filter { it.isTool }.sortedBy { it.displayName }
        if (tools.isNotEmpty()) {
            sections.add(SectionInfo(
                type = TYPE_SECTION_TOOL,
                title = GlobalApp.getString(com.hive.i8n.R.string.sc_export_dependency_tools),
                iconRes = com.hive.i8n.R.drawable.ic_wrench,
                iconBgRes = R.drawable.bg_tool_selector_icon_container,
                items = tools
            ))
        }

        return sections
    }

    private fun switchTab(tab: Int) {
        if (currentTab == tab) return
        currentTab = tab
        adapter.notifyDataSetChanged()
        updateTabUi()
    }

    private fun updateTabUi() {
        val depActive = currentTab == TAB_DEPENDENCY

        tabDependency?.apply {
            setBackgroundResource(if (depActive) R.drawable.design_tab_active_bg else 0)
            setTextColor(GlobalApp.getColor(
                if (depActive) com.hive.i8n.R.color.design_text_primary
                else com.hive.i8n.R.color.design_text_muted
            ))
            text = if (selectedSkillIds.size + selectedToolPaths.size > 0) {
                GlobalApp.getString(
                    com.hive.i8n.R.string.sc_export_dependency_tab_resources_with_count,
                    selectedSkillIds.size + selectedToolPaths.size
                )
            } else {
                GlobalApp.getString(com.hive.i8n.R.string.sc_export_dependency_tab_resources)
            }
        }

        tabPermission?.apply {
            setBackgroundResource(if (!depActive) R.drawable.design_tab_active_bg else 0)
            setTextColor(GlobalApp.getColor(
                if (!depActive) com.hive.i8n.R.color.design_text_primary
                else com.hive.i8n.R.color.design_text_muted
            ))
            text = if (selectedPermissions.size > 0) {
                GlobalApp.getString(
                    com.hive.i8n.R.string.sc_export_dependency_tab_permissions_with_count,
                    selectedPermissions.size
                )
            } else {
                GlobalApp.getString(com.hive.i8n.R.string.sc_export_dependency_tab_permissions)
            }
        }
    }

    override fun getWindowLayoutId(): Int = R.layout.dialog_dependency_selector

    // ========== Adapter ==========

    inner class DependencyAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int {
            if (currentTab == TAB_PERMISSION) return TYPE_CARD

            // 使用缓存计算 position 对应的类型
            var offset = 0
            cachedSections.forEach { section ->
                if (position == offset) return TYPE_SECTION_HEADER
                offset += 1 + section.items.size
            }
            return TYPE_CARD
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_SECTION_HEADER) {
                SectionHeaderViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_dependency_section_header, parent, false)
                )
            } else {
                CardViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_dependency_card, parent, false)
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is SectionHeaderViewHolder -> holder.bind(position)
                is CardViewHolder -> holder.bind(position)
            }
        }

        override fun getItemCount(): Int {
            return if (currentTab == TAB_DEPENDENCY) {
                // 每个 section: 1 header + N items
                cachedSections.sumOf { 1 + it.items.size }
            } else {
                scanResult?.allPermissions?.size ?: 0
            }
        }
    }

    // ========== ViewHolders ==========

    inner class SectionHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        private val tvTitle: TextView = view.findViewById(R.id.tv_title)
        private val iconContainer: FrameLayout = view.findViewById(R.id.icon_container)

        fun bind(position: Int) {
            // 根据 position 找到对应的 section
            var offset = 0
            cachedSections.forEach { section ->
                if (position == offset) {
                    ivIcon.setImageResource(section.iconRes)
                    iconContainer.setBackgroundResource(section.iconBgRes)
                    tvTitle.text = section.title
                    return
                }
                offset += 1 + section.items.size
            }
        }
    }

    inner class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        private val tvName: TextView = view.findViewById(R.id.tv_name)
        private val iconContainer: FrameLayout = view.findViewById(R.id.icon_container)
        private val checkContainer: View = view.findViewById(R.id.check_container)

        init {
            view.setOnClickListener {
                toggleSelection(bindingAdapterPosition)
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }

        fun bind(position: Int) {
            if (currentTab == TAB_DEPENDENCY) {
                bindDependencyCard(position)
            } else {
                bindPermissionCard(position)
            }
        }

        private fun bindDependencyCard(position: Int) {
            // 根据 position 找到对应的 item
            var offset = 0
            cachedSections.forEach { section ->
                val itemIndex = position - offset - 1
                if (itemIndex in section.items.indices) {
                    when (section.type) {
                        TYPE_SECTION_SKILL -> bindSkillCard(section.items[itemIndex] as SkillSpec, section)
                        TYPE_SECTION_WORKFLOW, TYPE_SECTION_TOOL -> bindToolCard(
                            section.items[itemIndex] as ExportToolItem,
                            section.iconRes,
                            section.iconBgRes
                        )
                    }
                    return
                }
                offset += 1 + section.items.size
            }
        }

        private fun bindSkillCard(spec: SkillSpec, section: SectionInfo) {
            ivIcon.setImageResource(section.iconRes)
            iconContainer.setBackgroundResource(section.iconBgRes)
            tvName.text = spec.name

            val isSelected = selectedSkillIds.contains(spec.id)
            itemView.isSelected = isSelected
            checkContainer.visibility = if (isSelected) View.VISIBLE else View.GONE
            tvName.setTextColor(
                GlobalApp.getColor(
                    if (isSelected) com.hive.i8n.R.color.design_accent_indigo
                    else com.hive.i8n.R.color.design_text_slate_400
                )
            )
        }

        private fun bindToolCard(item: ExportToolItem, iconRes: Int, iconBgRes: Int) {
            ivIcon.setImageResource(iconRes)
            iconContainer.setBackgroundResource(iconBgRes)
            tvName.text = item.displayName

            val isSelected = selectedToolPaths.contains(item.canonicalPath)
            itemView.isSelected = isSelected
            checkContainer.visibility = if (isSelected) View.VISIBLE else View.GONE
            tvName.setTextColor(
                GlobalApp.getColor(
                    if (isSelected) com.hive.i8n.R.color.design_accent_indigo
                    else com.hive.i8n.R.color.design_text_slate_400
                )
            )
        }

        private fun bindPermissionCard(position: Int) {
            val result = scanResult ?: return
            val sortedPermissions = result.allPermissions.sortedBy { it.second }
            if (position !in sortedPermissions.indices) return

            val (permId, displayName) = sortedPermissions[position]

            // 根据权限类型设置图标和背景色
            val (iconRes, iconBgRes) = when (permId) {
                "a11y" -> com.hive.i8n.R.drawable.ic_shield_check to R.drawable.bg_selector_icon_container
                "screenContent" -> com.hive.i8n.R.drawable.ic_monitor to R.drawable.bg_skill_selector_icon_container
                "autoStart" -> com.hive.i8n.R.drawable.ic_power to R.drawable.bg_tool_selector_icon_container
                "notification" -> com.hive.i8n.R.drawable.ic_bell to R.drawable.bg_skill_selector_icon_container
                "wallpaper" -> com.hive.i8n.R.drawable.ic_image to R.drawable.bg_tool_selector_icon_container
                "batteryOpt" -> com.hive.i8n.R.drawable.ic_battery to R.drawable.bg_selector_icon_container
                else -> com.hive.i8n.R.drawable.ic_shield to R.drawable.bg_dependency_icon_container
            }

            ivIcon.setImageResource(iconRes)
            iconContainer.setBackgroundResource(iconBgRes)
            tvName.text = displayName

            // 权限卡片使用 emerald 颜色（使用 selector 自动切换背景）
            val isSelected = selectedPermissions.contains(permId)
            itemView.isSelected = isSelected
            itemView.setBackgroundResource(R.drawable.bg_permission_card_selected)
            checkContainer.visibility = if (isSelected) View.VISIBLE else View.GONE
            checkContainer.setBackgroundResource(R.drawable.bg_permission_check_circle)
            tvName.setTextColor(
                GlobalApp.getColor(
                    if (isSelected) com.hive.i8n.R.color.design_accent_emerald
                    else com.hive.i8n.R.color.design_text_slate_400
                )
            )
        }
    }

    // ========== Selection Logic ==========

    private fun toggleSelection(position: Int) {
        if (currentTab == TAB_DEPENDENCY) {
            toggleDependencySelection(position)
        } else {
            togglePermissionSelection(position)
        }
    }

    private fun toggleDependencySelection(position: Int) {
        // 根据 position 找到对应的 item 并切换选中状态
        var offset = 0
        cachedSections.forEach { section ->
            val itemIndex = position - offset - 1
            if (itemIndex in section.items.indices) {
                when (section.type) {
                    TYPE_SECTION_SKILL -> {
                        val spec = section.items[itemIndex] as SkillSpec
                        if (selectedSkillIds.contains(spec.id)) {
                            selectedSkillIds.remove(spec.id)
                        } else {
                            selectedSkillIds.add(spec.id)
                        }
                    }
                    TYPE_SECTION_WORKFLOW, TYPE_SECTION_TOOL -> {
                        val item = section.items[itemIndex] as ExportToolItem
                        if (selectedToolPaths.contains(item.canonicalPath)) {
                            selectedToolPaths.remove(item.canonicalPath)
                        } else {
                            selectedToolPaths.add(item.canonicalPath)
                        }
                    }
                }
                adapter.notifyItemChanged(position)
                updateTabUi()
                return
            }
            offset += 1 + section.items.size
        }
    }

    private fun togglePermissionSelection(position: Int) {
        val result = scanResult ?: return
        val sortedPermissions = result.allPermissions.sortedBy { it.second }
        if (position !in sortedPermissions.indices) return

        val (permId, _) = sortedPermissions[position]
        if (selectedPermissions.contains(permId)) {
            selectedPermissions.remove(permId)
        } else {
            selectedPermissions.add(permId)
        }
        adapter.notifyItemChanged(position)
        updateTabUi()
    }

    // ========== Item Decorations ==========

    /**
     * Grid 间距装饰器（对齐 Tailwind grid-cols-2 gap-2 = 8dp）
     * 仅对卡片 item 生效，section header 不加间距
     */
    private inner class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int // dp
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val adapter = parent.adapter ?: return

            // Section header 不加间距
            val viewType = adapter.getItemViewType(position)
            if (viewType == TYPE_SECTION_HEADER) {
                return
            }

            // 仅对卡片加间距
            val spacingPx = (spacing * parent.context.resources.displayMetrics.density).toInt()

            // 计算当前卡片在网格中的位置
            // 需要排除 section header 的影响
            var cardIndex = 0
            for (i in 0 until position) {
                if (adapter.getItemViewType(i) != TYPE_SECTION_HEADER) {
                    cardIndex++
                }
            }

            val column = cardIndex % spanCount // 0 = 左列, 1 = 右列

            // 横向间距：gap-2 = 8dp（左右卡片之间）
            // 左卡片：右间距 4dp；右卡片：左间距 4dp
            if (column == 0) {
                outRect.right = spacingPx / 2
            } else {
                outRect.left = spacingPx / 2
            }

            // 纵向间距：gap-2 = 8dp（上下卡片之间）
            // 每个卡片下方加 8dp（通过 RecyclerView paddingBottom 实现最后一行的间距）
            outRect.bottom = spacingPx
        }
    }

    /**
     * Section 间距装饰器（对齐 Tailwind space-y-3 = 12dp）
     * 为 section header 添加顶部间距
     */
    private class SectionSpacingItemDecoration(
        private val spacing: Int // dp
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val adapter = parent.adapter ?: return

            // 检查是否是 section header（通过 viewType）
            val viewType = adapter.getItemViewType(position)
            if (viewType == TYPE_SECTION_HEADER) {
                // 转换 dp 到 px
                val spacingPx = (spacing * parent.context.resources.displayMetrics.density).toInt()
                outRect.top = spacingPx
            }
        }
    }

    // ========== Interface & Constants ==========

    interface OnConfirmListener {
        fun onConfirm(
            selectedSkillIds: Set<String>,
            selectedToolPaths: Set<String>,
            selectedPermissions: Set<String>
        )
        fun onCancel()
    }

    companion object {
        private const val TAB_DEPENDENCY = 0
        private const val TAB_PERMISSION = 1

        private const val TYPE_SECTION_HEADER = 0
        private const val TYPE_CARD = 1

        private const val TYPE_SECTION_WORKFLOW = 0
        private const val TYPE_SECTION_SKILL = 1
        private const val TYPE_SECTION_TOOL = 2
    }
}

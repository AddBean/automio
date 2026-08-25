// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hive.agent.views.FragmentAgentDebug
import com.hive.app.script.R
import com.hive.base.BaseFragment
import com.hive.base.BaseFragmentActivity
import com.hive.base.CommonFragmentActivity
import com.hive.config.BuildConfigHelper
import com.hive.engineer.EngineerHelper
import com.hive.event.AgentEvent
import com.hive.event.AgentEventType
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.permissions.PermissionsChecker
import com.hive.plugin.ComponentManager
import com.hive.app.script.BuildConfig
import com.hive.script.ScriptProvider
import com.hive.script.base.ScriptConst
import com.hive.script.driver.ScriptNotificationService
import com.hive.script.event.OnMcpToolEvent
import com.hive.script.utils.ScriptPermissionManager
import com.hive.script.views.manager.ScriptManager
import com.hive.timer.utils.CalendarUtils
import com.hive.utils.GCDefaultConst
import com.hive.utils.GlobalApp
import com.hive.utils.bar.ImmersionBar
import com.hive.utils.debug.DLog
import com.hive.utils.extends.color
import com.hive.utils.extends.visibleOrGone
import com.hive.utils.global.SPTools
import com.hive.utils.thread.UIHandlerUtils
import com.hive.utils.utils.IntentUtils
import com.hive.views.IBackListener
import com.hive.views.SampleDialog
import com.hive.views.TabButtonLayout
import com.hive.views.widgets.CommonToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.system.exitProcess
import com.hive.i8n.R as i8nR

class ActivityTab : BaseFragmentActivity(), TabButtonLayout.OnTabSelectedListener {
    private lateinit var mainTabTags: List<String>

    private var mCurrentTag = "f0"
    private var mImmersionBar: ImmersionBar? = null
    private var mPermissionsChecker: PermissionsChecker? = null

    private var layout_root: ViewGroup? = null
    private var layout_tabs: LinearLayout? = null

    private var tvMainToolbarTitle: TextView? = null
    private var layoutA11yBanner: View? = null

    private var statusBarView: View? = null


    private var layoutTitle: View? = null

    private var layoutToolbarA11y: View? = null
    private var ivToolbarA11y: ImageView? = null
    private var tvToolbarA11y: TextView? = null
    private var layoutToolbarAgentActions: View? = null
    private var btnToolbarAgentNewTask: View? = null
    private var btnToolbarAgentHistory: View? = null
    private var showAgentToolbarActions = true

    override fun getLayoutId(): Int {
        return R.layout.activity_tab
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CommonIntentHandler.instance.handleIntent(this, intent)
    }

    private fun removeSavedState(savedState: Bundle?) {
        if (savedState != null && savedState.getBoolean("destroyTag", false)) {
            clearAllFragments()
        }
    }

    override fun doOnCreate(savedState: Bundle?) {
        sInstance = this
        removeSavedState(savedState)
        GlobalApp.sMainActivity = this
        EventBus.getDefault().register(this)
        layout_root = findViewById(R.id.layout_root)
        layout_tabs = findViewById(R.id.layout_tabs)
        applyBottomNavWindowInsets()
        initMainToolbar()

        CommonIntentHandler.instance.handleIntent(this, intent)
        initFragments()
        showFirstPermissionDialog()

        startNotificationService()
        EngineerHelper.registerSwitcher("编辑自由拖动", "script_move_pin", false)
        EngineerHelper.registerTestEvent("Agent调试") {
            CommonFragmentActivity.start(this, FragmentAgentDebug::class.java)
        }
        EngineerHelper.registerTestEvent(getString(i8nR.string.engineer_generate_tag_test_scripts)) {
            ScriptManager.createTaggedTestScripts(
                tag = getString(i8nR.string.script_tag_test_observe),
                prefix = getString(i8nR.string.script_name_prefix_test_observe),
                count = 5
            ) { paths ->
                val tipRes = if (paths.isEmpty()) {
                    i8nR.string.engineer_generate_tag_test_scripts_failed
                } else {
                    i8nR.string.engineer_generate_tag_test_scripts_success
                }
                CommonToast.show(getString(tipRes, paths.size))
            }
        }

        //        if (MarkdownHelper.isTest) {
//            MarkdownHelper.test(this)
//        }
    }

    private fun startNotificationService() {
        if (ScriptPermissionManager.isNotificationOpen(this)) {
            ScriptNotificationService.start(this)
        }
    }

    private val FIRST_ENTER_PERMISSION = "FIRST_ENTER_PERMISSION"

    /**
     * 显示第一次权限弹窗
     */
    @SuppressLint("RtlHardcoded")
    private fun showFirstPermissionDialog() {
        if (!SPTools.getInstance().getBoolean(
                FIRST_ENTER_PERMISSION,
                false
            ) && BuildConfigHelper.getMapBoolean("showPermissionDialog")
        ) {
            val dialog = SampleDialog(this)
            dialog.mViewHolder.mTvContent.gravity = Gravity.START
            dialog.setDialogTitle(getString(com.hive.i8n.R.string.first_enter_permission_title))
            dialog.setDialogContent(getString(com.hive.i8n.R.string.first_enter_permission_content))
            dialog.setLeftText(getString(com.hive.i8n.R.string.agreement_left_text))
            dialog.setRightText(getString(com.hive.i8n.R.string.agreement_right_text))
            dialog.setOnDialogListener { isRight: Boolean ->
                dialog.dismiss()
                if (!isRight) {
                    SPTools.getInstance().putBoolean(FIRST_ENTER_PERMISSION, false)
                    exitProcess(-1)
                } else {
                    SPTools.getInstance().putBoolean(FIRST_ENTER_PERMISSION, true)
                }
            }
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    private fun initMainToolbar() {
        tvMainToolbarTitle = findViewById(R.id.tv_toolbar_title)
        layoutA11yBanner = findViewById(R.id.layout_a11y_banner)
        statusBarView = findViewById(R.id.status_bar_view)
        layoutTitle = findViewById(R.id.layout_title)

        layoutToolbarA11y = findViewById(R.id.layout_toolbar_a11y)
        ivToolbarA11y = findViewById(R.id.iv_toolbar_a11y_icon)
        tvToolbarA11y = findViewById(R.id.tv_toolbar_a11y_label)
        layoutToolbarAgentActions = findViewById(R.id.layout_toolbar_agent_actions)
        btnToolbarAgentNewTask = findViewById(R.id.btn_toolbar_agent_new_task)
        btnToolbarAgentHistory = findViewById(R.id.btn_toolbar_agent_history)
        btnToolbarAgentNewTask?.setOnClickListener {
            (getCurrentTabFragment() as? com.hive.ui.agent.AgentMainFragment)?.startToolbarNewTask()
        }
        btnToolbarAgentHistory?.setOnClickListener {
            (getCurrentTabFragment() as? com.hive.ui.agent.AgentMainFragment)?.showToolbarHistory()
        }
        layoutToolbarA11y?.setOnClickListener {
            ScriptProvider.startToAccessibilitySetting()
        }
        findViewById<View>(R.id.btn_a11y_banner_action)?.setOnClickListener {
            ScriptProvider.startToAccessibilitySetting()
        }
    }

    private fun refreshMainToolbar() {
        updateToolbarTitle(mCurrentTag)
        refreshToolbarAccessibility()
        refreshToolbarAgentActions()
    }

    private fun updateToolbarTitle(tag: String) {
        val res = when (tag) {
            "f2" -> i8nR.string.design_nav_agent
            "f3" -> i8nR.string.design_nav_workflow
            "f4" -> i8nR.string.design_nav_profile
            else -> i8nR.string.design_nav_agent
        }
        tvMainToolbarTitle?.setText(res)
    }

    private fun refreshToolbarAccessibility() {
        val enabled = ScriptManager.checkServerEnable()
        val showA11yPill = mCurrentTag != "f2"
        if (enabled) {
            layoutToolbarA11y?.setBackgroundResource(R.drawable.bg_toolbar_a11y_enabled)
            ivToolbarA11y?.setImageResource(i8nR.drawable.ic_shield_check)
            ivToolbarA11y?.setColorFilter(GlobalApp.getColor(i8nR.color.design_a11y_pill_enabled_text))
            tvToolbarA11y?.setText(i8nR.string.main_toolbar_a11y_enabled_short)
            tvToolbarA11y?.setTextColor(GlobalApp.getColor(i8nR.color.design_a11y_pill_enabled_text))
        } else {
            layoutToolbarA11y?.setBackgroundResource(R.drawable.bg_toolbar_a11y_disabled)
            ivToolbarA11y?.setImageResource(i8nR.drawable.ic_shield_alert)
            ivToolbarA11y?.setColorFilter(GlobalApp.getColor(i8nR.color.design_a11y_pill_disabled_text))
            tvToolbarA11y?.setText(i8nR.string.main_toolbar_a11y_disabled_short)
            tvToolbarA11y?.setTextColor(GlobalApp.getColor(i8nR.color.design_a11y_pill_disabled_text))
        }
        layoutToolbarA11y?.visibleOrGone(showA11yPill)
        layoutA11yBanner?.visibleOrGone(!enabled)
        layoutTitle?.visibleOrGone(enabled)
        statusBarView?.setBackgroundColor((if (!enabled) i8nR.color.design_a11y_banner_bg.color() else Color.TRANSPARENT))
    }

    private fun refreshToolbarAgentActions() {
        val isAgentTab = mCurrentTag == "f2"
        layoutToolbarAgentActions?.visibleOrGone(isAgentTab && showAgentToolbarActions)
        if (isAgentTab) {
            (getCurrentTabFragment() as? com.hive.ui.agent.AgentMainFragment)?.refreshToolbarActions()
        }
    }

    private fun initFragments() {
        mainTabTags = GCDefaultConst.getDefaultTabs()
            .filter { it.isEnable }
            .mapNotNull { it.tag }
        TabHelper.initTabs(layout_tabs!!)
        initTabListeners()
        val startIndex = mainTabTags.indexOf(TabHelper.defaultTag).takeIf { it >= 0 } ?: 0
        mCurrentTag = mainTabTags.getOrElse(startIndex) { TabHelper.defaultTag }
        for (tag in mainTabTags) {
            ensureMainTabFragment(tag)
        }
        switchMainTabFragment(mCurrentTag)
    }

    /**
     * 系统导航栏（手势条/三键）会叠在内容之上；给底部 Tab 区域补上 navigationBars inset。
     */
    private fun applyBottomNavWindowInsets() {
        val bottomNav = findViewById<View>(R.id.layout_bottom_nav) ?: return
        val basePaddingBottom = bottomNav.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, windowInsets ->
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                basePaddingBottom + navBars.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(bottomNav)
    }

    override fun initSystemBar(context: Context) {
        super.initSystemBar(context)
        mImmersionBar = ImmersionBar.with(this)
        // design-spec 深色主背景：状态栏使用浅色图标
        mImmersionBar?.statusBarDarkFont(false)
        mImmersionBar?.statusBarColor(com.hive.i8n.R.color.colorPrimary)
        // 导航栏与 Tab 栏同色，避免透明导航栏压住底部内容时出现断层
        mImmersionBar?.navigationBarColor(com.hive.i8n.R.color.design_bg_tab_bar)
        mImmersionBar?.navigationBarDarkFont(false)
        mImmersionBar?.init()
    }

    override fun isSupportStatusBarCompat(): Boolean {
        return false
    }

    override fun onResume() {
        super.onResume()
        refreshMainToolbar()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAgentToolbarActionsVisibility(event: AgentEvent) {
        if (event.type != AgentEventType.AGENT_CHAT_TOOLBAR_ACTIONS_VISIBILITY) return
        val visible = event.data as? Boolean ?: true
        if (showAgentToolbarActions == visible) return
        showAgentToolbarActions = visible
        if (mCurrentTag == "f2") {
            refreshToolbarAgentActions()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAgentNavigateToResourceType(event: AgentEvent) {
        if (event.type != AgentEventType.AGENT_NAVIGATE_TO_RESOURCE_TYPE) return
        val resourceType = event.data as? String ?: return
        selectFragment("f3")
        supportFragmentManager.executePendingTransactions()
        (supportFragmentManager.findFragmentByTag("f3") as? com.hive.ui.script.FragmentWorkflowPage)
            ?.navigateToResourceType(resourceType)
    }

    @Subscribe
    fun onMcpToolEvent(event: OnMcpToolEvent) {
        if (event.eventType == 1) {
            UIHandlerUtils.getInstance().post {
//                    ActivityMcpManager.start(this)
            }
        }
    }


    /**
     * 初始化tab监听器
     */
    private fun initTabListeners() {
        for (i in 0 until layout_tabs!!.childCount) {
            val view = layout_tabs?.getChildAt(i)
            if (view is TabButtonLayout) {
                view.setOnTabSelectedListener(this)
            }
        }
    }

    /**
     * tab栏被点击
     *
     * @param tabButton
     * @return
     */
    override fun onTabSelected(tabButton: TabButtonLayout): Boolean {
        val tag = tabButton.tag as String
        if (tag in mainTabTags) {
            switchMainTabFragment(tag)
        }
        return true
    }

    /**
     * 切换到指定 tag 的 Tab（供 DeepLink 等调用）。
     */
    fun selectFragment(tag: String) {
        if (tag in mainTabTags) {
            switchMainTabFragment(tag)
        }
    }

    private fun ensureMainTabFragment(tag: String): BaseFragment {
        var fragment = supportFragmentManager.findFragmentByTag(tag) as? BaseFragment
        if (fragment == null) {
            fragment = TabHelper.createFragmentByTag(tag)
            supportFragmentManager.beginTransaction()
                .add(R.id.layout_content, fragment, tag)
                .hide(fragment)
                .commitAllowingStateLoss()
            supportFragmentManager.executePendingTransactions()
        }
        return fragment
    }

    private fun switchMainTabFragment(tag: String) {
        if (tag !in mainTabTags) return
        ensureMainTabFragment(tag)
        val transaction = supportFragmentManager.beginTransaction()
        for (t in mainTabTags) {
            val f = supportFragmentManager.findFragmentByTag(t) as? BaseFragment ?: continue
            if (t == tag) {
                transaction.show(f)
            } else {
                transaction.hide(f)
            }
        }
        transaction.commitAllowingStateLoss()
        mCurrentTag = tag
        clearTabView()
        selectTabView(tag)
        refreshMainToolbar()
    }

    private fun findMainTabFragmentAt(position: Int): BaseFragment? {
        if (position !in mainTabTags.indices) return null
        return supportFragmentManager.findFragmentByTag(mainTabTags[position]) as? BaseFragment
    }

    private fun getCurrentTabFragment(): BaseFragment? {
        return supportFragmentManager.findFragmentByTag(mCurrentTag) as? BaseFragment
    }

    /**
     * 清除所有tab键；
     */
    private fun clearTabView() {
        for (i in 0 until layout_tabs!!.childCount) {
            val view = layout_tabs?.getChildAt(i)
            (view as? TabButtonLayout)?.isSelected = false
        }
    }

    /**
     * 安全地清除所有fragments
     */
    private fun clearAllFragments() {
        try {
            val transaction = supportFragmentManager.beginTransaction()
            for (fragment in supportFragmentManager.fragments) {
                if (fragment.isAdded) {
                    transaction.remove(fragment)
                }
            }
            transaction.commitAllowingStateLoss()
            supportFragmentManager.executePendingTransactions()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * 选择tab键；
     *
     * @param tag
     */
    private fun selectTabView(tag: String) {
        for (i in 0 until layout_tabs!!.childCount) {
            val view = layout_tabs?.getChildAt(i)
            if (tag == view?.tag) {
                view?.isSelected = true
                return
            }
        }
    }


    /**
     * 设置保存标识，防止重影；
     *
     * @param outState
     */
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("destroyTag", true)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layout_root?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        } else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            layout_root?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        mImmersionBar!!.destroy()
    }

    private var mExitTime: Long = 0

    //对返回键进行监听
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            if (ScriptManagerImpl.onBackPressed()) {
                return false
            }
            val currentTabFragment = getCurrentTabFragment()
            if (currentTabFragment is ITabFragment) {
                if ((currentTabFragment as ITabFragment).onBackPressed()) {
                    return false
                }
            }
            if (currentTabFragment is IBackListener) {
                if ((currentTabFragment as IBackListener).onBackPressed()) {
                    return false
                }
            }
            tryExitApp()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPermissionsChecker?.onActivityResult(requestCode, resultCode, data)
        CalendarUtils.onActivityResult(requestCode, resultCode, data)
        getCurrentTabFragment()?.onActivityResult(requestCode, resultCode, data)
    }

    private fun tryExitApp() {
        if (System.currentTimeMillis() - mExitTime > 2000) {
            CommonToast.show(getString(com.hive.i8n.R.string.sys_toast_exit_waring))
            mExitTime = System.currentTimeMillis()
        } else {
            finish()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPermissionsChecker?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        var sInstance: ActivityTab? = null
        fun start(context: Context) {
            IntentUtils.safeStartActivity(context, Intent(context, ActivityTab::class.java))
        }
    }
}

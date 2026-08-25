// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.profile

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hive.app.script.R
import com.hive.base.BaseLayout
import com.hive.framework.coper.ScriptManagerImpl
import com.hive.framework.coper.jump
import com.hive.engineer.EngineerHelper
import com.hive.timer.db.AlarmDbService
import com.hive.agent.views.provider.ActivityAgentSetting
import com.hive.ui.mcp.ActivityMcpConnectGuide
import com.hive.ui.setting.ActivityLanguages
import com.hive.ui.setting.ActivityMoreSetting
import com.hive.ui.setting.ActivityPermissionCenter
import com.hive.ui.schedule.ActivityScheduledTasks
import com.hive.utils.LanguageManager
import com.hive.utils.global.CommonUtilsWrapper
import com.hive.views.ActivitySimpleWeb

/**
 * design-spec ProfilePage（与 script-desgin ProfilePage.tsx 布局与入口一致）
 */
class ProfilePageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseLayout(context, attrs, defStyleAttr) {

    private var tvProfileAvatarInitial: TextView? = null
    private var tvProfileTitle: TextView? = null
    private var tvProfileSubtitle: TextView? = null
    private var tvRowLanguageValue: TextView? = null
    private var tvRowScheduledValue: TextView? = null
    private var tvRowPermissionValue: TextView? = null
    private var viewPermissionAlertDot: View? = null
    private var tvProfileVersion: TextView? = null
    private var tvProfileAgreementLinks: TextView? = null

    override fun getLayoutId(): Int = R.layout.profile_page_view

    override fun initView(view: View?) {
        tvProfileAvatarInitial = findViewById(R.id.tv_profile_avatar_initial)
        tvProfileTitle = findViewById(R.id.tv_profile_title)
        tvProfileSubtitle = findViewById(R.id.tv_profile_subtitle)
        tvRowLanguageValue = findViewById(R.id.tv_row_language_value)
        tvRowScheduledValue = findViewById(R.id.tv_row_scheduled_value)
        tvRowPermissionValue = findViewById(R.id.tv_row_permission_value)
        viewPermissionAlertDot = findViewById(R.id.view_permission_alert_dot)
        tvProfileVersion = findViewById(R.id.tv_profile_version)
        tvProfileAgreementLinks = findViewById(R.id.tv_profile_agreement_links)

        tvProfileTitle?.text = getString(com.hive.i8n.R.string.app_name)
        tvProfileSubtitle?.text = getString(com.hive.i8n.R.string.design_profile_local_subtitle)
        findViewById<android.widget.ImageView>(R.id.iv_profile_avatar)?.apply {
            setImageResource(com.hive.i8n.R.drawable.logo)
            visibility = View.VISIBLE
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
        tvProfileAvatarInitial?.visibility = View.GONE
        tvProfileVersion?.text = getString(
            com.hive.i8n.R.string.design_profile_version_format,
            CommonUtilsWrapper.getAppVersionName(context)
        )
        EngineerHelper.attachView(tvProfileVersion!!)

        // 协议链接点击处理
        setupAgreementLinks()

        findViewById<View>(R.id.row_language).setOnClickListener {
            ActivityLanguages.start(context)
        }
        findViewById<View>(R.id.row_model).setOnClickListener {
            ActivityAgentSetting.start(context)
        }
        findViewById<View>(R.id.row_scheduled_tasks).setOnClickListener {
            ActivityScheduledTasks.start(context)
        }
        findViewById<View>(R.id.row_permission).setOnClickListener { v ->
            v.jump(ActivityPermissionCenter::class.java)
        }
        findViewById<View>(R.id.row_more_settings).setOnClickListener {
            ActivityMoreSetting.start(context)
        }
        findViewById<View>(R.id.tv_mcp_connect_entry).setOnClickListener {
            ActivityMcpConnectGuide.start(context)
        }
        refreshAll()
    }

    fun refreshAll() {
        refreshLanguage()
        refreshScheduledTasks()
        refreshAccessibility()
    }

    private fun refreshLanguage() {
        tvRowLanguageValue?.text = LanguageManager.getLanguageDisplayName(context)
    }

    private fun refreshScheduledTasks() {
        val count = AlarmDbService.list()?.size ?: 0
        tvRowScheduledValue?.text = resources.getQuantityString(
            com.hive.i8n.R.plurals.design_profile_scheduled_tasks_count,
            count,
            count
        )
    }

    private fun refreshAccessibility() {
        val ok = ScriptManagerImpl.checkService()
        tvRowPermissionValue?.text = if (ok) {
            getString(com.hive.i8n.R.string.design_profile_permission_status_ok)
        } else {
            getString(com.hive.i8n.R.string.design_profile_permission_status_need)
        }
        val color = if (ok) {
            com.hive.i8n.R.color.design_text_tertiary
        } else {
            com.hive.i8n.R.color.design_a11y_pill_disabled_text
        }
        tvRowPermissionValue?.setTextColor(ContextCompat.getColor(context, color))
        viewPermissionAlertDot?.visibility = if (ok) GONE else VISIBLE
    }

    private fun setupAgreementLinks() {
        val tv = tvProfileAgreementLinks ?: return

        val agreementText = getString(com.hive.i8n.R.string.agreement_text)
        val privacyText = getString(com.hive.i8n.R.string.agreement_privacy)
        val separator = "  ·  "

        val fullText = "$agreementText$separator$privacyText"
        val spannable = android.text.SpannableString(fullText)

        // 用户协议可点击
        spannable.setSpan(object : android.text.style.ClickableSpan() {
            override fun onClick(widget: View) {
                openUserAgreement()
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(context, com.hive.i8n.R.color.design_text_tertiary)
            }
        }, 0, agreementText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 隐私政策可点击
        val privacyStart = agreementText.length + separator.length
        spannable.setSpan(object : android.text.style.ClickableSpan() {
            override fun onClick(widget: View) {
                openPrivacyPolicy()
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(context, com.hive.i8n.R.color.design_text_tertiary)
            }
        }, privacyStart, fullText.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tv.text = spannable
        tv.movementMethod = LinkMovementMethod.getInstance()
        tv.highlightColor = android.graphics.Color.TRANSPARENT
    }

    private fun openUserAgreement() {
        val code = CommonUtilsWrapper.getLanguage(context)
        val url = if (!code.startsWith("zh")) {
            "file:///android_asset/static/agreement_user_en.html"
        } else {
            "file:///android_asset/static/agreement_user.html"
        }
        ActivitySimpleWeb.start(context, url)
    }

    private fun openPrivacyPolicy() {
        val code = CommonUtilsWrapper.getLanguage(context)
        val url = if (!code.startsWith("zh")) {
            "file:///android_asset/static/agreement_privacy_en.html"
        } else {
            "file:///android_asset/static/agreement_privacy.html"
        }
        ActivitySimpleWeb.start(context, url)
    }

}

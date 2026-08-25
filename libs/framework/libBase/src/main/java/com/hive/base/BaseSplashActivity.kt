// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.hive.utils.GlobalApp
import com.hive.utils.global.CommonUtilsWrapper
import com.hive.views.DefaultPrivacyAgreementView
import com.hive.views.SampleDialog
import kotlin.system.exitProcess

/**
 *
 * @author jiadou
 * @date 2021/12/10
 */
abstract class BaseSplashActivity : AppCompatActivity() {
    protected var DP = 1
    private var baseApp: BaseApplication? = null

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityUtils.put(this)
        initSystemBar(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.base_splash_activity)

        if (GlobalApp.getApp() is BaseApplication) {
            baseApp = GlobalApp.getApp() as BaseApplication
            if (baseApp?.needShowPermissionDialog() == true
                && baseApp?.isPermissionGranted == false
            ) {
                showPermissionDialog()
            } else {
                startCreate()
            }
        } else {
            startCreate()
        }

    }

    private fun inflaterLayout() {
        val view = LayoutInflater.from(this).inflate(getLayoutId(), null)
        findViewById<ViewGroup>(R.id.root_layout)?.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun startCreate() {
        inflaterLayout()
        doOnCreate()
    }

    protected abstract fun doOnCreate()

    open fun initSystemBar(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            )
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            (context as Activity).window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    open fun showPermissionDialog() {
        val dialog = SampleDialog(this)
        dialog.mViewHolder.mLayoutHolder.addView(
            DefaultPrivacyAgreementView(baseContext).apply {
                setAgreementUrl(getAgreementUrl())
                setPrivacyUrl(getPrivacyUrl())
            }, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            })
        dialog.setDialogTitle(getString(com.hive.i8n.R.string.agreement_left_title))
        dialog.setDialogContent(getString(com.hive.i8n.R.string.agreement_left_content))
        dialog.setLeftText(getString(com.hive.i8n.R.string.agreement_left_text))
        dialog.setRightText(getString(com.hive.i8n.R.string.agreement_right_text))
        dialog.setOnDialogListener { isRight: Boolean ->
            if (isRight) {
                baseApp?.isPermissionGranted = true
                baseApp?.startInitApplication()
                dialog.dismiss()
//                CommonToast.show(com.hive.i8n.R.string.first_enter_main_loading)
                startCreate()
            } else {
                dialog.dismiss()
                exitProcess(-1)
            }
        }
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityUtils.remove(this)
    }

    open fun getAgreementUrl(): String {
        val code = CommonUtilsWrapper.getLanguage(
            GlobalApp.getContext()
        )
        return if (!code.startsWith("zh")) {
            "file:///android_asset/static/agreement_user_en.html"
        } else {
            "file:///android_asset/static/agreement_user.html"
        }
    }

    open fun getPrivacyUrl(): String {
        val code = CommonUtilsWrapper.getLanguage(
            GlobalApp.getContext()
        )
        return if (!code.startsWith("zh")) {
            "file:///android_asset/static/agreement_privacy_en.html"
        } else {
            "file:///android_asset/static/agreement_privacy.html"
        }
    }


    protected abstract fun getLayoutId(): Int
}
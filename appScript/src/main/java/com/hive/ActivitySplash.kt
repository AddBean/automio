// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import com.hive.app.script.R
import com.hive.base.BaseSplashActivity
import com.hive.utils.bar.ImmersionBar

/**
 *
 * @author jiadou
 * @date 3/24/21
 */
class ActivitySplash : BaseSplashActivity() {
    private var mImmersionBar: ImmersionBar? = null


    override fun doOnCreate() {
        instance = this
        jumpToMainActivity()
    }

    override fun initSystemBar(context: Context) {
        super.initSystemBar(context)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        mImmersionBar = ImmersionBar.with(this)
        mImmersionBar?.statusBarDarkFont(false)
        mImmersionBar?.statusBarColor(com.hive.i8n.R.color.colorPrimary)
        mImmersionBar?.navigationBarColor(com.hive.i8n.R.color.colorPrimary)
        mImmersionBar?.init()
    }


    private fun jumpToMainActivity() {
        val routeUri = intent?.getStringExtra(CommonIntentHandler.EXTRA_ROUTE_URI)
        val targetIntent = Intent(this, ActivityTab::class.java).apply {
            if (!routeUri.isNullOrBlank()) {
                putExtra(CommonIntentHandler.EXTRA_ROUTE_URI, routeUri)
            }
        }
        startActivity(targetIntent)
        finish()
        overridePendingTransition(
            com.hive.base.R.anim.anim_fade_in,
            com.hive.base.R.anim.anim_fade_out
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        mImmersionBar?.destroy()
    }


    override fun onBackPressed() {}


    override fun getLayoutId(): Int {
        return R.layout.welcome_activity
    }


    companion object {
        var instance: Activity? = null
    }
}

// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.hive.utils.utils.IntentUtils

class SchemeDispatchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.hive.app.script.R.layout.activity_scheme_dispatch)
        dispatch(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        dispatch(intent)
    }

    private fun dispatch(sourceIntent: Intent?) {
        val routeUri = sourceIntent?.data?.toString() ?: return
        val targetIntent = if (ActivityTab.sInstance != null) {
            Intent(this, ActivityTab::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } else {
            Intent(this, ActivitySplash::class.java)
        }.apply {
            putExtra(CommonIntentHandler.EXTRA_ROUTE_URI, routeUri)
        }
        IntentUtils.safeStartActivity(this, targetIntent)
        Handler(Looper.getMainLooper()).post {
            finish()
            overridePendingTransition(0, 0)
        }
    }
}

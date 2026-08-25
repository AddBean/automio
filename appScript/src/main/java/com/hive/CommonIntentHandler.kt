// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import com.hive.route.RouteParser
import com.hive.script.ActivityWorkflowDetail
import com.hive.utils.GlobalApp
import com.hive.utils.net.NetworkUtils
import com.hive.views.SampleDialog
import com.hive.views.ActivitySimpleWeb

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 5/15/21
 */
class CommonIntentHandler {

    fun handleIntent(activity: Activity, newIntent: Intent?) {
        if (newIntent == null) return

        handleLegacyIntent(activity, newIntent)

        val request = RouteParser.parse(newIntent.getStringExtra(EXTRA_ROUTE_URI))
            ?: RouteParser.parse(newIntent.data)
            ?: return

        when (request.page) {
            "mcp" -> handleMcpPage(activity)
            "tab" -> handleTabPage(activity, request.params["tab"])
            "workflow_detail" -> handleWorkflowDetail(activity, request.params["scriptPath"])
            "web" -> handleWebPage(activity, request.params["url"])
        }

        newIntent.removeExtra(EXTRA_ROUTE_URI)
    }

    fun handleDownloadUrl(activity: Activity, downloadUrl: String?) {
        if (downloadUrl.isNullOrBlank()) return
        if (!NetworkUtils.isWifi(activity)) {
            val dialog = SampleDialog(activity)
            dialog.setDialogTitle(GlobalApp.getString(com.hive.i8n.R.string.download_tips_title))
            dialog.setDialogContent(GlobalApp.getString(com.hive.i8n.R.string.download_tips_content))
            dialog.setLeftText(GlobalApp.getString(com.hive.i8n.R.string.cancel))
            dialog.setRightText(GlobalApp.getString(com.hive.i8n.R.string.download_btn_download))
            dialog.setOnDialogListener(object : SampleDialog.OnDialogListener {
                override fun onItemClick(isRight: Boolean) {
                    dialog.dismiss()
                    if (isRight) {
//                        DownloadHelper.getInstance().startDownload(activity, downloadUrl)
                    }
                }
            })
            dialog.show()
        } else {
//            DownloadHelper.getInstance().startDownload(activity, downloadUrl)
        }
    }

    private fun handleMcpPage(activity: Activity) {
        if (activity is ActivityTab) {
            activity.runOnUiThread {
                // AI 能力管理已迁移至工作流页面
                activity.selectFragment("f3")
            }
        }
    }

    private fun handleTabPage(activity: Activity, tab: String?) {
        if (activity is ActivityTab && !tab.isNullOrBlank()) {
            activity.runOnUiThread {
                activity.selectFragment(tab)
            }
        }
    }

    private fun handleWorkflowDetail(activity: Activity, scriptPath: String?) {
        if (scriptPath.isNullOrBlank()) return
        ActivityWorkflowDetail.start(activity, scriptPath)
    }

    private fun handleWebPage(activity: Activity, url: String?) {
        if (url.isNullOrBlank()) return
        ActivitySimpleWeb.start(activity, url)
    }

    private fun handleLegacyIntent(activity: Activity, intent: Intent) {
        val uri = intent.data ?: return
        runCatching {
            val downloadUrl = uri.getQueryParameter("downloadUrl")
            val movieId = uri.getQueryParameter("movieId")
            if (!downloadUrl.isNullOrBlank()) {
                handleDownloadUrl(activity, downloadUrl)
            }
            if (!TextUtils.isEmpty(movieId)) {
//                ActivityDetailPlayer.start(activity, movieId!!.toInt())
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE_URI = "extra_route_uri"

        val instance: CommonIntentHandler by lazy {
            CommonIntentHandler()
        }
    }
}

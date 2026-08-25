// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.hive.app.script.R
import com.hive.base.BaseFragmentActivity
import com.hive.i8n.R as i8nR
import com.hive.plugin.mcp.McpConst
import com.hive.utils.system.ClipboardUtil
import com.hive.utils.system.CommonUtils
import com.hive.utils.utils.IntentUtils
import com.hive.views.widgets.CommonToast

/**
 * 教用户如何用电脑接入本机 MCP，并提供可一键复制的配置。
 */
class ActivityMcpConnectGuide : BaseFragmentActivity() {

    override fun getLayoutId(): Int = R.layout.activity_mcp_connect_guide

    override fun doOnCreate(savedState: Bundle?) {
        val port = McpConst.StreamablePort
        val deviceIp = resolveDeviceIp()

        findViewById<TextView>(R.id.tv_step1_desc).text =
            getString(i8nR.string.mcp_connect_step1_desc, port)
        findViewById<TextView>(R.id.tv_step2_lan_desc).text =
            getString(i8nR.string.mcp_connect_step2_lan_desc, deviceIp)

        val adbReverse = "adb reverse tcp:$port tcp:$port"
        val configAdb = buildMcpConfig("127.0.0.1", port)
        val configLan = buildMcpConfig(deviceIpHost(deviceIp), port)

        bindCopyBlock(findViewById(R.id.block_adb_reverse), adbReverse)
        bindCopyBlock(findViewById(R.id.block_config_adb), configAdb)
        bindCopyBlock(findViewById(R.id.block_config_lan), configLan)
    }

    private fun resolveDeviceIp(): String {
        val ip = runCatching { CommonUtils.getIPAddress(true) }.getOrNull()
            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        return ip ?: getString(i8nR.string.mcp_connect_step2_lan_unknown)
    }

    private fun deviceIpHost(displayIp: String): String {
        val unknown = getString(i8nR.string.mcp_connect_step2_lan_unknown)
        return if (displayIp == unknown) "192.168.x.x" else displayIp
    }

    private fun buildMcpConfig(host: String, port: Int): String {
        return """
            {
              "mcpServers": {
                "automio": {
                  "url": "http://$host:$port/mcp"
                }
              }
            }
        """.trimIndent()
    }

    private fun bindCopyBlock(block: View, content: String) {
        val tvContent = block.findViewById<TextView>(R.id.tv_copy_content)
        tvContent.text = content
        val copyAction = View.OnClickListener {
            ClipboardUtil.getInstance(this).copyText("mcp_config", content)
            CommonToast.getInstance().showToast(i8nR.string.mcp_connect_copied)
        }
        block.setOnClickListener(copyAction)
        block.findViewById<View>(R.id.tv_copy_hint).setOnClickListener(copyAction)
    }

    companion object {
        fun start(context: Context) {
            IntentUtils.safeStartActivity(
                context,
                Intent(context, ActivityMcpConnectGuide::class.java)
            )
        }
    }
}

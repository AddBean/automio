// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.mcp

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.TextView
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.plugin.ComponentManager
import com.hive.plugin.mcp.McpConst
import com.hive.plugin.provider.IMcpProvider
import com.hive.utils.GlobalApp
import com.hive.utils.debug.DLog
import com.hive.utils.utils.DensityUtil
import com.hive.views.fragment.PagerHostFragment
import com.hive.views.fragment.PagerIndexHelper
import com.hive.views.fragment.PagerTag
import com.hive.views.fragment.PagerTitleScroller

/**
 * MCP 管理页面
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2024/12/19
 */
class FragmentMcpManager : PagerHostFragment<McpTitleView>(), PagerIndexHelper.OnCovertCallback {

    private var tv_service_status: TextView? = null
    private var status_indicator: View? = null
    private var mcpProvider: IMcpProvider? = null
    private var mIndexHelper: PagerIndexHelper? = null
    private var mIndexPaint: Paint? = null

    override fun getLayoutId(): Int = R.layout.fragment_mcp_manager


    override fun initFragment() {
        // 获取 MCP 服务提供者
        mcpProvider = ComponentManager.getInstance()
            .getProvider(IMcpProvider::class.java) as IMcpProvider?

        mIndexHelper = PagerIndexHelper()
        mTabFragments.clear()

        tv_service_status = view?.findViewById(R.id.tv_service_status)
        status_indicator = view?.findViewById(R.id.status_indicator)
        // 创建子Fragment并设置provider
        val toolsCustomFragment = FragmentToolCustomList()

        // 创建子Fragment并设置provider
        val toolsBuildInFragment = FragmentMcpToolList()

        // 确保子Fragment获得provider引用
        if (mcpProvider != null) {
            toolsBuildInFragment.mcpProvider = mcpProvider
        }
        mTabFragments.add(toolsCustomFragment)
        mTabFragments.add(toolsBuildInFragment)
        mTabFragments[0].setPagerTag(PagerTag(getString(com.hive.i8n.R.string.mcp_custom_tools), 0))
        mTabFragments[1].setPagerTag(
            PagerTag(
                getString(com.hive.i8n.R.string.mcp_builtin_tools),
                1
            )
        )
        notifyDataSetChanged(mTabFragments)


        mViewHolder.mViewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // 此方法在页面滑动时调用
            }

            override fun onPageSelected(position: Int) {
                // 此方法在新页面被选中时调用，主动加载数据
                if (position < mTabFragments.size) {
                    val fragment = mTabFragments[position]
                    when (fragment) {
                        is FragmentToolCustomList -> fragment.loadData()
                        is FragmentMcpToolList -> fragment.loadData()
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                // 此方法在滑动状态改变时调用
            }
        })

        initServiceStatus()

        // 初始化完成后，加载第一个页面的数据
        if (mTabFragments.isNotEmpty() && mcpProvider != null) {
            val firstFragment = mTabFragments[0]
            if (firstFragment is FragmentMcpToolList) {
                firstFragment.loadData()
            }
        }

    }

    private fun initServiceStatus() {
        updateServiceStatus()
    }

    private fun updateServiceStatus() {
        val provider = mcpProvider
        if (provider == null) {
            // 如果 provider 未初始化，显示默认状态
            tv_service_status?.text = getString(com.hive.i8n.R.string.mcp_service_not_initialized)
            status_indicator?.setBackgroundResource(R.drawable.xml_status_offline_bg)
            return
        }

        try {
            val isRunning = provider.isMcpServiceRunning()

            if (isRunning) {
                tv_service_status?.text = getString(
                    com.hive.i8n.R.string.mcp_service_running,
                    McpConst.SsePort,
                    McpConst.StreamablePort
                )
                status_indicator?.setBackgroundResource(R.drawable.xml_status_online_bg)
            } else {
                tv_service_status?.text =
                    getString(
                        com.hive.i8n.R.string.mcp_service_stopped,
                         McpConst.StreamablePort.toString()
                    )
                status_indicator?.setBackgroundResource(R.drawable.xml_status_offline_bg)
            }
        } catch (e: Exception) {
            DLog.e("FragmentMcpManager", "更新服务状态失败: ${e.message}")
            tv_service_status?.text = getString(com.hive.i8n.R.string.mcp_service_status_failed)
            status_indicator?.setBackgroundResource(R.drawable.xml_status_offline_bg)
        }
    }

    override fun onShow() {
        super.onShow()
        // 只有在 mcpProvider 已初始化时才更新状态
        if (mcpProvider != null) {
            updateServiceStatus()
        }
    }

    override fun onIndexDraw(
        scroller: PagerTitleScroller<*>?, canvas: Canvas?, position: Int,
        positionOffset: Float, positionOffsetPixels: Int
    ) {
        if (mIndexHelper != null) {
            mIndexHelper?.setPosition(scroller, canvas, position, positionOffset)
            mIndexHelper?.setCallback(this)
        }
    }

    override fun onCovertFinished(canvas: Canvas, x1: Int, y1: Int, x2: Int, y2: Int) {
        if (mIndexPaint == null) {
            DP = DensityUtil.dip2px(1.0f)
            mIndexPaint = Paint()
            mIndexPaint?.color = GlobalApp.getColor(com.hive.i8n.R.color.colorAccent)
            mIndexPaint?.strokeWidth = (1 * DP).toFloat()
            mIndexPaint?.isAntiAlias = true
            mIndexPaint?.style = Paint.Style.FILL
            mIndexPaint?.strokeCap = Paint.Cap.ROUND
        }
        val cX = x1 + (x2 - x1) / 2
        canvas.drawRoundRect(
            x1.toFloat() + 4f * DP,
            4f * DP,
            x2.toFloat() - 4f * DP,
            y1.toFloat() - 4f * DP,
            8f * DP,
            8f * DP,
            mIndexPaint!!
        )
    }
} 
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.ui.script

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.hive.app.script.R
import com.hive.i8n.R as i8nR
import com.hive.utils.GlobalApp
import com.hive.utils.system.SystemProperty
import com.hive.utils.utils.DensityUtil
import com.hive.views.fragment.PagerHostFragment
import com.hive.views.fragment.PagerIndexHelper
import com.hive.views.fragment.PagerTag
import com.hive.views.fragment.PagerTitleScroller


class FragmentScriptIndex : PagerHostFragment<ScriptScheduleTitleView>(),
    PagerIndexHelper.OnCovertCallback {

    private var mIndexHelper: PagerIndexHelper? = null
    private var mIndexPaint: Paint? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setPadding(
            0,
            SystemProperty.getStatusBarHeight(GlobalApp.getContext()),
            0, 0
        )
    }

    override fun initFragment() {
        mIndexHelper = PagerIndexHelper()
        mTabFragments.clear()
        mTabFragments.add(FragmentScriptList())
        mTabFragments.add(FragmentScriptSchedule())
        mTabFragments[0].setPagerTag(PagerTag(getStr(com.hive.i8n.R.string.script_index_title_1), 0))
        mTabFragments[1].setPagerTag(PagerTag(getStr(com.hive.i8n.R.string.script_index_title_2), 1))
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
                // 此方法在新页面被选中时调用
            }

            override fun onPageScrollStateChanged(state: Int) {
                // 此方法在滑动状态改变时调用
            }
        })
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


    override fun getLayoutId() = R.layout.fragment_script_index

}

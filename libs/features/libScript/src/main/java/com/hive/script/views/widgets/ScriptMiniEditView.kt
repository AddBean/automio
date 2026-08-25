// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.widgets

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.hive.anim.AnimUtils
import com.hive.script.R
import com.hive.script.ScriptProvider
import com.hive.utils.GlobalApp
import com.hive.utils.utils.BitmapUtils
import com.hive.utils.utils.DeviceCompatHelper
import com.hive.utils.utils.ViewUtils
import com.hive.views.widgets.AbsWindowFloatView

@SuppressLint("ViewConstructor")
class ScriptMiniEditView : AbsWindowFloatView(ScriptProvider.getViewContext(), null),
    ScriptMaxMinDialog.IScriptMiniView {

    private var bmpSnap: Bitmap? = null

    private var frameView: View? = null

    private var ivSnapshot: ImageView? = null

    private var viewMask: View? = null

    private var view = LayoutInflater.from(context).inflate(R.layout.script_mini_edit_view, this).apply {
        frameView=this.findViewById(R.id.frameView)
        ivSnapshot=this.findViewById(R.id.ivSnapshot)
        viewMask=this.findViewById(R.id.viewMask)
    }

    override fun getStartPosition(pw: Int, ph: Int): Point {
        return Point(pw - mViewWidth - 20 * DP, ph / 2 - mViewHeight / 2)
    }

    override fun setViewSnapshot(view: View?) {
        view ?: return
        bmpSnap = BitmapUtils.getViewBitmap(view)
        ivSnapshot?.setImageBitmap(bmpSnap)
        changeViewSize(bmpSnap)
    }


    private fun changeViewSize(bmp: Bitmap?) {
        bmp ?: return
        if (!DeviceCompatHelper.isLandscape()) {
            ViewUtils.setSize(
                frameView,
                90 * GlobalApp.DP,
                (90 * GlobalApp.DP * (bmp.height / bmp.width.toFloat())).toInt()
            )
        } else {
            ViewUtils.setSize(
                frameView,
                160 * GlobalApp.DP,
                (160 * GlobalApp.DP * (bmp.height / bmp.width.toFloat())).toInt()
            )
        }
    }

    /**
     *警告动画
     */
    fun startWarningAnim() {
        viewMask?.isSelected = true
        AnimUtils.fadeOutAnim(viewMask, 1000L, object : AnimUtils.AnimListener() {
            override fun onOver(v: View?) {
                super.onOver(v)
                viewMask?.alpha = 1f
                viewMask?.isSelected = false
            }
        })
    }

}
// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.hive.base.BaseFragmentActivity


class FloatDialogActivity : BaseFragmentActivity() {
    private var ivCancel: View? = null
    private var tvSlide: View? = null
    private var tvSwitch: View? = null

    override fun doOnCreate(p0: Bundle?) {
        ivCancel = findViewById(R.id.ivCancel)
        tvSlide = findViewById(R.id.tvSlide)
        tvSwitch = findViewById(R.id.tvSwitch)
        window.decorView.post {
            ObjectAnimator.ofFloat(tvSlide, "translationX", 0f, tvSwitch!!.width.toFloat() / 2)
                .apply {
                    duration = 1300
                    repeatMode = ValueAnimator.RESTART
                    repeatCount = ValueAnimator.INFINITE
                }.start()
        }
        ivCancel?.setOnClickListener {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onStart() {
        super.onStart()
        window?.let {
            it.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            it.setGravity(Gravity.BOTTOM)
            it.setDimAmount(0f)
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.xml_guid_dialog_view
    }

}
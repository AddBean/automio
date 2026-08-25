// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.views

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import androidx.fragment.app.DialogFragment
import com.hive.libfiles.R

import com.hive.utils.system.SystemProperty

/**
 *
 * @author jiadou
 * @date 4/9/21
 */
abstract class XFileStyleDialog : DialogFragment() {

    private var mRootView: View? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, com.hive.base.R.style.BottomDialogAnimation)
    }


    fun initThemeStyle() {
        //设置宽度顶满屏幕,无左右留白
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        //设置背景色透明,在style中已设置backgroundDimEnabled为false,这里不需要.
        val window = dialog?.window
        val windowParams = window!!.attributes
        windowParams.dimAmount = 0.0f
        window.attributes = windowParams
        window.setBackgroundDrawableResource(com.hive.i8n.R.color.transparent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            )
//            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        //再次设置出现动画
        window.attributes.windowAnimations = com.hive.base.R.style.BottomDialogAnimation
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (mRootView == null) mRootView = inflater.inflate(R.layout.x_file_menu_dialog, null)
        mRootView?.setOnClickListener { v: View? -> dismissAllowingStateLoss() }
        mRootView!!.findViewById<ViewGroup>(R.id.menu_container_layout)
            .addView(inflater.inflate(getLayoutResId(), null))
        return mRootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        if (checkDeviceHasNavigationBar(requireContext())) {
//            view.setPadding(0, 0, 0, StatusBarCompat.getNavigationBarHeight(context))
//        }
        initThemeStyle()
        super.onViewCreated(view, savedInstanceState)
        dialog?.setOnKeyListener { dialog, keyCode, event ->
            if (event?.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                onBackPressed()
            } else {
                false
            }
        }
    }

    open fun checkDeviceHasNavigationBar(context: Context): Boolean {
        val defaultDisplay =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        //屏幕实际高度
        val realPoint = Point()
        defaultDisplay.getRealSize(realPoint)
        //屏幕显示高度
        val outMetrics = DisplayMetrics()
        defaultDisplay.getMetrics(outMetrics)
        //虚拟底部导航高度
        val navigationBarHeight: Int = SystemProperty.getNavigationBarHeight(context)
        return outMetrics.heightPixels + navigationBarHeight <= realPoint.y
    }

    protected abstract fun getLayoutResId(): Int

    open fun onBackPressed(): Boolean = false

}
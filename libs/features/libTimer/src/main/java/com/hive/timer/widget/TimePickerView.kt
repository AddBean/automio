// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.timer.widget

import android.content.Context
import android.graphics.Color
import android.os.Vibrator
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hive.base.BaseLayout
import com.hive.timer.R
import com.hive.utils.utils.ViewUtils
import com.hive.views.widgets.wheel.WheelView
import com.hive.views.widgets.wheel.adapters.ArrayWheelAdapter

/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 7/19/21
 */
class TimePickerView(context: Context?, attrs: AttributeSet?) : BaseLayout(context, attrs) {

    private lateinit var mBackColor: IntArray
    private lateinit var mHourList: MutableList<Int>
    private lateinit var mMinList: MutableList<Int>
    private lateinit var mSecList: MutableList<Int>
    private var mainColor = Color.WHITE
    private var textColor = Color.BLACK
    private val vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    var onTimeChangedListener: OnTimeChangedListener? = null

    private var wheel_1: WheelView? = null
    private var wheel_2: WheelView? = null
    private var wheel_3: WheelView? = null

    override fun initAttrs(attrs: AttributeSet?) {
        attrs?.run {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.TimePickerView
            )
            val count = a.indexCount
            for (i in 0 until count) {
                when (val attr = a.getIndex(i)) {
                    R.styleable.TimePickerView_timePickerColor -> {
                        mainColor = a.getColor(attr, Color.WHITE)
                    }

                    R.styleable.TimePickerView_timePickerTextColor -> {
                        textColor = a.getColor(attr, Color.BLACK)
                    }
                }

            }
            a.recycle()
            initView(this@TimePickerView)
        }
    }

    override fun initView(view: View?) {
        wheel_1= findViewById(R.id.wheel_1)
        wheel_2 = findViewById(R.id.wheel_2)
        wheel_3 = findViewById(R.id.wheel_3)
        mHourList = mutableListOf()
        mMinList = mutableListOf()
        mSecList = mutableListOf()

        mBackColor = intArrayOf(
            (0xff000000 or ((0x00ffffff.toLong() and mainColor.toLong()))).toInt(),
            (0xef000000 or ((0x00ffffff.toLong() and mainColor.toLong()))).toInt(),
            (0xbf000000 or ((0x00ffffff.toLong() and mainColor.toLong()))).toInt(),
            0x000000,
            0x000000
        )

        for (i in 0..23) {
            mHourList.add(i)
        }
        for (i in 0..59) {
            mMinList.add(i)
        }
        for (i in 0..59) {
            mSecList.add(i)
        }

        wheel_1?.viewAdapter = Adapter(context, mHourList.toTypedArray()).apply {
            itemResource = R.layout.timer_text_view
            itemTextResource = R.id.tv_number
        }
        wheel_1?.setShadowColor(mBackColor)
        wheel_1?.setWheelForeground(R.drawable.xml_transparent)
        wheel_1?.setWheelBackground(R.drawable.xml_white)
        wheel_1?.setDrawCenterRect(false)
        wheel_1?.isCyclic = true
        wheel_1?.addChangingListener { _, _, _ ->
            vibrator.vibrate(10L)
            notifyTimeChanged()
        }

        wheel_2?.viewAdapter = Adapter(context, mMinList.toTypedArray()).apply {
            itemResource = R.layout.timer_text_view
            itemTextResource = R.id.tv_number
        }
        wheel_2?.setShadowColor(mBackColor)
        wheel_2?.setWheelForeground(R.drawable.xml_transparent)
        wheel_2?.setWheelBackground(R.drawable.xml_white)
        wheel_2?.setDrawCenterRect(false)
        wheel_2?.isCyclic = true
        wheel_2?.addChangingListener { _, _, _ ->
            vibrator.vibrate(10L)
            notifyTimeChanged()
        }

        wheel_3?.viewAdapter = Adapter(context, mSecList.toTypedArray()).apply {
            itemResource = R.layout.timer_text_view
            itemTextResource = R.id.tv_number
        }
        wheel_3?.setShadowColor(mBackColor)
        wheel_3?.setWheelForeground(R.drawable.xml_transparent)
        wheel_3?.setWheelBackground(R.drawable.xml_white)
        wheel_3?.setDrawCenterRect(false)
        wheel_3?.isCyclic = true
        wheel_3?.addChangingListener { _, _, _ ->
            vibrator.vibrate(10L)
            notifyTimeChanged()
        }

        ViewUtils.traverseViewTree(this) {
            if (it is TextView) {
                it.setTextColor(textColor)
            }
        }
    }

    private fun notifyTimeChanged() {
        onTimeChangedListener?.onTimeChanged(
            mHourList[wheel_1!!.currentItem],
            mMinList[wheel_2!!.currentItem],
            mSecList[wheel_3!!.currentItem]
        )
    }

    fun getSelectedTime(): Triple<Int, Int, Int> {
        return Triple(
            mHourList.getOrNull(wheel_1?.currentItem ?: 0) ?: 0,
            mMinList.getOrNull(wheel_2?.currentItem ?: 0) ?: 0,
            mSecList.getOrNull(wheel_3?.currentItem ?: 0) ?: 0
        )
    }

    fun setSelectedTime(hour: Int, minute: Int, second: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val safeSecond = second.coerceIn(0, 59)
        wheel_1?.currentItem = safeHour
        wheel_2?.currentItem = safeMinute
        wheel_3?.currentItem = safeSecond
        notifyTimeChanged()
    }

    inner class Adapter(context: Context?, val items: Array<out Int>) :
        ArrayWheelAdapter<Int>(context, items) {
        override fun getItemText(index: Int) =
            if (items[index] < 10) "0${items[index]}" else "${items[index]}"
    }

    interface OnTimeChangedListener {
        fun onTimeChanged(hour: Int, minus: Int, secs: Int)
    }

    override fun getLayoutId() = R.layout.time_picker_view
}

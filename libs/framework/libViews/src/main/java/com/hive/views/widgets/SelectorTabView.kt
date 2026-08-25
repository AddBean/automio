// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.hive.utils.GlobalApp
import com.hive.utils.extends.dpi
import com.hive.views.R

/**
 *
 * @author jiadou
 * @date 6/30/21
 */
class SelectorTabView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var mTabBackground = R.drawable.xml_selector_round_tab_item_view

    private var mTextColor: ColorStateList? = getColorStateList(R.drawable.xml_selector_round_tab_item_view_color)

    private var mCurrentValue: String? = null

    private val dp = GlobalApp.DP

    var onTabSelectedChangedListener: OnTabSelectedChangedListener? = null

    var valueList = mutableListOf<Pair<String?, String?>>()

    init {
        orientation = HORIZONTAL
        initAttrs(attrs)
    }

    fun initAttrs(attrs: AttributeSet?) {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.SelectorTabView)
            mCurrentValue = ta.getString(R.styleable.SelectorTabView_tabDefaultValue)
            mTextColor = ta.getColorStateList(R.styleable.SelectorTabView_tabTxtColor)
            mTabBackground = ta.getResourceId(R.styleable.SelectorTabView_tabBackground, R.drawable.xml_selector_round_tab_item_view)
            val ls1 = ta.getString(R.styleable.SelectorTabView_tabNameList)?.split(",")?.toMutableList()
            val ls2 = ta.getString(R.styleable.SelectorTabView_tabValueList)?.split(",")?.toMutableList()
            ls1?.run {
                for (i in ls1!!.indices) {
                    valueList.add(ls1[i] to (ls2?.get(i) ?: ""))
                }
                updateUi()
            }
            ta.recycle()
        }
    }

    fun setValue(value: String) {
        mCurrentValue = value
        updateUi()
    }

    private fun updateUi() {
        this.removeAllViews()
        valueList.forEach {
            val item = ItemView()
            item.setBackgroundResource(mTabBackground)
            this.addView(item, FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setPadding(3 * dp, 3 * dp, 3 * dp, 3 * dp)
            })
            item.isSelected = it.second == mCurrentValue
            item.bindData(it)
        }
    }

    fun getColorStateList(redId: Int): ColorStateList? {
        try {
            val csl = ResourcesCompat.getColorStateList(resources, redId, null);
            return csl
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null;
    }

    interface OnTabSelectedChangedListener {
        fun onSelectedChanged(p: Pair<String?, String?>?)
    }


    private fun onItemSelected(data: Pair<String?, String?>) {
        mCurrentValue = data.second
        updateUi()
        onTabSelectedChangedListener?.onSelectedChanged(data)
    }


    inner class ItemView : androidx.appcompat.widget.AppCompatTextView(context) {

        lateinit var mData: Pair<String?, String?>

        init {
            textSize = 12f
            setPadding(8.5f.dpi(), 6.5f.dpi(), 8.5f.dpi(), 6.5f.dpi())
            gravity = Gravity.CENTER
            if (mTextColor != null)
                setTextColor(mTextColor)
            setOnClickListener {
                onItemSelected(mData)
            }
        }

        fun bindData(it: Pair<String?, String?>) {
            mData = it
            text = mData.first
            typeface = if (!this.isSelected) Typeface.DEFAULT else Typeface.DEFAULT_BOLD
        }
    }
}
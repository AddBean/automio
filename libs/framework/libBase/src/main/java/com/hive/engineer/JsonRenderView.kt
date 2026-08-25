// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.engineer

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.hive.base.BaseLayout
import com.hive.base.R
import com.hive.extension.visibleOrGone
import com.hive.global.GlobalConfigModel
import com.hive.utils.utils.GsonHelper
import com.hive.views.widgets.CommonToast

/**
 *
 * @author jiadou
 * @date 2021/12/10
 */
class JsonRenderView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    var maxLines: Int = 3
        set(value) {
            field = value
            updateConfig()
        }

    val text: String
        get() = GlobalConfigModel.read()?.toString() ?: ""

    init {
        orientation = VERTICAL
    }

    fun updateConfig() {
        removeAllViews()
        val config = GlobalConfigModel.read()
        config?.data?.sortBy { it.key }
        if (config?.data != null) {
            for (i in config.data.indices) {
                addView(ItemView().bindData(config.data[i]).visibleOrGone(i < maxLines))
            }
        }
    }

    inner class ItemView : BaseLayout(context),
        OnClickListener, OnLongClickListener {

        var itemData: GlobalConfigModel.ConfigListBean? = null

        var tv_title: TextView? = null
        var tv_content: TextView? = null

        override fun initView(view: View?) {
            tv_title = findViewById(R.id.tv_title)
            tv_content = findViewById(R.id.tv_content)
            tv_title?.setOnClickListener(this)
            tv_content?.isSelected = false
            tv_content?.setOnLongClickListener(this)
            tv_content?.visibleOrGone(tv_content?.isSelected == true)
        }

        fun bindData(itemData: GlobalConfigModel.ConfigListBean?): ItemView {
            this.itemData = itemData
            tv_title?.text = itemData?.key ?: ""
            tv_content?.text = GsonHelper.toFormatJsonString(itemData?.value.toString())
            return this
        }

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.tv_title -> {
                    tv_content?.isSelected = tv_content?.isSelected == false
                    tv_content?.visibleOrGone(tv_content?.isSelected == true)
                }
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val cm = context.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
            cm.text = GsonHelper.toFormatJsonString(itemData?.value.toString())
            CommonToast.show(context.getString(com.hive.i8n.R.string.base_copied_to_clipboard))
            return true
        }

        override fun getLayoutId() = R.layout.json_render_view_item_view


    }


}
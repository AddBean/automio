// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.cards

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.hive.files.model.FileCardData
import com.hive.files.utils.XAppInfoParser
import com.hive.script.R
import com.hive.utils.utils.RelativeDateFormat
import com.hive.utils.utils.StringUtils
import com.hive.views.list_view.ListRecyclerItemView
import java.util.Date

/**
 * @author jiadou
 * @date 4/7/21
 */
open class ScriptAppFileCard(context: Context) : ListRecyclerItemView(context) {

    private val view = LayoutInflater.from(context).inflate(R.layout.script_app_file_card, this)

    var mAppInfo: XAppInfoParser.AppInfo? = null
    private var iv_icon: ImageView? = null
    private var tv_info: TextView? = null
    private var tv_name: TextView? = null
    private var tv_pkg: TextView? = null
    private var tv_time: TextView? = null

    override fun bindData(data: Any?) {
        val fileData = data as FileCardData
        mAppInfo = fileData.cardData as XAppInfoParser.AppInfo?
        iv_icon = view.findViewById(R.id.iv_icon)
        tv_info = view.findViewById(R.id.tv_info)
        tv_name = view.findViewById(R.id.tv_name)
        tv_pkg = view.findViewById(R.id.tv_pkg)
        tv_time = view.findViewById(R.id.tv_time)

        iv_icon?.setImageDrawable(mAppInfo?.icon)
        tv_name?.text = mAppInfo?.appName
        tv_info?.text = StringUtils.byte2XB(fileData.fileSize)
        tv_time?.text = RelativeDateFormat.format(Date(fileData.lastModified))
        tv_pkg?.text = mAppInfo?.packageName
        if (!TextUtils.isEmpty(fileData.searchData)) {
            StringUtils.setSpanningText(tv_name, fileData.searchData)
        }
    }
}
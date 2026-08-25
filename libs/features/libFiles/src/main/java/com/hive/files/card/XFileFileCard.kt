// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.card

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hive.files.model.FileCardData
import com.hive.files.utils.XImageLoader
import com.hive.libfiles.R
import com.hive.utils.utils.RelativeDateFormat
import com.hive.utils.utils.StringUtils
import java.util.*

/**
 * @author jiadou
 * @date 4/7/21
 */
open class XFileFileCard(context: Context) : XFileBaseCard(context) {


    override fun bindFileData(fileData: FileCardData) {
        val iv_icon: ImageView? = findViewById(R.id.iv_icon)
        val tv_name: TextView? = findViewById(R.id.tv_name)
        val tv_info: TextView? = findViewById(R.id.tv_info)
        val tv_time: TextView? = findViewById(R.id.tv_time)
        XImageLoader.load(iv_icon!!, fileData)
        tv_name?.text = fileData.fileName
        tv_info?.text = StringUtils.byte2XB(fileData.fileSize)
        tv_time?.text = RelativeDateFormat.format(Date(fileData.lastModified))
        if (!TextUtils.isEmpty(fileData.searchData)) {
            StringUtils.setSpanningText(tv_name, fileData.searchData)
        }
    }


    override fun onEditModelChanged(editModel: Boolean) {
        switch_check?.visibility = if (editModel) View.VISIBLE else View.GONE
    }

    override fun onUpdateSelectStatus(selected: Boolean) {
        switch_check?.switchStatus = selected
    }

    override fun getLayoutId(): Int = R.layout.x_file_file_card
}
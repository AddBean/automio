// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.card

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.hive.files.model.FileCardData
import com.hive.files.utils.XImageLoader
import com.hive.libfiles.R

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/7/21
 */
open class XFileFolderCard2(context: Context) : XFileBaseCard(context) {

    override fun bindFileData(fileData: FileCardData) {
        val iv_icon: ImageView = findViewById(R.id.iv_icon)
        val tv_name: TextView = findViewById(R.id.tv_name)
        val tv_info: TextView = findViewById(R.id.tv_info)
        val tv_time: TextView = findViewById(R.id.tv_time)
        fileData.cardData?.run {
            var firstData = this as FileCardData?
            if (firstData?.isVideo() == true || firstData?.isImage() == true) {
                XImageLoader.loadImage(iv_icon, firstData)
            } else {
                iv_icon.setImageResource(R.drawable.file_icon_folder)
            }
        }
        tv_name?.text = fileData.fileName
        tv_info?.text = context.getString(com.hive.i8n.R.string.x_file_item_count, fileData.subFileCount)

    }

    override fun onEditModelChanged(editModel: Boolean) {
        switch_check?.visibility = if (editModel) View.VISIBLE else View.GONE
    }

    override fun onUpdateSelectStatus(selected: Boolean) {
        switch_check?.switchStatus = selected
    }

    override fun getLayoutId(): Int = R.layout.x_file_folder_card2

}
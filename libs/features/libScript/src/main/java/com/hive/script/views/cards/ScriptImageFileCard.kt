// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.views.cards

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.hive.extension.visibleOrGone
import com.hive.net.image.GlideApp
import com.hive.script.R
import com.hive.script.net.data.ScriptImageBean
import com.hive.script.views.dialog.DialogImageManager
import com.hive.views.list_view.ListRecyclerItemView

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/7/21
 */
open class ScriptImageFileCard(context: Context, var selector: DialogImageManager) :
    ListRecyclerItemView(context) {

    private var imageBean: ScriptImageBean? = null


    private val view =
        LayoutInflater.from(context).inflate(R.layout.script_image_file_card, this).apply {
            setOnClickListener {
                if (imageBean?.type == 0) {
                    postEvent(ImageType.PREVIEW)
                } else {
                    postEvent(ImageType.ADD_IMAGE)
                }
            }
            this.findViewById<View>(R.id.iv_checkbox)?.setOnClickListener {
                postEvent(ImageType.SELECTED)
            }
        }

    override fun bindData(data: Any?) {
        imageBean = data as ScriptImageBean

        val iv_icon = view.findViewById<ImageView>(R.id.iv_icon)
        val fl_container = view.findViewById<View>(R.id.fl_container)
        val iv_checkbox = view.findViewById<View>(R.id.iv_checkbox)
        val iv_plus = view.findViewById<View>(R.id.iv_plus)
        GlideApp.with(context).load(imageBean?.path).dontAnimate()
            .into<DrawableImageViewTarget>(DrawableImageViewTarget(iv_icon))
        iv_checkbox?.visibleOrGone(selector.selectorMode || selector.editorSelectMode)
        iv_checkbox?.isSelected =
            selector.selectedImages.find { it.path == imageBean?.path } != null
        iv_plus?.visibleOrGone(imageBean?.type == -1)
        fl_container?.visibleOrGone(imageBean?.type == 0)
    }

    enum class ImageType {
        SELECTED, ADD_IMAGE, PREVIEW
    }

}
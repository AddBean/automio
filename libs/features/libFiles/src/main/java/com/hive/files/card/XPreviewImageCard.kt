// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.card

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.hive.files.model.FileCardData
import com.hive.files.utils.XImageLoader
import com.hive.largeimg.DismissFrameLayout
import com.hive.largeimg.LargeImageLoader
import com.hive.largeimg.PhotoView
import com.hive.libfiles.R
import com.hive.utils.utils.ScreenUtils

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/7/21
 */
class XPreviewImageCard(context: Context) : XFileBaseCard(context),
    DismissFrameLayout.OnDismissListener {
    private var imageLoader = LargeImageLoader(context)
    private val mRequestOptions: RequestOptions =
        RequestOptions().diskCacheStrategy(DiskCacheStrategy.DATA).skipMemoryCache(true)
            .placeholder(
                ColorDrawable(
                    Color.TRANSPARENT
                )
            )

    private var layout_dismiss: DismissFrameLayout? = null

    private var photo_view: PhotoView? = null


    override fun initView(view: View?) {
        layout_dismiss = view?.findViewById(R.id.layout_dismiss)
        photo_view = view?.findViewById(R.id.photo_view)
        layout_dismiss?.setDismissListener(this)
        photo_view?.setOnClickListener {
            postEvent(1)
        }
    }

    override fun bindFileData(fileData: FileCardData) {
        var orientation = XImageLoader.getImageOrientation(fileData.filePath);
        photo_view?.orientation = orientation + fileData.orientation
        imageLoader.load(
            photo_view!!.context,
            photo_view!!,
            fileData.filePath,
            mRequestOptions,
            object : LargeImageLoader.IPhotonImageLoadCall {
                override fun onLoadErr(url: String?, isFromCache: Boolean, errInfo: String?) {
                    photo_view?.setImageResource(R.drawable.file_load_fail)
                }

                override fun onLoadSucc(url: String?, isFromCache: Boolean) {
                }
            })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpecNew =
            MeasureSpec.makeMeasureSpec(ScreenUtils.getScreenWidth(), MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpecNew, heightMeasureSpec)
    }

    override fun onEditModelChanged(editModel: Boolean) {

    }

    override fun onUpdateSelectStatus(selected: Boolean) {

    }

    override fun getLayoutId(): Int = R.layout.x_preview_image_card

    override fun onViewDismiss(anim: Boolean, isFling: Boolean) {
        var cxt = context
        if (cxt is Activity) {
            cxt.finish()
        }
    }

    override fun onCancel() {
        var cxt = context
        if (cxt is Activity) {
            cxt.findViewById<View>(android.R.id.content).alpha = 1f
        }
    }

    override fun onDoubleClick() {

    }

    override fun onScaleProgress(scale: Float) {
        var cxt = context
        if (cxt is Activity) {
            cxt.findViewById<View>(android.R.id.content).alpha = 1f - scale
        }
    }

    override fun onPhotoTap(isInPhoto: Boolean) {
        postEvent(1)
    }

}
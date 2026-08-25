// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils

import android.widget.ImageView
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.gif.GifOptions
import com.bumptech.glide.request.RequestOptions
import com.hive.net.NetHelper
import com.hive.net.image.ImageLoader
import com.hive.utils.debug.DLog

fun ImageView.loadUrl(url: String?) {
    url ?: return
    if (url.endsWith("gif", true)) {
        ImageLoader.getInstance().loadImage(
            this.context, this, NetHelper.covertRes(url), RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .set(GifOptions.DISABLE_ANIMATION, false)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
        )
    } else {
        ImageLoader.getInstance()
            .loadImage(this.context, this, NetHelper.covertRes(url))
    }
}

fun ImageView.loadUrl(url: String?, defaultRes: Int) {
    url ?: return
    DLog.d("load image url", "url = $url")
    if (url.endsWith("gif", true)) {
        ImageLoader.getInstance().loadImage(
            this.context, this, NetHelper.covertRes(url), RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .set(GifOptions.DISABLE_ANIMATION, false)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.DATA).placeholder(defaultRes)
        )
    } else {
        ImageLoader.getInstance()
            .loadImage(this.context, this, NetHelper.covertRes(url), defaultRes)
    }

}

fun ImageView.loadLocal(url: String?) {
    url ?: return
    ImageLoader.getInstance().loadImage(this.context, this, url)
}
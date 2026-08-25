// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files.utils

import android.app.Activity
import android.media.ExifInterface
import android.widget.ImageView
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hive.files.XFileUtils
import com.hive.files.model.FileCardData
import com.hive.files.model.XFileSetting
import com.hive.libfiles.R
import com.hive.net.image.GlideApp
import com.hive.net.image.ImageLoader
import com.hive.utils.file.MediaFileUtil
import com.hive.utils.utils.ColorUtils


/**
 *
 * @author jiadou
 * @email 172111432@qq.com
 * @date 4/8/21
 */
object XImageLoader {

    fun loadCover(iv: ImageView, f: FileCardData?) {
        f?.run {
            var context = iv.context
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            loadImage(iv, f);
        }
    }


    fun loadImage(iv: ImageView, f: FileCardData?) {
        f?.run {
            var context = iv.context
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            val requests = if (context is Activity) {
                GlideApp.with((context as Activity?)!!)
            } else {
                GlideApp.with(context)
            }
            requests.load(f.newFile())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .priority(Priority.IMMEDIATE)
                    .dontAnimate()
                    .thumbnail(0.2f)
                    .centerCrop()
                    .placeholder(ColorUtils.getRandomColorDrawableByUrl(f.filePath))
                    .into(iv)
        }
    }

    fun loadIcon(iv: ImageView, url: String?) {
        iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
        when {

            url?.startsWith("magnet:") == true -> {
                iv?.setImageResource(XFileUtils.getFileResId(MediaFileUtil.FILE_TYPE_TORRENT))
            }
            MediaFileUtil.isImageFileType(url) -> {
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                ImageLoader.getInstance().loadImage(iv.context, iv, url)
            }
            else -> {
                iv?.setImageResource(XFileUtils.getFileResId(url!!))
            }
        }

    }

    fun loadVideoLargeCover(iv: ImageView, f: FileCardData?) {
        f?.run {

            if (XFileSetting.instance.showThumb) {
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                loadImage(iv, f)
            } else {
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setImageResource(R.drawable.video_large_place_holder)
            }
        }
    }

    fun load(iv: ImageView, f: FileCardData?) {
        f?.run {
            var context = iv.context
            when {
                isVideo() && XFileSetting.instance.showThumb -> {
                    iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    iv?.setImageResource(XFileUtils.getFileResId(f.filePath))
                }
                isImage() -> {
                    iv.scaleType = ImageView.ScaleType.CENTER_CROP
                    val requests = if (context is Activity) {
                        GlideApp.with((context as Activity?)!!)
                    } else {
                        GlideApp.with(context)
                    }
                    requests.load(f.newFile())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .priority(Priority.IMMEDIATE)
                            .dontAnimate()
                            .thumbnail(0.5f)
                            .override(512, 512)
                            .centerCrop()
                            .placeholder(ColorUtils.getRandomColorDrawableByUrl(f.filePath))
                            .into(iv)
                }
                else -> {
                    iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    iv?.setImageResource(XFileUtils.getFileResId(f.filePath))
                }
            }
        }
    }

    fun getImageOrientation(path: String): Int {
        val exifInterface = ExifInterface(path)
        val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}
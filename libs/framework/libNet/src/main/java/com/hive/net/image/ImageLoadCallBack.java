// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.lang.ref.WeakReference;

/**
 * 图片回调二次封装
 * Created by kuaigeng01 on 2018/5/18.
 */
public abstract class ImageLoadCallBack extends SimpleTarget<Bitmap> {
    public ImageLoadCallBack() {
    }

    public ImageLoadCallBack(int width, int height) {
        super(width, height);
    }

    @Override
    public final void onLoadStarted(@Nullable Drawable placeholder) {
        onImageLoadStart();
    }

    @Override
    public final void onLoadFailed(@Nullable Drawable errorDrawable) {
        onImageLoadFinish(null);
    }

    @Override
    public final void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
        onImageLoadFinish(bitmap);
    }

    public void onImageLoadStart() {

    }

    public void onImageLoadFinish(@Nullable Bitmap bitmap) {

    }

    public static class ImageloaderListener extends ImageLoadCallBack {

        private WeakReference<ImageView> mImageView;


        public ImageloaderListener(ImageView imageView) {
            mImageView = new WeakReference<ImageView>(imageView);
        }

        @Override
        public void onImageLoadFinish(@Nullable Bitmap bitmap) {
            super.onImageLoadFinish(bitmap);
            if (mImageView != null && mImageView.get() != null && bitmap != null) {
                mImageView.get().setImageBitmap(bitmap);
            }
        }
    }
}

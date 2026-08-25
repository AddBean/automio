// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import java.io.File;


/**
 * 图片显示加载器
 * Created by kuaigeng01 on 2018/2/26.
 */
public class ImageLoader implements IImageDisplay {

    private GlideImageLoader impl;

    private ImageLoader() {
        impl = new GlideImageLoader();
    }

    private static ImageLoader imageLoader;

    public static GlideImageLoader getInstance() {
        if (null == imageLoader) {
            synchronized (ImageLoader.class) {
                if (null == imageLoader) {
                    imageLoader = new ImageLoader();
                }
            }
        }

        return imageLoader.impl;
    }

    @Override
    public void onTrimMemory(Context context, int level) {
        impl.onTrimMemory(context, level);
    }

    @Override
    public void onLowMemory(Context context) {
        impl.onLowMemory(context);
    }

    @Override
    public void loadImage(Activity activity, ImageView view, String imgUrl, int placeholder) {
        impl.loadImage(activity, view, imgUrl, placeholder);
    }

    @Override
    public void loadImage(Activity activity, ImageView view, String imgUrl) {
        impl.loadImage(activity, view, imgUrl);
    }

    @Override
    public void loadImageNoAnim(Context context, ImageView view, String imgUrl) {
        impl.loadImageNoAnim(context, view, imgUrl);
    }

    @Override
    public void loadImage(Context context, ImageView view, String imgUrl) {
        impl.loadImage(context, view, imgUrl);
    }

    @Override
    public void loadImage(Activity activity, ImageView view, int resId) {
        impl.loadImage(activity, view, resId);
    }

    @Override
    public void loadImage(Context context, ImageView view, int resId) {
        impl.loadImage(context, view, resId);
    }

    @Override
    public void loadImageTo8888(Context context, ImageView view, String imgUrl, int placeholder) {
        impl.loadImageTo8888(context, view, imgUrl, placeholder);
    }

    @Override
    public void loadImageNoCache8888(Context context, ImageView view, String imgUrl) {
        impl.loadImageNoCache8888(context, view, imgUrl);
    }

    @Override
    public void loadImageNoAnim8888(Context context, ImageView view, String imgUrl) {
        impl.loadImageNoAnim8888(context, view, imgUrl);
    }

    @Override
    public void loadImageTo8888(Activity activity, ImageView view, String imgUrl, int placeholder) {
        impl.loadImageTo8888(activity, view, imgUrl, placeholder);
    }

    @Override
    public void loadImageTo8888(Activity activity, ImageView view, String imgUrl) {
        impl.loadImageTo8888(activity, view, imgUrl);
    }

    @Override
    public void loadImageTo8888(Context context, ImageView view, String imgUrl) {
        impl.loadImageTo8888(context, view, imgUrl);
    }

    @Override
    public void loadImageTo8888(Activity activity, ImageView view, int resId) {
        impl.loadImageTo8888(activity, view, resId);
    }

    @Override
    public void loadImageTo8888(Context context, ImageView view, int resId) {
        impl.loadImageTo8888(context, view, resId);
    }

    @Override
    public void loadImage(Context context, ImageView view, String imgUrl, int placeholder) {
        impl.loadImage(context, view, imgUrl, placeholder);
    }

    @Override
    public void loadImageNoCache(Context context, ImageView view, String imgUrl) {
        impl.loadImageNoCache(context, view, imgUrl);
    }

    @Override
    public Bitmap loadImageSync(Activity activity, String imgUrl) {
        return impl.loadImageSync(activity, imgUrl);
    }

    @Override
    public Bitmap loadImageSync(Context context, String imgUrl) {
        return impl.loadImageSync(context, imgUrl);
    }

    @Override
    public void loadImageAsync(Activity activity, String imgUrl, ImageLoadCallBack callBack) {
        impl.loadImageAsync(activity, imgUrl, callBack);
    }

    @Override
    public void loadImageAsync(Context context, String imgUrl, ImageLoadCallBack callBack) {
        impl.loadImageAsync(context, imgUrl, callBack);
    }

    @Override
    public Bitmap loadImageFromCache(Context context, String imgUrl) {
        return impl.loadImageFromCache(context, imgUrl);
    }

    @Override
    public void preCache(Activity activity, String imgUrl) {
        impl.preCache(activity, imgUrl);
    }

    @Override
    public void preCache(Context context, String imgUrl) {
        impl.preCache(context, imgUrl);
    }

    @Override
    public void cancelPreCacheTask() {
        impl.cancelPreCacheTask();
    }

    @Override
    public File getDiskCache(Context context) {
        return impl.getDiskCache(context);
    }

    @Override
    public boolean isFileExistInDiskCache(Context context, String imgUrl) {
        return impl.isFileExistInDiskCache(context, imgUrl);
    }

    @Override
    public void clearMemory(Context context) {
        impl.clearMemory(context);
    }

    @Override
    public void clearDiskCache(Context context) {
        impl.clearDiskCache(context);
    }

    @Override
    public void resumeImageLoader(Context context) {
        impl.resumeImageLoader(context);
    }

    @Override
    public void pauseImageLoader(Context context) {
        impl.pauseImageLoader(context);
    }
}

// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import java.io.File;

/**
 * 图片显示
 * Created by kuaigeng01 on 2018/2/26.
 */
interface IImageDisplay {

    /**
     * 内存紧张
     *
     * @param context 上下文
     * @param level   紧张等级
     */
    void onTrimMemory(Context context, int level);

    /**
     * 内存紧张
     *
     * @param context 上下文
     */
    void onLowMemory(Context context);

    /**
     * 加载图片
     *
     * @param context     全局上下文
     * @param view        图片View
     * @param imgUrl      图片加载地址
     * @param placeholder 占位图
     */
    void loadImage(Context context, ImageView view, String imgUrl, int placeholder);

    void loadImageNoCache(Context context, ImageView view, String imgUrl);

    void loadImageNoAnim(Context context, ImageView view, String imgUrl);

    void loadImage(Activity activity, ImageView view, String imgUrl, int placeholder);

    void loadImage(Activity activity, ImageView view, String imgUrl);

    void loadImage(Context context, ImageView view, String imgUrl);

    void loadImage(Activity activity, ImageView view, int redId);

    void loadImage(Context context, ImageView view, int redId);




    void loadImageTo8888(Context context, ImageView view, String imgUrl, int placeholder);

    void loadImageNoCache8888(Context context, ImageView view, String imgUrl);

    void loadImageNoAnim8888(Context context, ImageView view, String imgUrl);

    void loadImageTo8888(Activity activity, ImageView view, String imgUrl, int placeholder);

    void loadImageTo8888(Activity activity, ImageView view, String imgUrl);

    void loadImageTo8888(Context context, ImageView view, String imgUrl);

    void loadImageTo8888(Activity activity, ImageView view, int redId);

    void loadImageTo8888(Context context, ImageView view, int redId);





    /**
     * 同步加载图片获取bitmap
     *
     * @param context 上下文
     * @param imgUrl  图片加载地址
     */
    Bitmap loadImageSync(Context context, String imgUrl);

    Bitmap loadImageSync(Activity activity, String imgUrl);

    /**
     * 恢复图片加载
     *
     * @param context 上下文
     */
    void resumeImageLoader(Context context);

    /**
     * 暂停图片加载
     *
     * @param context 上下文
     */
    void pauseImageLoader(Context context);

    /**
     * 异步加载图片获取bitmap
     *
     * @param activity 上下文
     * @param imgUrl   图片地址
     * @param callBack 加载结果回调
     */
    void loadImageAsync(Activity activity, String imgUrl, ImageLoadCallBack callBack);


    void loadImageAsync(Context context, String imgUrl, ImageLoadCallBack callBack);

    /**
     * 从缓存中获取图片
     *
     * @param context 上下文
     * @param imgUrl  图片地址
     * @return bitmap
     */
    Bitmap loadImageFromCache(Context context, String imgUrl);

    /**
     * 异步缓存图片
     *
     * @param activity 上下文
     * @param imgUrl   图片地址
     */
    void preCache(Activity activity, String imgUrl);

    /**
     * 预加载
     *
     * @param context 上下文
     * @param imgUrl  图片地址
     */
    void preCache(Context context, String imgUrl);

    /**
     * 取消预缓存
     */
    void cancelPreCacheTask();

    /**
     * 检查缓存文件是否存在
     *
     * @param context 上下文
     * @param imgUrl  图片地址
     * @return true or false
     */
    boolean isFileExistInDiskCache(Context context, String imgUrl);

    File getDiskCache(Context context);

    /**
     * 清除缓存
     *
     * @param context 上下文
     */
    void clearMemory(Context context);

    /**
     * 清除磁盘内容
     *
     * @param context 上下文
     */
    void clearDiskCache(Context context);


}

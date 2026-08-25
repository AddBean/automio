// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import androidx.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ViewTarget;
import com.hive.net.R;
import com.hive.utils.debug.DLog;

import java.io.InputStream;

import okhttp3.OkHttpClient;

@GlideModule
public class BbAppGlideModule extends AppGlideModule {

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setLogLevel(Log.WARN);

//======================================================
        int memoryCacheSizeBytes = calculateMemoryCacheSize(context); // 20mb
        builder.setMemoryCache(new LruResourceCache(memoryCacheSizeBytes));

//        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
//                .setMemoryCacheScreens(2)
//                .build();
//        builder.setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()));


//======================================================
////        int bitmapPoolSizeBytes = 1024 * 1024 * 30; // 30mb
////        builder.setBitmapPool(new LruBitmapPool(bitmapPoolSizeBytes));
//
//        calculator = new MemorySizeCalculator.Builder(context)
//                .setBitmapPoolScreens(3)
//                .build();
//        builder.setBitmapPool(new LruBitmapPool(calculator.getBitmapPoolSize()));


//======================================================
        int diskCacheSizeBytes = 1024 * 1024 * 200;  //200 MB
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, diskCacheSizeBytes));


//======================================================
        RequestOptions defaultOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .dontAnimate()
                .format(DecodeFormat.PREFER_RGB_565);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultOptions = defaultOptions.disallowHardwareConfig();
        }

        builder.setDefaultRequestOptions(defaultOptions);

        ViewTarget.setTagId(R.id.id_glide_image_target);

        super.applyOptions(context, builder);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        /* GIFs - 使用 Glide 内置的 GIF 解码器，不再需要自定义解码器 */
        DLog.e("registerComponents","_________registerComponents_________");
        // 移除自定义 GIF 解码器，使用 Glide 内置的 GIF 支持
        // Glide 4.x 已经内置了 GIF 解码器，会自动处理 GIF 文件

        OkHttpClient client = new OkHttpClient.Builder().build();
        registry.replace(GlideUrl.class, InputStream.class,
                new OkHttpUrlLoader.Factory(client));
//        super.registerComponents(context, glide, registry);
    }

    private static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            memoryClass = am.getLargeMemoryClass();
        }
        // Target ~15% of the available heap.
        int byteSize = 1024 * 1024 * memoryClass / 7;
        if (DLog.isDebug()) {
            System.out.println("memoryCache before calculate " + byteSize);
        }

        int max = 15 * 1024 * 1024;// 限制最大 15M
        byteSize = Math.min(max, byteSize);
        if (DLog.isDebug()) {
            System.out.println("memoryCache after calculate " + byteSize);
        }
        return byteSize;
    }
}

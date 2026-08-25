// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import android.content.Context;

import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;
import com.hive.utils.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;
import okhttp3.Cache;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

public class CacheManager {

    /**
     * 只清理网络缓存
     *
     * @param listener
     */
    public static void clearNetworkCache(final OnCacheListener listener) {
        Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> emitter) throws Exception {
                File cacheFile = new File(GlobalApp.sContext.getCacheDir().toString(), "http_cache");
                FileUtils.clearDir(cacheFile);
                emitter.onNext(-1L);
                emitter.onComplete();
            }
        }).compose(RxTransformer.io_main).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long value) throws Exception {
                if (listener != null)
                    listener.onFinish(value);
            }
        });

    }

    /**
     * 只清理图片缓存
     *
     * @param listener
     */
    public static void clearImageCache(final OnCacheListener listener) {
        Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> emitter) throws Exception {
                File cacheFile = new File(GlobalApp.sContext.getCacheDir().toString(), "image_manager_disk_cache");
                FileUtils.clearDir(cacheFile);
                emitter.onNext(-1L);
                emitter.onComplete();
            }
        }).compose(RxTransformer.io_main).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long value) throws Exception {
                if (listener != null)
                    listener.onFinish(value);
            }
        });

    }

    /**
     * 清理所有数据
     *
     * @param listener
     */
    public static void clearAllCache(final Context context, final OnCacheListener listener) {
        Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> emitter) throws Exception {
                File cacheFile = new File("/data/data/" + context.getPackageName() + "/");
                FileUtils.clearDir(cacheFile);
                emitter.onNext(-1L);
                emitter.onComplete();
            }
        }).compose(RxTransformer.io_main).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long value) throws Exception {
                if (listener != null)
                    listener.onFinish(value);
            }
        });

    }

    /**
     * 清理所有缓存
     *
     * @param listener
     */
    public static void clearCache(final OnCacheListener listener) {
        Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> emitter) throws Exception {
                File cacheFile = new File(GlobalApp.sContext.getCacheDir().toString());
                FileUtils.clearDir(cacheFile);
                emitter.onNext(-1L);
                emitter.onComplete();
            }
        }).compose(RxTransformer.io_main).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long value) throws Exception {
                if (listener != null)
                    listener.onFinish(value);
            }
        });

    }

    /**
     * 获取缓存大小
     *
     * @param listener
     */
    public static void getCacheSize(final OnCacheListener listener) {
        Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> emitter) throws Exception {
                File cacheFile = new File(GlobalApp.sContext.getCacheDir().toString());
                emitter.onNext(FileUtils.getFileSize(cacheFile));
                emitter.onComplete();
            }
        }).compose(RxTransformer.io_main).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long value) throws Exception {
                if (listener != null)
                    listener.onFinish(value);
            }
        });

    }


    public static Cache newHttpCache() {
        File cacheFile = new File(GlobalApp.sContext.getCacheDir().toString(), "http_cache");
        int cacheSize = 10 * 1024 * 1024;
        Cache cache = new Cache(cacheFile, cacheSize);
        return cache;
    }

    public static Cache getHttpCache() {
        return ApiClientCreator.getHttpCache();
    }

    public interface OnCacheListener {
        void onFinish(long value);
    }
}

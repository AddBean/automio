// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.largeimg;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.rastermillv2.FrameSequence;
import android.support.rastermillv2.FrameSequenceDrawable;
import android.support.rastermillv2.ImageTypeHelper;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.hive.image.R;
import com.hive.utils.thread.PriorityThreadFactory;
import com.hive.utils.thread.UIHandlerUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 加载大图处理
 */
public class LargeImageLoader {

    private static final String TAG_NAME = "photoView";
    private final Context mContext;
    private ThreadPoolExecutor mLoadImgExecutor;
    private final Object initLock = new Object();
    private final WeakHashMap<String, Drawable> mCacheImg = new WeakHashMap<>();
    private final HashMap<String, String> mFileType = new HashMap<>();
    private FrameSequenceDrawable.BitmapProvider mBitmapProvider;
    private Map<String, Boolean> mIsLoadingFlag = new ConcurrentHashMap<>();

    public LargeImageLoader(final Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * @param activity
     * @param imageView
     * @param url
     * @param requestOptions
     * @param call
     */
    public void load(@NonNull Context activity, @NonNull ImageView imageView, @NonNull String url, @Nullable RequestOptions requestOptions, IPhotonImageLoadCall call) {
        if (imageView == null || url == null) {
            if (call != null) {
                call.onLoadErr(url, false, "param err");
            }
            return;
        }
        Drawable drawable = imageView.getDrawable();
        if (drawable != null && drawable instanceof FrameSequenceDrawable) {
            ((FrameSequenceDrawable) drawable).stop();
        }

        if (mLoadImgExecutor == null) {
            mLoadImgExecutor = new ThreadPoolExecutor(2, 5, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory(android.os.Process.THREAD_PRIORITY_BACKGROUND, TAG_NAME));
        }
        if (requestOptions.getPlaceholderDrawable() != null) {
            imageView.setImageDrawable(requestOptions.getPlaceholderDrawable());
        }
        mLoadImgExecutor.execute(new LoadImageTask(activity, imageView, url, call, requestOptions));
    }


    /**
     * 是否资源
     */
    public void onDestroy() {
        if (mCacheImg != null) {
            try {
                for (String key : mCacheImg.keySet()) {
                    if (!TextUtils.isEmpty(key)) {
                        Drawable drawable = mCacheImg.get(key);
                        if (drawable != null && drawable instanceof FrameSequenceDrawable) {
                            ((FrameSequenceDrawable) drawable).stop();
                            ((FrameSequenceDrawable) drawable).destroy();
                        }
                    }
                }
            } catch (Throwable t) {

            }
            mCacheImg.clear();
        }
        if (mLoadImgExecutor != null) {
            mLoadImgExecutor.shutdown();
        }
        if (mFileType != null) {
            mFileType.clear();
        }
        if (mIsLoadingFlag != null) {
            mIsLoadingFlag.clear();
        }
    }

    public class LoadImageTask implements Runnable {
        private WeakReference<Context> mParentActivity;
        private WeakReference<ImageView> mTargetImg;
        private WeakReference<IPhotonImageLoadCall> mImageCall;

        private RequestOptions mRequestOptions;
        private String mImgUrl;


        public LoadImageTask(Context activity, ImageView imageView, String url, IPhotonImageLoadCall call, RequestOptions requestOptions) {
            imageView.setTag(R.id.id_photo_id_image_target, url);
            mParentActivity = new WeakReference<>(activity);
            mTargetImg = new WeakReference<>(imageView);
            if (call != null) {
                mImageCall = new WeakReference<>(call);
            }
            mRequestOptions = requestOptions;
            mImgUrl = url;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public void run() {
            final Context parent = mParentActivity.get();
            if (parent == null || isDestroy(parent)) {
                return;
            }
            final ImageView targetImg = mTargetImg.get();
            if (targetImg == null) {
                return;
            }
            final IPhotonImageLoadCall call = mImageCall != null ? mImageCall.get() : null;
            File cacheFile = null;
            if (mImgUrl != null && mImgUrl.startsWith(ImageSource.FILE_SCHEME)) {
                cacheFile = new File(mImgUrl.substring(ImageSource.FILE_SCHEME.length() - 1));
            } else {
                FutureTarget future = Glide.with(parent).
                        load(mImgUrl).
                        apply(mRequestOptions).
                        downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                try {
                    cacheFile = (File) future.get();
                } catch (InterruptedException e) {
                } catch (ExecutionException e) {
                }
            }

            if (cacheFile != null && cacheFile.isFile()) {
                mIsLoadingFlag.put(mImgUrl, Boolean.FALSE);
                showImage(parent, targetImg, mImgUrl, cacheFile, true, call);
            }
        }

        private void showImage(final Context parent, ImageView targetImg, String url, File saveFile, boolean isFromCache, IPhotonImageLoadCall call) {
            FileInputStream fileInputStream = null;
            try {
                FrameSequenceDrawable cacheDrawable = getFromCache(parent, url);
                if (cacheDrawable != null) {
                    final FrameSequenceDrawable frameSequenceDrawable = cacheDrawable;
                    if (TextUtils.equals(url, (CharSequence) targetImg.getTag(R.id.id_photo_id_image_target))) {
                        postUiShowImage(targetImg, null, frameSequenceDrawable, isFromCache, call);
                    }
                    return;
                }
                boolean isAnimalImg = isAnimalImage(saveFile);
                if (isAnimalImg) {
                    fileInputStream = new FileInputStream(saveFile);
                    FrameSequence frameSequence = FrameSequence.decodeStream(fileInputStream);
                    final FrameSequenceDrawable frameSequenceDrawable = new FrameSequenceDrawable(frameSequence, mBitmapProvider);
                    if (TextUtils.equals(url, (CharSequence) targetImg.getTag(R.id.id_photo_id_image_target))) {
                        postUiShowImage(targetImg, saveFile, frameSequenceDrawable, isFromCache, call);
                    }
                    if (frameSequenceDrawable != null) {
                        synchronized (initLock) {
                            mCacheImg.put(url, frameSequenceDrawable);
                        }
                        mFileType.put(url, "gif");
                    }
                } else {
                    postUiShowImage(targetImg, saveFile, null, isFromCache, call);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void postUiShowImage(final ImageView targetImage, final File saveFile, final Drawable drawable, final boolean isFromCache, final IPhotonImageLoadCall call) {
            UIHandlerUtils.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    if (call != null) {
                        call.onLoadSucc(mImgUrl, isFromCache);
                    }
                    if (drawable != null) {
                        targetImage.setImageDrawable(drawable);
                    } else if (targetImage != null && targetImage instanceof PhotoView && saveFile != null) {
                        ((PhotoView) targetImage).setImage(ImageSource.uri(saveFile.getAbsolutePath()));
                    }
                }
            });
        }

    }

    private FrameSequenceDrawable getFromCache(final Context parent, String url) {
        FrameSequenceDrawable cacheDrawable;
        synchronized (initLock) {
            cacheDrawable = (FrameSequenceDrawable) mCacheImg.get(url);
            if (mBitmapProvider == null) {
                mBitmapProvider = new FrameSequenceDrawable.BitmapProvider() {
                    @Override
                    public Bitmap acquireBitmap(int minWidth, int minHeight) {
                        return Glide.get(parent).getBitmapPool().get(minWidth, minHeight, Bitmap.Config.ARGB_8888);
                    }

                    @Override
                    public void releaseBitmap(Bitmap bitmap) {
                        Glide.get(parent).getBitmapPool().put(bitmap);
                    }
                };
            }
        }
        return cacheDrawable;
    }

    private boolean isAnimalImage(File saveFile) {
        if (TextUtils.equals(mFileType.get(String.valueOf(saveFile.hashCode())), "gif") || ImageTypeHelper.isAnimalImage(mContext, saveFile)) {
            mFileType.put(String.valueOf(saveFile.hashCode()), "gif");
            return true;
        }
        return false;
    }

    private static boolean isDestroy(Context parent) {
        if (parent != null && parent instanceof Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return ((Activity) parent).isDestroyed() || ((Activity) parent).isFinishing();
            }
            return ((Activity) parent).isFinishing();
        }
        return false;
    }

    /**
     * 加载图片回调
     */
    public static interface IPhotonImageLoadCall {
        /**
         * 加载图片成功
         *
         * @param url
         * @param isFromCache
         */
        void onLoadSucc(String url, boolean isFromCache);

        /**
         * 加载图片失败
         *
         * @param url
         * @param isFromCache
         */
        void onLoadErr(String url, boolean isFromCache, String errInfo);
    }
}

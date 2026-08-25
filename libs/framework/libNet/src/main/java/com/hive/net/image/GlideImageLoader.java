// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.image;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.SafeKeyGenerator;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.EmptySignature;
import com.hive.utils.debug.DLog;
import com.hive.utils.thread.ThreadPools;
import com.hive.utils.thread.UIHandlerUtils;
import com.hive.utils.utils.ColorUtils;

import java.io.File;

public class GlideImageLoader implements IImageDisplay {

    public static boolean autoAdjustView = true;

    @Override
    public void onTrimMemory(Context context, int level) {
        if (null == context) {
            return;
        }

        try {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                GlideApp.get(context).clearMemory();
            } else {
                GlideApp.get(context).onTrimMemory(level);
            }
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    @Override
    public void onLowMemory(Context context) {
        if (null == context) {
            return;
        }

        try {
            GlideApp.get(context).onLowMemory();
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    @Override
    public void loadImage(Activity activity, ImageView view, String imgUrl, int placeholder) {
        if (!isValidContextForGlide(activity)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideApp.with(activity).load(imgUrl).placeholder(placeholder).into(new DrawableImageViewTarget(view));
    }

    @Override
    public void loadImage(Activity activity, ImageView view, String imgUrl) {
        if (!isValidContextForGlide(activity)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        Drawable drawable = ColorUtils.getRandomColorDrawableByUrl(imgUrl);
        if (autoAdjustView) {
            view.post(() -> {
                drawable.setBounds(0, 0, view.getWidth(), view.getHeight());
                GlideApp.with(activity).load(imgUrl).placeholder(drawable).into(new DrawableImageViewTarget(view));
            });
        } else {
            GlideApp.with(activity).load(imgUrl).placeholder(drawable).into(new DrawableImageViewTarget(view));
        }
    }

    @Override
    public void loadImageNoAnim(Context context, ImageView view, String imgUrl) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        Drawable drawable = ColorUtils.getRandomColorDrawableByUrl(imgUrl);
        if (autoAdjustView) {
            view.post(() -> {
                drawable.setBounds(0, 0, view.getWidth(), view.getHeight());
                GlideApp.with(context).load(imgUrl).placeholder(drawable).dontAnimate().into(new DrawableImageViewTarget(view));
            });
        } else {
            GlideApp.with(context).load(imgUrl).placeholder(drawable).dontAnimate().into(new DrawableImageViewTarget(view));
        }
    }


    @Override
    public void loadImage(Context context, ImageView view, String imgUrl, int placeholder) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideRequests requests;
        if (context instanceof Activity) {
            requests = GlideApp.with((Activity) context);
        } else {
            requests = GlideApp.with(context);
        }

        requests.load(imgUrl).placeholder(placeholder).transition(DrawableTransitionOptions.withCrossFade()).into(new DrawableImageViewTarget(view));
    }

    @Override
    public void loadImageNoCache(Context context, ImageView view, String imgUrl) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideRequests requests;
        if (context instanceof Activity) {
            requests = GlideApp.with((Activity) context);
        } else {
            requests = GlideApp.with(context);
        }

        requests.load(imgUrl).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(new DrawableImageViewTarget(view));
    }


    @Override
    public void loadImage(Context context, ImageView view, String imgUrl) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideRequests requests;
        if (context instanceof Activity) {
            requests = GlideApp.with((Activity) context);
        } else {
            requests = GlideApp.with(context);
        }
        Drawable drawable = ColorUtils.getRandomColorDrawableByUrl(imgUrl);
        if (autoAdjustView) {
            view.post(() -> {
                drawable.setBounds(0, 0, view.getWidth(), view.getHeight());
                requests.load(imgUrl)
                        .placeholder(drawable)
                        .into(new DrawableImageViewTarget(view));
            });
        } else {
            requests.load(imgUrl)
                    .placeholder(drawable)
                    .into(new DrawableImageViewTarget(view));
        }
    }

    @Override
    public void loadImage(Activity activity, ImageView view, int redId) {
        if (!isValidContextForGlide(activity)) {
            return;
        }

        GlideApp.with(activity).load(redId).into(new DrawableImageViewTarget(view));
    }

    @Override
    public void loadImage(Context context, ImageView view, int redId) {
        if (!isValidContextForGlide(context)) {
            return;
        }

        GlideRequests requests;
        if (context instanceof Activity) {
            requests = GlideApp.with((Activity) context);
        } else {
            requests = GlideApp.with(context);
        }
        requests.load(redId).into(view);

    }

    @Override
    public void loadImageTo8888(Activity activity, ImageView view, String imgUrl, int placeholder) {
        if (!isValidContextForGlide(activity)) {
            return;
        }
        GlideOptions options = new GlideOptions().format(DecodeFormat.PREFER_ARGB_8888);
        GlideApp.with(activity).load(imgUrl)
                .placeholder(placeholder)
                .apply(options).apply(options).into(new DrawableImageViewTarget(view));
    }

    @Override
    public void loadImageTo8888(Activity activity, ImageView view, String imgUrl) {
        if (!isValidContextForGlide(activity)) {
            return;
        }
        GlideOptions options = new GlideOptions().format(DecodeFormat.PREFER_ARGB_8888);
        DLog.d("loadImage", "url = " + imgUrl);
        Drawable drawable = ColorUtils.getRandomColorDrawableByUrl(imgUrl);
        if (autoAdjustView) {
            view.post(() -> {
                drawable.setBounds(0, 0, view.getWidth(), view.getHeight());
                GlideApp.with(activity).load(imgUrl).apply(options).placeholder(drawable).into(new DrawableImageViewTarget(view));

            });
        } else {
            GlideApp.with(activity).load(imgUrl).apply(options).placeholder(drawable).into(new DrawableImageViewTarget(view));

        }

    }

    @Override
    public void loadImageNoAnim8888(Context context, ImageView view, String imgUrl) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideOptions options = new GlideOptions().format(DecodeFormat.PREFER_ARGB_8888);
        Drawable drawable = ColorUtils.getRandomColorDrawableByUrl(imgUrl);
        if (autoAdjustView) {
            view.post(() -> {
                drawable.setBounds(0, 0, view.getWidth(), view.getHeight());
                GlideApp.with(context).load(imgUrl).apply(options).placeholder(drawable).dontAnimate().into(new DrawableImageViewTarget(view));
            });
        } else {
            GlideApp.with(context).load(imgUrl).apply(options).placeholder(drawable).dontAnimate().into(new DrawableImageViewTarget(view));
        }
    }


    @Override
    public void loadImageTo8888(Context context, ImageView view, String imgUrl, int placeholder) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideOptions options = new GlideOptions().format(DecodeFormat.PREFER_ARGB_8888);
        GlideRequests requests;
        if (context instanceof Activity) {
            requests = GlideApp.with((Activity) context);
        } else {
            requests = GlideApp.with(context);
        }

        requests.load(imgUrl).apply(options).placeholder(placeholder).transition(DrawableTransitionOptions.withCrossFade()).into(new DrawableImageViewTarget(view));
    }

    @Override
    public void loadImageNoCache8888(Context context, ImageView view, String imgUrl) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideOptions options = new GlideOptions().format(DecodeFormat.PREFER_ARGB_8888);
        GlideRequests requests;
        if (context instanceof Activity) {
            requests = GlideApp.with((Activity) context);
        } else {
            requests = GlideApp.with(context);
        }

        requests.load(imgUrl).apply(options).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(new DrawableImageViewTarget(view));
    }


    @Override
    public void loadImageTo8888(Context context, ImageView view, String imgUrl) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideOptions options = new GlideOptions().format(DecodeFormat.PREFER_ARGB_8888);
        GlideRequests requests;
        if (context instanceof Activity) {
            requests = GlideApp.with((Activity) context);
        } else {
            requests = GlideApp.with(context);
        }

        Drawable drawable = ColorUtils.getRandomColorDrawableByUrl(imgUrl);
        if (autoAdjustView) {
            view.post(() -> {
                drawable.setBounds(0, 0, view.getWidth(), view.getHeight());
                requests.load(imgUrl)
                        .placeholder(drawable)
                        .apply(options)
                        .into(new DrawableImageViewTarget(view));
            });
        } else {
            requests.load(imgUrl)
                    .placeholder(drawable)
                    .apply(options)
                    .into(new DrawableImageViewTarget(view));
        }

    }

    @Override
    public void loadImageTo8888(Activity activity, ImageView view, int redId) {
        if (!isValidContextForGlide(activity)) {
            return;
        }
        GlideOptions options = new GlideOptions().format(DecodeFormat.PREFER_ARGB_8888);
        GlideApp.with(activity).load(redId).apply(options).into(new DrawableImageViewTarget(view));
    }

    @Override
    public void loadImageTo8888(Context context, ImageView view, int redId) {
        if (!isValidContextForGlide(context)) {
            return;
        }
        GlideOptions options = new GlideOptions().format(DecodeFormat.PREFER_ARGB_8888);
        GlideRequests requests;
        if (context instanceof Activity) {
            requests = GlideApp.with((Activity) context);
        } else {
            requests = GlideApp.with(context);
        }
        requests.load(redId).apply(options).into(view);

    }


    @Override
    public Bitmap loadImageSync(Activity activity, String imgUrl) {
        if (!isValidContextForGlide(activity)) {
            return null;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        FutureTarget<Bitmap> bitmapFutureTarget = GlideApp.with(activity).asBitmap().load(imgUrl).submit();
        try {
            return bitmapFutureTarget.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Bitmap loadImageSync(Fragment fragment, String imgUrl) {
        if (!isValidContextForGlide(fragment == null ? null : fragment.getActivity())) {
            return null;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        FutureTarget<Bitmap> bitmapFutureTarget = GlideApp.with(fragment).asBitmap().load(imgUrl).submit();
        try {
            return bitmapFutureTarget.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Bitmap loadImageSync(Context context, String imgUrl) {
        if (!isValidContextForGlide(context)) {
            return null;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        FutureTarget<Bitmap> bitmapFutureTarget = GlideApp.with(context).asBitmap().load(imgUrl).submit();
        try {
            return bitmapFutureTarget.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Bitmap loadImageSync(@NonNull Context context, @NonNull String imgUrl, @NonNull RequestOptions options) {
        if (!isValidContextForGlide(context)) {
            return null;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideRequests glideRequests;
        if (context instanceof Activity) {
            glideRequests = GlideApp.with((Activity) context);
        } else {
            glideRequests = GlideApp.with(context);
        }

        FutureTarget<Bitmap> bitmapFutureTarget = glideRequests.asBitmap().apply(options).load(imgUrl).submit();
        try {
            return bitmapFutureTarget.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void loadImageAsync(Activity activity, String imgUrl, ImageLoadCallBack callBack) {
        if (!isValidContextForGlide(activity)) {
            if (null != callBack) {
                callBack.onImageLoadFinish(null);
            }
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideApp.with(activity).asBitmap().load(imgUrl).into(callBack);
    }

    public void loadImageAsync(Fragment fragment, String imgUrl, ImageLoadCallBack callBack) {
        if (!isValidContextForGlide(fragment == null ? null : fragment.getActivity())) {
            return;
        }

        GlideApp.with(fragment).asBitmap().load(imgUrl).into(callBack);
    }

    @Override
    public void loadImageAsync(Context context, String imgUrl, ImageLoadCallBack callBack) {
        if (!isValidContextForGlide(context)) {
            if (null != callBack) {
                callBack.onImageLoadFinish(null);
            }
            return;
        }
        DLog.d("loadImage", "url = " + imgUrl);
        GlideApp.with(context).asBitmap().load(imgUrl).into(callBack);
    }

    @Override
    public void preCache(Activity activity, String imgUrl) {
        if (!isValidContextForGlide(activity)) {
            return;
        }
        DLog.d("loadImage", "preCache url = " + imgUrl);
        GlideApp.with(activity).load(imgUrl).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.DATA).priority(Priority.LOW).preload();
    }


    @Override
    public void preCache(Context context, String imgUrl) {
        if (null == context) {
            return;
        }
        DLog.d("loadImage", "preCache url = " + imgUrl);
        GlideApp.with(context).load(imgUrl).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.DATA).priority(Priority.LOW).preload();
    }


    @Override
    public File getDiskCache(Context context) {

        return Glide.getPhotoCacheDir(context);
    }

    @Override
    public void clearMemory(final Context context) {
        if (null == context) {
            return;
        }

        UIHandlerUtils.getInstance().executeInMainThread(new Runnable() {
            @Override
            public void run() {
                GlideApp.get(context).clearMemory();
            }
        });
    }

    @Override
    public void clearDiskCache(final Context context) {
        if (null == context) {
            return;
        }

        ThreadPools.getInstance().post(new Runnable() {
            @Override
            public void run() {
                GlideApp.get(context).clearDiskCache();
            }
        });
    }


    @Override
    public void resumeImageLoader(Context context) {
        if (isValidContextForGlide(context)) {
            GlideApp.with(context).resumeRequests();
        }
    }

    @Override
    public void pauseImageLoader(Context context) {
        if (null != context) {
            GlideApp.with(context).pauseRequests();
        }
    }


    @Override
    public void cancelPreCacheTask() {
        //TODO
    }

    @Override
    public boolean isFileExistInDiskCache(Context context, String imgUrl) {
        BbDataCacheKey dataCacheKey = new BbDataCacheKey(new GlideUrl(imgUrl), EmptySignature.obtain());
        SafeKeyGenerator safeKeyGenerator = new SafeKeyGenerator();
        String safeKey = safeKeyGenerator.getSafeKey(dataCacheKey);

//        Log.e("ImageKey", safeKey);
//        filePath = safeKey + ".0"  >>>>  9901a8e2be384999e0c73a68ba882c0caf0821ce4b6a7d3e471ef7f669fc535b.0

        String filePath = safeKey + ".0";
        File imgFile = new File(filePath);
        return imgFile.exists() && imgFile.isFile();
    }


    @Override
    public Bitmap loadImageFromCache(Context context, String imgUrl) {
        if (!isValidContextForGlide(context)) {
            return null;
        }

        FutureTarget<Bitmap> futureTarget = GlideApp.with(context).asBitmap().onlyRetrieveFromCache(true).load(imgUrl).submit();
        try {
            return futureTarget.get();
        } catch (Exception e) {
            //ignore
        }

        return null;
    }

    //=============================================================================
    //Glide 特有
    //=============================================================================
    public static boolean isValidContextForGlide(final Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;

            if (Build.VERSION.SDK_INT >= 17) {
                if (activity.isDestroyed() || activity.isFinishing()) {
                    return false;
                }
            } else {
                if (activity.isFinishing()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void preCache(Fragment fragment, String imgUrl) {
        if (!isValidContextForGlide(fragment == null ? null : fragment.getActivity())) {
            return;
        }

        GlideApp.with(fragment).load(imgUrl).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.DATA).priority(Priority.LOW).preload();
    }


    public void loadImage(Fragment fragment, ImageView view, String imgUrl, int placeholder) {
        if (!isValidContextForGlide(fragment == null ? null : fragment.getActivity())) {
            return;
        }

        GlideApp.with(fragment).load(imgUrl).placeholder(placeholder).into(new DrawableImageViewTarget(view));
    }

    public void loadImage(Fragment fragment, @NonNull ImageView imageView, String imgUrl, RequestOptions options) {
        if (!isValidContextForGlide(fragment == null ? null : fragment.getActivity())) {
            return;
        }

        GlideApp.with(fragment).load(imgUrl).apply(options).into(new DrawableImageViewTarget(imageView));
    }

    public void loadImage(Context context, @NonNull ImageView imageView, String imgUrl, RequestOptions options) {
        if (!isValidContextForGlide(context)) {
            return;
        }

        if (context instanceof Activity) {
            GlideApp.with((Activity) context).load(imgUrl).apply(options).into(new DrawableImageViewTarget(imageView));
        } else {
            GlideApp.with(context).load(imgUrl).apply(options).into(new DrawableImageViewTarget(imageView));
        }
    }

    public void loadImage(Activity activity, @NonNull ImageView imageView, String imgUrl, RequestOptions options) {
        if (!isValidContextForGlide(activity)) {
            return;
        }

        GlideApp.with(activity).load(imgUrl).apply(options).into(new DrawableImageViewTarget(imageView));
    }


    public void loadImage(Context context, @NonNull ImageView imageView, String imgUrl, RequestOptions options, RequestListener<Drawable> listener) {
        if (!isValidContextForGlide(context)) {
            return;
        }

        if (context instanceof Activity) {
            GlideApp.with((Activity) context).load(imgUrl).apply(options).listener(listener).into(new DrawableImageViewTarget(imageView));
        } else {
            GlideApp.with(context).load(imgUrl).apply(options).listener(listener).into(new DrawableImageViewTarget(imageView));
        }
    }

    public void loadImage(Context context, @NonNull ImageView imageView, Drawable drawable, String imgUrl, RequestListener<Drawable> listener) {
        if (!isValidContextForGlide(context)) {
            return;
        }

        if (context instanceof Activity) {
            GlideApp.with((Activity) context).load(imgUrl).listener(listener).into(new DrawableImageViewTarget(imageView));
        } else {
            GlideApp.with(context).load(imgUrl).placeholder(drawable).listener(listener).into(new DrawableImageViewTarget(imageView));
        }
    }

    public void loadImage(Activity activity, @NonNull ImageView imageView, String imgUrl, RequestOptions options, RequestListener<Drawable> listener) {
        if (!isValidContextForGlide(activity)) {
            return;
        }

        GlideApp.with(activity).load(imgUrl).apply(options).listener(listener).into(new DrawableImageViewTarget(imageView));
    }


    public void clearView(@NonNull ImageView imageView) {
        if (null == imageView) {
            return;
        }

        Context context = imageView.getContext();

        if (!isValidContextForGlide(context)) {
            return;
        }

        GlideApp.with(context).clear(imageView);
    }

    /************************************Glide加载字节流********************************************/
//    private static void loadImage(Context context, String imageUrl, int resourceId, ImageView imageView) {
//        loadImage(GlideApp.with(context), imageUrl, resourceId, imageView);
//    }
//
//    private static void loadImage(RequestManager manager, String imageUrl, int resourceId, ImageView imageView) {
//        loadImage(manager, imageUrl, RequestOptions.placeholderOf(resourceId), imageView);
//    }
    public void loadImageMaybeGif(Context context, ImageView imageView, String imageUrl, RequestOptions options) {
        if (!isValidContextForGlide(context)) {
            return;
        }

        loadImageMaybeGif(GlideApp.with(context), imageUrl, options, new ByteImageViewTarget.Builder(imageView).build(), null);
    }

    public void loadImageMaybeGif(Activity activity, ImageView imageView, String imageUrl, RequestOptions options, RequestListener listener) {
        if (!isValidContextForGlide(activity)) {
            return;
        }

        loadImageMaybeGif(GlideApp.with(activity), imageUrl, options, new ByteImageViewTarget.Builder(imageView).build(), listener);
    }

    private void loadImageMaybeGif(RequestManager manager, String imageUrl, RequestOptions options, Target<byte[]> target, RequestListener listener) {
        RequestBuilder builder = manager.as(byte[].class).load(imageUrl).apply(options);
        if (null != listener) {
            builder = builder.listener(listener);
        }
        builder.into(target);
    }


}

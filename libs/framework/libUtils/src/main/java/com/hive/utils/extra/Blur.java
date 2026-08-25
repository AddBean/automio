// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.extra;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;

import com.hive.utils.debug.DLog;

public class Blur {

    private static final String TAG = "Blur";

//	private static boolean isLruCache_init = false;
//	private static LruCache<String, Bitmap> mMemoryCache;
//
//	public static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
//		if (getBitmapFromMemCache(key) == null) {
//			mMemoryCache.put(key, bitmap);
//		}
//	}
//
//	public static Bitmap getBitmapFromMemCache(String key) {
//		return mMemoryCache.get(key);
//	}
//
//	public static void reInitLurCache() {
//		//DLog.d(TAG,"reInitLurCache calling");
//                isLruCache_init = false;
//	}

    @SuppressLint("NewApi")
    public static Bitmap fastblur(Bitmap sentBitmap, int radius) throws OutOfMemoryError{

        Matrix matrix = new Matrix();
        matrix.postScale(0.5f, 0.5f); //长和宽放大缩小的比例

        try {
            sentBitmap = Bitmap.createBitmap(sentBitmap, 0, 0, sentBitmap.getWidth(), sentBitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            System.gc();
            return null;
        }
        //7.0系统SO库会有崩溃  先统一使用算法
//        try {
//            if (VERSION.SDK_INT > 16) {
//                Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
//
//                RenderScript rs = RenderScript.create(context);
//                Allocation input;
//                try {
//                    input = Allocation.createFromBitmap(rs, sentBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
//                } catch (OutOfMemoryError e) {
//                    System.gc();
//                    return null;
//                }
//                Allocation output = Allocation.createTyped(rs, input.getType());
//                ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
//                script.setRadius(radius /* e.g. 3.f */);
//                script.setInput(input);
//                script.forEach(output);
//                output.copyTo(bitmap);
//                return bitmap;
//            }
//        }catch (OutOfMemoryError e){
//            return null;
//        }

        // Stack Blur v1.0 from
        // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
        //
        // Java Author: Mario Klingemann <mario at quasimondo.com>
        // http://incubator.quasimondo.com
        // created Feburary 29, 2004
        // Android port : Yahel Bouaziz <yahel at kayenko.com>
        // http://www.kayenko.com
        // ported april 5th, 2012

        // This is a compromise between Gaussian Blur and Box blur
        // It creates much better looking blurs than Box Blur, but is
        // 7x faster than my Gaussian Blur implementation.
        //
        // I called it Stack Blur because this describes best how this
        // filter works internally: it creates a kind of moving stack
        // of colors whilst scanning through the image. Thereby it
        // just has to add one new block of color to the right side
        // of the stack and remove the leftmost color. The remaining
        // colors on the topmost layer of the stack are either added on
        // or reduced by one, depending on if they are on the right or
        // on the left side of the stack.
        //
        // If you are using this algorithm in your code please add
        // the following line:
        //
        // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

        try {
            Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

            if (radius < 1) {
                return (null);
            }

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            int[] pix = new int[w * h];
            DLog.e("pix", w + " " + h + " " + pix.length);
            bitmap.getPixels(pix, 0, w, 0, 0, w, h);

            int wm = w - 1;
            int hm = h - 1;
            int wh = w * h;
            int div = radius + radius + 1;

            int r[] = new int[wh];
            int g[] = new int[wh];
            int b[] = new int[wh];
            int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
            int vmin[] = new int[Math.max(w, h)];

            int divsum = (div + 1) >> 1;
            divsum *= divsum;
            int dv[] = new int[256 * divsum];
            for (i = 0; i < 256 * divsum; i++) {
                dv[i] = (i / divsum);
            }

            yw = yi = 0;

            int[][] stack = new int[div][3];
            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int r1 = radius + 1;
            int routsum, goutsum, boutsum;
            int rinsum, ginsum, binsum;

            for (y = 0; y < h; y++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                for (i = -radius; i <= radius; i++) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rbs = r1 - Math.abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                }
                stackpointer = radius;

                for (x = 0; x < w; x++) {

                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm);
                    }
                    p = pix[yw + vmin[x]];

                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[(stackpointer) % div];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi++;
                }
                yw += w;
            }
            for (x = 0; x < w; x++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                yp = -radius * w;
                for (i = -radius; i <= radius; i++) {
                    yi = Math.max(0, yp) + x;

                    sir = stack[i + radius];

                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];

                    rbs = r1 - Math.abs(i);

                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;

                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }

                    if (i < hm) {
                        yp += w;
                    }
                }
                yi = x;
                stackpointer = radius;
                for (y = 0; y < h; y++) {
                    // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                    pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w;
                    }
                    p = x + vmin[y];

                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi += w;
                }
            }

            DLog.e("pix", w + " " + h + " " + pix.length);
            bitmap.setPixels(pix, 0, w, 0, 0, w, h);
            return bitmap;
        }catch (OutOfMemoryError e){

        }
        return null;
    }


    /**
     * 加载所有屏幕的高斯模糊图
     * <p>
     * 2015-01-08
     *
     * @param bkg
     * @param view return
     * @author 李晓磊
     */
    public static Bitmap scaleBlurBitmap(Bitmap bkg, View view) {
        if (bkg == null || bkg.getWidth() <= 0 || bkg.getHeight() <= 0 || bkg.isRecycled()) {
            DLog.d("blur", "bkg fail");
            return null;
        }

        if (view == null /*|| view.getWidth() == 0 || view.getHeight() == 0*/) {
            DLog.d("blur", "view fail");
            return null;
        }

        //需要缩小的等级，在代码中我把bitmap的尺寸缩小到原图的1/10。因为这个bitmap在模糊处理时会先被缩小然后再放大。
//        float scaleFactor = 10;
//        float radius = 15;//模糊度

        // 缩小为宽为100的尺寸，并根据这个尺寸计算缩放值
        int standardW = 50;
        int standardH = (bkg.getHeight() * standardW / bkg.getWidth());
        int scaleFactorW = (bkg.getWidth() > standardW) ? (bkg.getWidth() / standardW) : 1;
        int scaleFactorH = (bkg.getHeight() > standardH) ? (bkg.getHeight() / standardH) : 1;
        int radius = (scaleFactorH * scaleFactorW < 20) ? 8 : 15;

//        if (null != LauncherAppState.getInstance().getDynamicGrid() && null != LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile()) {
//            float density = LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile().mDensity;
//            DLog.d("blur", "mDensity = " + density);
//
//            if (density < 2) {//屏幕密度低的手机，模糊的太高就完全看不清了
//                scaleFactor = 5;
//                radius = 10;
//            }
//        }

        Bitmap overlay = Bitmap.createBitmap((int) (bkg.getWidth() * 1.0f / scaleFactorW), (int) (bkg.getHeight() * 1.0f / scaleFactorH), Bitmap.Config.ARGB_4444);

        Canvas canvas = new Canvas(overlay);
        canvas.translate(-view.getLeft() * 1.0f / scaleFactorW, -view.getTop() * 1.0f / scaleFactorH);
        canvas.scale(1.0f / scaleFactorW, 1.0f / scaleFactorH);
        Paint paint = new Paint();

        // Paint提供了FILTER_BITMAP_FLAG标示，这样的话在处理bitmap缩放的时候，就可以达到双缓冲的效果，模糊处理的过程就更加顺畅了。
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bkg, 0, 0, paint);

        bkg.recycle();

        overlay = doBlurBitmap(overlay, radius, true);

        return overlay;
    }


    /**
     * 模糊处理操作，这次的图片小了很多，幅度也降低了很多，所以模糊过程非常快。
     *
     * @param sentBitmap
     * @param radius
     * @param canReuseInBitmap return  把模糊处理后的图片作为背景，它会自动进行放大操作的。
     */
    public static Bitmap doBlurBitmap(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {
        // Stack Blur v1.0 from
        // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
        //
        // Java Author: Mario Klingemann <mario at quasimondo.com>
        // http://incubator.quasimondo.com
        // created Feburary 29, 2004
        // Android port : Yahel Bouaziz <yahel at kayenko.com>
        // http://www.kayenko.com
        // ported april 5th, 2012

        // This is a compromise between Gaussian Blur and Box blur
        // It creates much better looking blurs than Box Blur, but is
        // 7x faster than my Gaussian Blur implementation.
        //
        // I called it Stack Blur because this describes best how this
        // filter works internally: it creates a kind of moving stack
        // of colors whilst scanning through the image. Thereby it
        // just has to add one new block of color to the right side
        // of the stack and remove the leftmost color. The remaining
        // colors on the topmost layer of the stack are either added on
        // or reduced by one, depending on if they are on the right or
        // on the left side of the stack.
        //
        // If you are using this algorithm in your code please add
        // the following line:
        //
        // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

        Bitmap bitmap = null;

        try {
            if (canReuseInBitmap) {
                bitmap = sentBitmap;
            } else {
                bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
            }

            if (radius < 1) {
                return (null);
            }

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            int[] pix = new int[w * h];
            bitmap.getPixels(pix, 0, w, 0, 0, w, h);

            int wm = w - 1;
            int hm = h - 1;
            int wh = w * h;
            int div = radius + radius + 1;

            int r[] = new int[wh];
            int g[] = new int[wh];
            int b[] = new int[wh];
            int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
            int vmin[] = new int[Math.max(w, h)];

            int divsum = (div + 1) >> 1;
            divsum *= divsum;
            int dv[] = new int[256 * divsum];
            for (i = 0; i < 256 * divsum; i++) {
                dv[i] = (i / divsum);
            }

            yw = yi = 0;

            int[][] stack = new int[div][3];
            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int r1 = radius + 1;
            int routsum, goutsum, boutsum;
            int rinsum, ginsum, binsum;

            for (y = 0; y < h; y++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                for (i = -radius; i <= radius; i++) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rbs = r1 - Math.abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                }
                stackpointer = radius;

                for (x = 0; x < w; x++) {

                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm);
                    }
                    p = pix[yw + vmin[x]];

                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[(stackpointer) % div];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi++;
                }
                yw += w;
            }
            for (x = 0; x < w; x++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                yp = -radius * w;
                for (i = -radius; i <= radius; i++) {
                    yi = Math.max(0, yp) + x;

                    sir = stack[i + radius];

                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];

                    rbs = r1 - Math.abs(i);

                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;

                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }

                    if (i < hm) {
                        yp += w;
                    }
                }
                yi = x;
                stackpointer = radius;
                for (y = 0; y < h; y++) {
                    // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                    pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;

                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];

                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];

                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w;
                    }
                    p = x + vmin[y];

                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];

                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];

                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;

                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer];

                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];

                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];

                    yi += w;
                }
            }
            bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        } catch (OutOfMemoryError e) {

        }
        return (bitmap);
    }

//    /**
//     * 获取高斯模糊后的壁纸
//     */
//    public static void buildWallpaperBitmap(final Context context, final BlurWallpaperListener listener) {
//
//        ThreadPools.getInstance().post(new Runnable() {
//            @Override
//            public void run() {
//                Bitmap bitmap = buildWallpaperBitmap(context);
//                if (null != listener) {
//                    listener.onWallpaperBlur(bitmap);
//                }
//            }
//        });
//    }
//
//    public static Bitmap buildWallpaperBitmap(Context context) {
//
//        try {
//            Bitmap wallpaper = null;
//            if (LauncherAppState.getInstance().getLruCacheForBlur() != null) {
//                wallpaper = LauncherAppState.getInstance().getLruCacheForBlur().getBitmap(LruCacheForBlur.KEY_WALLPAPER_BLUR);
//                if (null != wallpaper) {
//                    return wallpaper;
//                }
//            }
//
//            if (null == context) {
//                return null;
//            }
//
//            int width = context.getResources().getDisplayMetrics().widthPixels;
//            int height = context.getResources().getDisplayMetrics().heightPixels;
//
//            if (width == 0 || height == 0) {
//
//                return null;
//            }
//
//            if (LauncherAppState.getInstance().getLruCacheForBlur() != null) {
//                wallpaper = LauncherAppState.getInstance().getLruCacheForBlur().getBitmap(LruCacheForBlur.KEY_WALLPAPER_BACKGROUND);
//            }
//
//            if (null == wallpaper) {
//                DLog.d(TAG, "try get wallpaper bitmap");
//                wallpaper = getWallpaperBitmap(context, width, height);
//
//                if (null != wallpaper && wallpaper.getWidth() > 0 && wallpaper.getHeight() > 0 && null != LauncherAppState.getInstance().getLruCacheForBlur()) {
//                    //缩小100倍缓存
//                    int standardW = 150;
//                    int standardH = (wallpaper.getHeight() * standardW / wallpaper.getWidth());
//                    int scaleFactorW = (wallpaper.getWidth() > standardW) ? (wallpaper.getWidth() / standardW) : 1;
//                    int scaleFactorH = (wallpaper.getHeight() > standardH) ? (wallpaper.getHeight() / standardH) : 1;
//                    float scaleW = (float) (1.0 / scaleFactorW);
//                    float scaleH = (float) (1.0 / scaleFactorH);
//
//                    Matrix matrix = new Matrix();
//                    matrix.postScale(scaleW, scaleH);
//                    Bitmap wallpaperSmall = Bitmap.createBitmap(wallpaper, 0, 0, wallpaper.getWidth(), wallpaper.getHeight(), matrix, true);
//
//                    LauncherAppState.getInstance().getLruCacheForBlur().putBitmap(LruCacheForBlur.KEY_WALLPAPER_BACKGROUND, wallpaperSmall);
//
//                    if(wallpaper != wallpaperSmall) {
//                        wallpaper.recycle();
//                        wallpaper = wallpaperSmall;
//                    }
//                }
//            } else {
//                DLog.d(TAG, "get wallpaper bitmap from cache");
//            }
//
//            if (null == wallpaper || wallpaper.isRecycled()) {
//                DLog.d(TAG, "get wallpaper is null");
//
//                return null;
//            }
//
//            width = wallpaper.getWidth();
//            height = wallpaper.getHeight();
//
//            if (width <= 0 || height <= 0) {
//                DLog.d(TAG, "width or height illegal");
//                return null;
//            }
//
//            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//            Canvas canvas = new Canvas(result);
//            Rect desRect = new Rect(0, 0, width, height);
//            canvas.drawBitmap(wallpaper, null, desRect, null);
//
//            result = doBlurBitmap(result, 5, true);
//            LauncherAppState.getInstance().getLruCacheForBlur().putBitmap(LruCacheForBlur.KEY_WALLPAPER_BLUR, result);
//            return result;
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public interface BlurWallpaperListener {
//        void onWallpaperBlur(Bitmap result);
//    }
//
//    /**
//     * 利用当前页面内容生成高斯模糊
//     */
//    public static Bitmap buildCurrentPageBitmap(Context context, int color) {
//
//        if (null == context) {
//            return null;
//        }
//        int width = context.getResources().getDisplayMetrics().widthPixels;
//        int height = context.getResources().getDisplayMetrics().heightPixels;
//
//        if (width == 0 || height == 0) {
//
//            return null;
//        }
//
//        Bitmap wallpaper = null;
//
//        if (LauncherAppState.getInstance().getLruCacheForBlur() != null) {
//            wallpaper = LauncherAppState.getInstance().getLruCacheForBlur().getBitmap(LruCacheForBlur.KEY_WALLPAPER_BACKGROUND);
//        }
//
//        if (null == wallpaper) {
//            DLog.d(TAG, "try get wallpaper bitmap");
//            wallpaper = getWallpaperBitmap(context, width, height);
//
//            if (null != wallpaper && wallpaper.getWidth() > 0 && wallpaper.getHeight() > 0 && null != LauncherAppState.getInstance().getLruCacheForBlur()) {
//                //缩小100倍缓存
//                int standardW = 100;
//                int standardH = (wallpaper.getHeight() * standardW / wallpaper.getWidth());
//                int scaleFactorW = (wallpaper.getWidth() > standardW) ? (wallpaper.getWidth() / standardW) : 1;
//                int scaleFactorH = (wallpaper.getHeight() > standardH) ? (wallpaper.getHeight() / standardH) : 1;
//                float scaleW = (float) (1.0 / scaleFactorW);
//                float scaleH = (float) (1.0 / scaleFactorH);
//
//                Matrix matrix = new Matrix();
//                matrix.postScale(scaleW, scaleH);
//                Bitmap wallpaperSmall = Bitmap.createBitmap(wallpaper, 0, 0, wallpaper.getWidth(), wallpaper.getHeight(), matrix, true);
//
//                LauncherAppState.getInstance().getLruCacheForBlur().putBitmap(LruCacheForBlur.KEY_WALLPAPER_BACKGROUND, wallpaperSmall);
//            }
//        } else {
//            DLog.d(TAG, "get wallpaper bitmap from cache");
//        }
//
//        if (null == wallpaper || wallpaper.isRecycled()) {
//            DLog.d(TAG, "get wallpaper is null");
//
//            return null;
//        }
//
//        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(result);
//        Rect desRect = new Rect(0, 0, width, height);
//        canvas.drawBitmap(wallpaper, null, desRect, null);
//
//        Launcher launcher = LauncherAppState.getInstance().getLauncher();
//        if (null == launcher) {
//            DLog.w(TAG, "launcher is null");
////            canvas.drawColor(color, PorterDuff.Mode.SRC_ATOP);
////            canvas.setBitmap(null);
////            return result;
//
//            canvas.setBitmap(null);
//            result.recycle();
//            return null;
//        }
//
//        if (null == launcher.getWorkspace()) {
//            DLog.w(TAG, "workspace is null");
//
////            canvas.drawColor(color, PorterDuff.Mode.SRC_ATOP);
////            canvas.setBitmap(null);
////            return result;
//
//            canvas.setBitmap(null);
//            result.recycle();
//            return null;
//        }
//
//        View cellLayout = launcher.getWorkspace().getChildAt(launcher.getWorkspace().getCurrentPage());
//        View hotSeat = launcher.getDockBar();
//
//        if (cellLayout == null || hotSeat == null) {
//            DLog.w(TAG, "cellLayout or  hotSeat is null");
//
////            canvas.drawColor(color, PorterDuff.Mode.SRC_ATOP);
////            canvas.setBitmap(null);
////            return result;
//
//            canvas.setBitmap(null);
//            result.recycle();
//            return null;
//        }
//
//
//        Bitmap cellBitmap = cellLayout.getDrawingCache();
//
//        if (null != cellBitmap && !cellBitmap.isRecycled()) {
//            int[] loc = new int[2];
////            cellLayout.getLocationInWindow(loc);
////            DLog.d(TAG, "getLocationOnScreen " + loc[0] + ", " + loc[1]);
//
//
//            cellLayout.getLocationOnScreen(loc);
//            DLog.d(TAG, "getLocationOnScreen " + loc[0] + ", " + loc[1]);
//
//            if (null != launcher.getSystemBarTintManager()) {
//                loc[1] += launcher.getSystemBarTintManager().getConfig().getStatusBarHeight();
//                DLog.d(TAG, "getLocationOnScreen " + loc[0] + ", " + loc[1]);
//
////                loc[1] += LauncherAppState.getInstance().getDynamicGrid().getDeviceProfile().padding;
////                DLog.d(TAG, "getLocationOnScreen " + loc[0] + ", " + loc[1]);
//            }
//
////
////            desRect = new Rect(0, loc[1], width, loc[1] + cellLayout.getHeight() * width / cellLayout.getWidth());
////            canvas.drawBitmap(cellBitmap, null, desRect, null);
//
//            desRect = new Rect(loc[0], loc[1], loc[0] + cellBitmap.getWidth(), loc[1] + cellBitmap.getHeight());
//            canvas.drawBitmap(cellBitmap, null, desRect, null);
//
//            cellBitmap.recycle();
//        } else {
//
//            canvas.setBitmap(null);
//            result.recycle();
//            return null;
//        }
//
//        Bitmap hotBitmap = hotSeat.getDrawingCache();
//        if (null != hotBitmap && !hotBitmap.isRecycled()) {
//
//            desRect = new Rect(0, height - hotSeat.getHeight() * width / hotSeat.getWidth(), width, height);
//            canvas.drawBitmap(hotBitmap, null, desRect, null);
//            hotBitmap.recycle();
//
//        } else {
//
//            canvas.setBitmap(null);
//            result.recycle();
//            return null;
//        }
//
//        cellLayout.destroyDrawingCache();
//        hotSeat.destroyDrawingCache();
//        canvas.drawColor(color, PorterDuff.Mode.SRC_ATOP);
//        canvas.setBitmap(null);
//
//        DLog.d(TAG, "blur is ok");
//
//        return result;
//    }


//    /**
//     * 将View转成图片
//     *
//     * @param context
//     * @param view
//     * @return
//     */
//    public static Bitmap getBitmapFromView(Context context, final View view) {
//
//        if (view == null || view.getWidth() == 0 || view.getHeight() == 0) {
//            return null;
//        }
//
//        DLog.d(TAG, "source view width = " + view.getWidth() + "; height = " + view.getHeight());
//
//        //long start = System.currentTimeMillis();
//
//        Bitmap result = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(result);
//        //DLog.d("debug","time 000000 getBitmapFromView getWallpaperBitmap = " +(System.currentTimeMillis() - start));
//
//        //start = System.currentTimeMillis();
//        Bitmap wallpaper = null;
//
//        if (LauncherAppState.getInstance().getLruCacheForBlur() != null) {
//            wallpaper = LauncherAppState.getInstance().getLruCacheForBlur().getBitmap(LruCacheForBlur.KEY_WALLPAPER_BACKGROUND);
//        }
//        if (null == wallpaper) {
//            DLog.d(TAG, "try get wallpaper bitmap");
//            wallpaper = getWallpaperBitmap(context, view.getWidth(), view.getHeight());
//
//            if (LauncherAppState.getInstance().getLruCacheForBlur() != null) {
//                LauncherAppState.getInstance().getLruCacheForBlur().putBitmap(LruCacheForBlur.KEY_WALLPAPER_BACKGROUND, wallpaper);
//            }
//        } else {
//            DLog.d(TAG, "get wallpaper bitmap from cache");
//        }
//
//        //DLog.d("debug","time getBitmapFromView getWallpaperBitmap = " +(System.currentTimeMillis() - start));
//        //start = System.currentTimeMillis();
//
//        if (wallpaper != null) {
//            //壁纸伸缩处理
//            Rect desRect = new Rect(0, 0, view.getWidth(), view.getHeight());
//            canvas.drawBitmap(wallpaper, null, desRect, null);
//        }
//        //DLog.d("debug","time 000000 getBitmapFromView drawBitmap = " +(System.currentTimeMillis() - start));
//
//        //canvas.drawColor(Color.argb(120, 256, 256, 256));//增加灰度
//        //start = System.currentTimeMillis();
//
//        try {
//            view.draw(canvas);
//
//        } catch (Exception e) {
//            DLog.w(TAG, "view is has been gc; ");
//            return null;
//        }
//        canvas.setBitmap(null);
//        //DLog.d("debug","time getBitmapFromView drawBitmap = " +(System.currentTimeMillis() - start));
//
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                destroyViewDrawingCache(view);
//            }
//        }).start();
//
//        return result;
//    }
//
//
//    //获得裁剪后的壁纸
//    public static Bitmap getWallpaperBitmap(Context context, int width, int height) {
//        if (null == context || width <= 0 || height <= 0) {
//            return null;
//        }
//
//        WallpaperManager manager = WallpaperManager.getInstance(context);
//
//        Drawable drawable = null;
//        Bitmap wallBitmap = null;
//
//        if (null != manager.getWallpaperInfo()) {
//            //当前是动态壁纸
//
//        } else {
//            drawable = manager.getDrawable();
//        }
//
//        if (null != drawable) {
//            wallBitmap = ((BitmapDrawable) drawable).getBitmap();
//        }
//
//        manager.forgetLoadedWallpaper();
//        if (wallBitmap == null || wallBitmap.isRecycled()) {
//
////            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
////            Canvas canvas = new Canvas(bitmap);
////            canvas.drawColor(Color.parseColor("#99FFFFFF"), PorterDuff.Mode.SRC_ATOP);
////            canvas.setBitmap(null);
////
////            return bitmap;
//
//            return null;
//        }
//
//        int top, left;
//        if (width < wallBitmap.getWidth()) {
//            left = (wallBitmap.getWidth() - width) / 2;
//        } else {
//            left = 0;
//            width = wallBitmap.getWidth();
//        }
//
//        if (height < wallBitmap.getHeight()) {
//            top = (wallBitmap.getHeight() - height) / 2;
//        } else {
//            top = 0;
//            height = wallBitmap.getHeight();
//        }
//
//        Bitmap result = null;
//        try {
//            result = Bitmap.createBitmap(wallBitmap, left, top, width, height);
//        } catch (Exception e) {
//            e.printStackTrace();
//
//            return null;
//        }
//
//        if (result != wallBitmap) {
//            wallBitmap.recycle();
//        }
//
//        return result;
//    }
//
//
//    private static void destroyViewDrawingCache(View view) {
//        if (view instanceof ViewGroup) {
//            ViewGroup group = (ViewGroup) view;
//            for (int i = 0; i < group.getChildCount(); i++) {
//                View child = ((ViewGroup) view).getChildAt(i);
//                destroyViewDrawingCache(child);
//            }
//        }
//        view.destroyDrawingCache();
//    }
}

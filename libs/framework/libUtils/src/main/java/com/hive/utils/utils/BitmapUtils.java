// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;

import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class BitmapUtils {
    /**
     * 发送广播，重新挂载SD卡
     */
    public static void sendBroadCaseRemountSDcard(Context context) {
        Intent intent = new Intent();
        // 重新挂载的动作
        intent.setAction(Intent.ACTION_MEDIA_MOUNTED);
        // 要重新挂载的路径
        intent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
        context.sendBroadcast(intent);
    }

    public static String saveBitmapFromLayoutId(Context context, int resId) {
        return saveBitmapFromLayoutId(context, resId, -1, -1);
    }

    public static String saveBitmapFromLayoutId(Context context, int resId, int w, int h) {
        View view = LayoutInflater.from(context).inflate(resId, null);
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(w > 0 ? w : 0, w > 0 ? View.MeasureSpec.EXACTLY : View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(h > 0 ? h : 0, h > 0 ? View.MeasureSpec.EXACTLY : View.MeasureSpec.UNSPECIFIED);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        view.setDrawingCacheEnabled(true);
        view.setDrawingCacheBackgroundColor(Color.WHITE);
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return BitmapUtils.saveBitmap(bitmap);
    }


    public static String saveBitmap( Bitmap bitmap) {
        return saveBitmap(bitmap, null);
    }

    public static String saveBitmap(Bitmap bitmap, String fileName) {
        File file = null;
        try {
            file = new File(getExternalCacheDir(), TextUtils.isEmpty(fileName) ? getFileName() : fileName);
            FileOutputStream os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        openFile(context, file);
        return file.getPath();
    }

    public static String saveBitmapLocal(Bitmap bitmap, String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            FileOutputStream os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        openFile(context, file);
        return file.getPath();
    }

    public static String saveBitmapFormView(View view) {
        view.invalidate();
        File file = null;
        try {
            file = new File(getExternalCacheDir(), getFileName());
            FileOutputStream os = new FileOutputStream(file);
            getViewBitmap(view).compress(Bitmap.CompressFormat.PNG, 90, os);
            os.flush();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getPath();
    }

    /**
     * 加载本地图片，动态缩放；
     *
     * @param path
     * @return
     */
    public static Bitmap getBitmapFromFile(String path) {
        printfMemInf();
        Bitmap bmp = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            bmp = BitmapFactory.decodeFile(path, opts);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            DLog.e("内存溢出");
        }

        return bmp;
    }

    public static Bitmap createBitmapSafely(int width, int height, Bitmap.Config config, int retryCount) {
        try {
            return Bitmap.createBitmap(width, height, config);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            if (retryCount > 0) {
                System.gc();
                return createBitmapSafely(width, height, config, retryCount - 1);
            }
            return null;
        }
    }

    public static void printfMemInf() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();//应用程序已获得内存
        long freeMemory = Runtime.getRuntime().freeMemory();//应用程序已获得内存中未使用内存
        DLog.e("maxMemory=" + maxMemory / 1024 / 1024 + "M,totalMemory=" + totalMemory / 1024 / 1024 + "M,freeMemory=" + freeMemory / 1024 / 1024 + "M");
    }


    public static Bitmap getViewBitmap(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap bitmap = view.getDrawingCache(true);

        Bitmap bmp = duplicateBitmap(bitmap);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        view.setDrawingCacheEnabled(false);
        return bmp;
    }


    public static Bitmap duplicateBitmap(Bitmap bmpSrc) {
        if (null == bmpSrc) {
            return null;
        }

        int bmpSrcWidth = bmpSrc.getWidth();
        int bmpSrcHeight = bmpSrc.getHeight();

        Bitmap bmpDest = Bitmap.createBitmap(bmpSrcWidth, bmpSrcHeight, Bitmap.Config.ARGB_8888);
        if (null != bmpDest) {
            Canvas canvas = new Canvas(bmpDest);
            final Rect rect = new Rect(0, 0, bmpSrcWidth, bmpSrcHeight);

            canvas.drawBitmap(bmpSrc, rect, rect, null);
        }

        return bmpDest;
    }

    public static Bitmap createViewBitmap(ViewGroup v) {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        v.draw(canvas);
        return bitmap;
    }

    public static String getExternalCacheDir() {

        String path = GlobalApp.sContext.getExternalCacheDir() + "/";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }

    public static String getTimeCacheDir() {
        String format = "yyyy/MM/dd/";
        SimpleDateFormat df = new SimpleDateFormat(format);
        String path = GlobalApp.sContext.getExternalCacheDir() + "/" + df.format(new Date());
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }

    public static String getFileName() {
        Random r = new Random();
        int randomNum = r.nextInt(10000);
        String format = "yyyyMMddHHmmss";
        SimpleDateFormat df = new SimpleDateFormat(format);
        String name = df.format(new Date());
        name = name + String.valueOf(randomNum);
        return name + ".jpg";
    }

    public static void openFile(Context context, File file) {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
        //获取文件file的MIME类型
        String type = getMIMEType(file);
        //设置intent的data和Type属性。
        intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
        //跳转
        context.startActivity(intent);     //这里最好try一下，有可能会报错。 //比如说你的MIME类型是打开邮箱，但是你手机里面没装邮箱客户端，就会报错。

    }

    /**
     * 通过路径获取图片Bitmap
     *
     * @param path
     * @param w    小于或等于0返回原图
     * @param h    小于或等于0返回原图
     * @return
     */
    public static Bitmap getBitmapFromPath(String path, int w, int h, int inSampleSize) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = inSampleSize;
            Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
            DLog.e("inSampleSize:" + inSampleSize + "\n bitmapHeight:" + bitmap.getHeight() + " bitmapWidth:" + bitmap.getWidth());
            if ((w > 0 && h > 0))
                return Bitmap.createScaledBitmap(bitmap, w, h, true);
            else
                return bitmap;
        } catch (OutOfMemoryError e) {
            DLog.e("内存溢出,重新取值");
            System.gc();
            //改变缩放大小；
            return getBitmapFromPath(path, w, h, inSampleSize * 2);
        }
    }

    /**
     * 根据文件后缀名获得对应的MIME类型。
     *
     * @param file
     */
    public static String getMIMEType(File file) {

        String type = "*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /* 获取文件的后缀名*/
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0; i < MIME_MapTable.length; i++) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    public static String bitmap2String(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
        byte[] buff = stream.toByteArray();
        return Base64.encodeToString(buff, Base64.DEFAULT);
    }


    public static Bitmap createScaledBitmap(Bitmap srcBmp, double newWidth,
                                            double newHeight) {
        // 获取这个图片的宽和高
        float width = srcBmp.getWidth();
        float height = srcBmp.getHeight();
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 计算宽高缩放率
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmap = Bitmap.createBitmap(srcBmp, 0, 0, (int) width, (int) height, matrix, false);
        return bitmap;
    }

    public static Bitmap getLocalBitmap(String path) {

        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, newOpts);
        return bitmap;
    }


    public static Bitmap drawableToBitmap(@DrawableRes int resId) {
        return drawableToBitmap(GlobalApp.getContext().getResources().getDrawable(resId));
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap.Config config =
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565;
        bitmap = Bitmap.createBitmap(w, h, config);
        //注意，下面三行代码要用到，否在在View或者surfaceview里的canvas.drawBitmap会看不到图
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    public static final String[][] MIME_MapTable = {
            //{后缀名，MIME类型}
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop", "text/plain"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/x-zip-compressed"},
            {"", "*/*"}
    };

    public static Bitmap autoResizeBitmap(Bitmap bitmap, int maxSize) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = 1.0f;
        if (width >= height && width > maxSize) {
            scale = ((float) maxSize) / width;
        } else if (height > width && height > maxSize) {
            scale = ((float) maxSize) / height;
        }
        if (scale != 1.0f) {
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            Bitmap newBmp = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            if (newBmp != bitmap) {
                bitmap.recycle();
                bitmap = null;
            }
            return newBmp;
        }
        return bitmap;
    }

    /**
     * 质量压缩并存到SD卡中
     *
     * @param bitmap
     * @param reqSize 需要的大小
     * @return
     */
    public static Bitmap compressQuality(Bitmap bitmap, int reqSize) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //这里100表示不压缩，把压缩后的数据存放到baos中
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            int options = 95;
            //如果压缩后的大小超出所要求的，继续压缩
            while (baos.toByteArray().length / 1024 > reqSize) {
                baos.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);

                //每次减少5%质量
                if (options > 5) {//避免出现options<=0
                    options -= 5;
                } else {
                    break;
                }
            }
            byte[] bytes = baos.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap compressAndResize(Bitmap bitmap, int maxSize, int reqSize) {
        Bitmap newBmp = autoResizeBitmap(bitmap, maxSize);
        return compressQuality(newBmp, reqSize);
    }
}

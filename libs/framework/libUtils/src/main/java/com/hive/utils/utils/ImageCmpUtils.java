// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.hive.utils.debug.DLog;
import com.hive.utils.io.IoUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;


/**
 * 图片压缩工具类（参照http://104zz.iteye.com/blog/1694762文章）
 */
public class ImageCmpUtils {


    /**
     * @param srcPath
     * @param destH   output bitmap max height
     * @param destW   output bitmap max wight
     * @return null if crop it error
     */
    public static Bitmap cropImage(final String srcPath, final int destH, final int destW) {

        if (TextUtils.isEmpty(srcPath) || !new File(srcPath).isFile() || destH < 0 || destW < 0) {
            return null;
        }

        final BitmapFactory.Options scrOpts = new BitmapFactory.Options();
        scrOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, scrOpts);

        scrOpts.inJustDecodeBounds = false;

        final int srcW = scrOpts.outWidth;
        final int srcH = scrOpts.outHeight;

        final float hh = destH;
        final float ww = destW;

        int sampleSize = 1;
        if (srcW > srcH && srcW > ww) {
            sampleSize = (int) (srcW / ww);
        } else if (srcW < srcH && srcH > hh) {
            sampleSize = (int) (srcH / hh);
        }
        if (sampleSize <= 0) sampleSize = 1;
        scrOpts.inSampleSize = sampleSize;

        return BitmapFactory.decodeFile(srcPath, scrOpts);
    }

    /**
     * @param srcBitmap
     * @param maxSize   bytes
     * @return
     */
    public static Bitmap compressImage(final Bitmap srcBitmap, final long maxSize) {
        if (srcBitmap == null) {
            return null;
        }
        ByteArrayOutputStream baos = null;
        try {

            int options = 100;
            baos = new ByteArrayOutputStream();
            srcBitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);

            while (baos.toByteArray().length > maxSize && options > 10) {
                baos.reset();
                srcBitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);
                options -= 10;
            }

            return BitmapFactory.decodeStream(new ByteArrayInputStream(baos.toByteArray()), null, null);

        } catch (Exception e) {
            return null;
        } finally {
            IoUtil.closeSilently(baos);
        }

    }

    /**
     * @param srcPath
     * @param destH
     * @param destW
     * @param maxSize
     * @return
     */
    public static Bitmap cropImageThenCompress(final String srcPath, final int destH, final int destW, final int maxSize) {

        Bitmap bitmap = cropImage(srcPath, destH, destW);
        if (bitmap != null) {
            Bitmap dest = compressImage(bitmap, maxSize);
            if (dest != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
            return dest;
        }
        return bitmap;
    }

    /**
     * 将bitmap写入文件
     *
     * @param path
     * @param img
     */
    public static boolean saveBitmap(final String path, final Bitmap img) {
        if (TextUtils.isEmpty(path) || img == null) {
            return false;
        }
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out = null;
        ByteArrayOutputStream bout = null;
        try {
            bout = new ByteArrayOutputStream();
            out = new FileOutputStream(path);
            img.compress(Bitmap.CompressFormat.JPEG, 50, bout);
            out.write(bout.toByteArray());
            out.flush();
            return true;
        } catch (IOException e) {
            if (DLog.isDebug()) {
                DLog.i("UserProtocolController", "saveBitmap error:"+e.getMessage());
            }
        } finally {
            IoUtil.closeSilently(bout);
            IoUtil.closeSilently(out);
        }
        return false;
    }

    public static Bitmap getImageBitmap(Context ctx,String path){
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path,newOpts);//此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;

        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 200f;//这里设置高度为800f
        float ww = 120f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(path, newOpts);
        return bitmap;
    }

    /**
     * 查询相片地址
     *
     * @param contentResolver
     * @param uri
     * @return
     */
    public static String getPathByDocUri(ContentResolver contentResolver, Uri uri) {
        // Will return "image:x*"
        String wholeId = "";
        try {
            Class<?> obj = Class.forName("android.provider.DocumentsContract");
            Method method = obj.getDeclaredMethod("getDocumentId", Uri.class);
            wholeId = (String) method.invoke(obj, uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StringUtils.isEmpty(wholeId) || !wholeId.contains(":")) {
            return null;
        }
        // Split at colon, use second item in the array
        String id = wholeId.split(":")[1];

        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column, sel, new String[] { id }, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(column[0]);
                return cursor.getString(columnIndex);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public static String getPathByNormal(ContentResolver contentResolver, Uri uri) {
        Cursor c = null;
        if (uri.getScheme().equals("file"))
            return uri.getPath();
        if (uri.getScheme().equals("content"))
            try {
                String str = "";
                c = contentResolver.query(uri, new String[]{"_data"}, null, null, null);
                if ((c != null) && (c.moveToFirst()))
                    str = c.getString(0);
                return str;
            } catch (Exception e) {
                return null;
            } finally {
                if (c != null)
                    c.close();
            }
        return null;
    }

    public static int[] getImageFileWH(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        return new int[]{options.outWidth,options.outHeight};
    }
}

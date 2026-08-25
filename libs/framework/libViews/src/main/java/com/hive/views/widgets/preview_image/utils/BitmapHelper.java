// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.preview_image.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * Created by AddBean on 2016/5/23.
 */
public class BitmapHelper {
    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        if (degrees == 0) return bitmap;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        bitmap.recycle();
        return newBitmap;
    }

    public static Bitmap flip(Bitmap bitmap, boolean isVer) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        if (isVer)
            matrix.postScale(1, -1);//镜像垂直翻转
        else
            matrix.postScale(-1, 1); //镜像水平翻转
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
        bitmap.recycle();
        return newBitmap;
    }

    /**
     * 获取压缩后的bitmap
     *
     * @param path
     * @return
     */
    public static Bitmap getCompressedBitmap(String path) {
        Bitmap bmp = BitmapFactory.decodeFile(path);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
//        int options = 100;
//        //循环判断如果压缩后图片是否大于10M,大于继续压缩
//        while (baos.toByteArray().length / 1024 * 1024 * 10 > 100) {
//            baos.reset();
//            bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
//            options -= 10;  //每次都减少10
//            if (options <= 0) {
//                options = 10;
//                baos.reset();
//                bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
//                break;
//            }
//        }
//        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
//        bmp = BitmapFactory.decodeStream(isBm, null, null);
        return bmp;
    }
}

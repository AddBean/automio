// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.effect_text;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Created by Administrator on 2016/10/31.
 */

public class EffectTextHelper {
    private int DP=1;
    public EffectTextHelper(Context contex) {

    }

    public Bitmap generateTextHorizontalBitmap(String text, int size, int width) {
        Paint mPaint=new Paint();
        String lines[] = text.split("\n");
        int textWidth = 0;
        for (String str : lines) {
            int temp = (int) mPaint.measureText(str);
            if (temp > textWidth)//找出最长
                textWidth = temp;
        }
        if (textWidth < 1)
            textWidth = 1;
        Bitmap bitmap = Bitmap.createBitmap(textWidth, size * (lines.length) + 16 * DP, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(0, 0, 0, 0);
        for (int i = 1; i <= lines.length; i++) {
            mPaint.setStrokeWidth(width);
            canvas.drawText(lines[i - 1], 0, i * size, mPaint);
            mPaint.setStrokeWidth(0);
            canvas.drawText(lines[i - 1], 0, i * size, mPaint);
        }
        return bitmap;
    }

    public Bitmap generateTextVerticalBitmap(Paint mPaint, String text, int size, int width) {
        String lines[] = text.split("\n");
        int textHeight = 0;
        for (String str : lines) {
            int temp = (int) mPaint.measureText(str);
            if (temp > textHeight)
                textHeight = temp;
        }
        if (textHeight < 1)
            textHeight = 1;
        Bitmap bitmap = Bitmap.createBitmap(size * (lines.length) + 8 * DP, textHeight,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(0, 0, 0, 0);
        for (int i = 0; i < lines.length; i++) {
            for (int j = 0; j < lines[i].length(); j++) {
                char[] str = lines[i].toCharArray();
                canvas.drawText(String.valueOf(str[j]), 2 * DP + i * size, (j + 1) * size, mPaint);
            }
        }
        return bitmap;
    }

    public Bitmap getBitmapByText(String text, int size, int width) {
        Paint mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mPaint.setTextSize(size);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        float[] direction = new float[]{1, 1, 1};//设置光源的方向
        float light = 0.4f;//设置环境光亮度
        float specular = 6;//选择要应用的反射等级
        float blur = 3.5f;//向mask应用一定级别的模糊
        mPaint.setMaskFilter(new EmbossMaskFilter(direction, light, specular, blur));
        mPaint.setDither(true);
        mPaint.setFlags(Paint.SUBPIXEL_TEXT_FLAG);
        return generateTextHorizontalBitmap(text,size,width);
    }
}
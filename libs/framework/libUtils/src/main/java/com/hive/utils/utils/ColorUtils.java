// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import android.text.TextUtils;
import android.widget.ImageView;

import com.hive.utils.encrypt.Md5Utils;

import java.util.HashMap;

public class ColorUtils {
    private static HashMap<Integer, Drawable> sColorMap = new HashMap<>();
    private static int[] sColors = new int[]{Color.BLACK,Color.BLACK};

    /**
     * 根据Url分配一个Drawable；
     *
     * @param url
     * @return
     */
    public static Drawable getRandomColorDrawableByUrl(String url) {
        if (TextUtils.isEmpty(url))
            url = "0";
        ColorDrawable colorDrawable;
        int colorIndex = 0;
        //优化性能
        if (sColors.length > 1) {
            colorIndex = Md5Utils.string2md5(url).getBytes()[0] % sColors.length;
        }

        int color = sColors[colorIndex];
        if (sColorMap.containsKey(color)) {
            colorDrawable = (ColorDrawable) sColorMap.get(color);
            return colorDrawable;
        }
        colorDrawable = new ColorDrawable();

        colorDrawable.setColor(color);
        sColorMap.put(color, colorDrawable);
        return colorDrawable;
    }


    public static void setDefaultColors(int[] colors) {
        sColors = colors;
    }

    public static Drawable getDefaultImageDrawble(ImageView iv) {
        if (iv == null)
            return null;
        return iv.getDrawable();
    }

    public static boolean isDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        if (darkness < 0.5) {
            return false; // It's a light color
        } else {
            return true; // It's a dark color
        }
    }

    public static boolean isDark(Double r, Double g, Double b) {
        if (r * 0.299 + g * 0.578 + b * 0.114 >= 192) { //浅色
            return false;
        } else {  //深色
            return true;
        }
    }

    public static Bitmap createColorBitmap(String color) {
        Bitmap bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.parseColor(color));
        return bmp;
    }

    public static String toHexColor(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    public static int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;

        final float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRatio);
        final float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRatio);
        final float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRatio);
        final float a = (Color.alpha(color1) * ratio) + (Color.alpha(color2) * inverseRatio);

        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }
}

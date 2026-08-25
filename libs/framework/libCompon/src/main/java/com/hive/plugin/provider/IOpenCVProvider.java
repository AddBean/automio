// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Pair;

import com.hive.plugin.IComponentProvider;

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
public interface IOpenCVProvider extends IComponentProvider {

    Point findColor(Bitmap bmp, int color, int threshold);

    Point[] findColors(Bitmap bmp, int color, int threshold);

    Rect[] findColorToRect(Bitmap bmp, int color, int threshold);

    Rect findImage(Bitmap template, Bitmap dest, Double desiredAccuracy);
}

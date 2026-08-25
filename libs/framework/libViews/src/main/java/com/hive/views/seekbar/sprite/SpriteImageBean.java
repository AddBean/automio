// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar.sprite;

import android.graphics.Bitmap;

import androidx.annotation.Keep;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 2022/9/21
 */
@Keep
public class SpriteImageBean {

    public String url;

    public String path;

    public int startTime;

    public int endTime;

    public int minX;

    public int minY;

    public int maxX;

    public int maxY;

    public WeakReference<Bitmap> refBitmap;

    public List<SpriteFrameBean> frames;

}

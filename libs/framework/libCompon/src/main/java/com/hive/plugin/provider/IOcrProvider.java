// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.provider;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import com.hive.plugin.IComponentProvider;
import com.hive.plugin.ocr.IOcrResultListener;

import java.util.List;

/**
 * @author jiadou
 * @email 172111432@qq.com
 * @date 6/11/21
 */
public interface IOcrProvider extends IComponentProvider {

    /**
     * 查找文字位置
     *
     * @param bmp
     * @param type     0:包含 1:精确匹配
     * @param text
     * @param regions
     * @param listener
     */
    void findText(Bitmap bmp, int type, List<String> text, List<Rect> regions, IOcrResultListener listener);

    void readText(Bitmap bmp, List<Rect> regions, IOcrResultListener regionListener, IOcrResultListener finalListener);

}

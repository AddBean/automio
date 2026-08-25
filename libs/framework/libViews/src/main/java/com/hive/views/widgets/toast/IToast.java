// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.widgets.toast;


import android.content.Context;
import android.view.View;

import java.lang.reflect.InvocationTargetException;

/**
 * @Description 自定义Toast接口
 * @Author Andy.fang
 * @CreateDate 2019-09-10 18:37
 */
public interface IToast {
    /**
     * 设置默认的 NextView
     *
     * @param context Context
     * @param text    CharSequence
     */
    void setDefaultNextView(Context context, CharSequence text);

    /**
     * 自定义Toast 展示
     */
    void show() throws Exception;

    /**
     * 设置Toast gravity,和偏移
     *
     * @param gravity int
     * @param xOffset int
     * @param yOffset int
     */
    void setGravity(int gravity, int xOffset, int yOffset);

    /**
     * 设置文本margin
     *
     * @param horizontalMargin float
     * @param verticalMargin   float
     */
    void setMargin(float horizontalMargin, float verticalMargin);

    /**
     * 设置Toast 消息时长
     *
     * @param duration int
     */
    void setDuration(int duration);

    /**
     * 与show相反，同时清理Toast队列
     */
    void cancel();

    /**
     * 获取 NextView
     *
     * @return View
     */
    View getNextView();

    /**
     * 设置 NextView
     *
     * @param view View
     */
    void setNextView(View view);
}

// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.cache;

import android.content.Context;

/**
 * 业务无关外部禁止使用
 * Created by kuaigeng01 on 2018/4/24.
 */
public class LabSp extends BaseSPTools {
    private final static String SP_NAME = "bobo_inner_sp";


    // 键盘高度
    public static final String KG_SOFT_KEYBOARD_WINDOW_HEIGHT = "kg_soft_keyboard_window_height";
    /**
     * 下载目录
     */
    public static final String SETTING_DOWNLOAD_DIRECTORY = "setting_download_directory";

    /**
     * 是否支持无损带透明的 webp {type int : 0 还没检测过, 1: 支持, -1:不支持}
     */
    public static final String SupportLosslessAndTransparentWebp = "webpSupport";


    public LabSp(Context context, String spName) {
        super(context, spName);
    }

    private LabSp(Context context) {
        super(context, SP_NAME);
    }

    private static LabSp instance;

    public static LabSp getInstance(Context context) {
        if (null == instance) {
            synchronized (LabSp.class) {
                if (null == instance) {
                    instance = new LabSp(context);
                }
            }
        }

        return instance;
    }
}

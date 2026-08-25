// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import com.hive.utils.cache.BaseSPTools;

public class DefaultSPTools extends BaseSPTools {

    private final static String SP_NAME = "SPTools";

    public final static String APP_GLOBAL_PERMISSION_GRANT = "app.global.permission.grant";
    public final static String CONFIG_SELECTED_DOMAIN_DATA = "config_domain_data";
    public final static String CONFIG_SELECTED_DOMAIN_STATISTIC = "config_domain_statistic";
    public final static String CONFIG_SELECTED_DOMAIN_OTHER = "config_domain_other";
    public final static String CONFIG_SELECTED_DOMAIN_RES = "config_domain_res";


    /**
     * 锁屏是否显示过
     */
    public static final String SCREEN_LOCK_SHOW_FLAG = "screen_lock_show_flag";


    private DefaultSPTools() {
        super(GlobalApp.sContext, SP_NAME);
    }

    private static class SingleHolder {
        static DefaultSPTools instance = new DefaultSPTools();
    }

    public static DefaultSPTools getInstance() {
        if (null == SingleHolder.instance) {
            synchronized (DefaultSPTools.class) {
                if (null == SingleHolder.instance) {
                    SingleHolder.instance = new DefaultSPTools();
                }
            }
        }

        return SingleHolder.instance;
    }
}
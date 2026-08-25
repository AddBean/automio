// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.global;

import com.hive.net.ApiDnsManager;
import com.hive.utils.BaseConfig;
import com.hive.utils.DefaultSPTools;
import com.hive.utils.DomainChecker;

import java.util.Arrays;
import java.util.List;

public class UrlConfigHelper {

    public static boolean sEnableUpdate = true;


    public static void init() {
        ApiDnsManager.sDataUrl = BaseConfig.DATA_URL;
        ApiDnsManager.sStatisticUrl = BaseConfig.STATISTIC_URL;
        ApiDnsManager.sOtherUrl = BaseConfig.OTHER_URL;
        ApiDnsManager.sResUrl = BaseConfig.RES_URL;
    }

    public static void updateBaseUrl() {
        if (!sEnableUpdate) return;
        ApiDnsManager.sDataUrl = getDefaultUrl(0);
        ApiDnsManager.sStatisticUrl = getDefaultUrl(1);
        ApiDnsManager.sOtherUrl = getDefaultUrl(2);
        ApiDnsManager.sResUrl = getDefaultUrl(3);
        List<String> dataUrls = GlobalConfig.getInstance().getListObject(GlobalConfig.CONFIG_DOMAIN_DATA, String.class, Arrays.asList(BaseConfig.DATA_URL));
        List<String> statisticUrls = GlobalConfig.getInstance().getListObject(GlobalConfig.CONFIG_DOMAIN_STATISTIC, String.class, Arrays.asList(BaseConfig.STATISTIC_URL));
        List<String> otherUrls = GlobalConfig.getInstance().getListObject(GlobalConfig.CONFIG_DOMAIN_OTHER, String.class, Arrays.asList(BaseConfig.OTHER_URL));
        List<String> resUrls = GlobalConfig.getInstance().getListObject(GlobalConfig.CONFIG_DOMAIN_RES, String.class, Arrays.asList(BaseConfig.RES_URL));
        new DomainChecker(dataUrls, 0).start();
        new DomainChecker(statisticUrls, 1).start();
        new DomainChecker(otherUrls, 2).start();
        new DomainChecker(resUrls, 3).start();
    }

    public static boolean isUrlConfigCharged(GlobalConfigModel data) {
        String oldConfig = GlobalConfig.getInstance().getString(GlobalConfig.CONFIG_DOMAIN_DATA, "") +
                GlobalConfig.getInstance().getString(GlobalConfig.CONFIG_DOMAIN_STATISTIC, "") +
                GlobalConfig.getInstance().getString(GlobalConfig.CONFIG_DOMAIN_OTHER, "") +
                GlobalConfig.getInstance().getString(GlobalConfig.CONFIG_DOMAIN_RES, "");
        String newConfig = data.get(GlobalConfig.CONFIG_DOMAIN_DATA) +
                data.get(GlobalConfig.CONFIG_DOMAIN_STATISTIC) +
                data.get(GlobalConfig.CONFIG_DOMAIN_OTHER) +
                data.get(GlobalConfig.CONFIG_DOMAIN_RES);
        if (newConfig != null)
            return !newConfig.equals(oldConfig);
        return false;
    }

    /**
     * 获取一个随机域名；
     *
     * @return
     */
    public static String getDefaultUrl(int type) {
        switch (type) {
            case 0:
                return DefaultSPTools.getInstance().getString(DefaultSPTools.CONFIG_SELECTED_DOMAIN_DATA, BaseConfig.DATA_URL);
            case 1:
                return DefaultSPTools.getInstance().getString(DefaultSPTools.CONFIG_SELECTED_DOMAIN_STATISTIC, BaseConfig.STATISTIC_URL);
            case 2:
                return DefaultSPTools.getInstance().getString(DefaultSPTools.CONFIG_SELECTED_DOMAIN_OTHER, BaseConfig.OTHER_URL);
            case 3:
                return DefaultSPTools.getInstance().getString(DefaultSPTools.CONFIG_SELECTED_DOMAIN_RES, BaseConfig.RES_URL);
            default:
                return BaseConfig.DATA_URL;
        }
    }
}

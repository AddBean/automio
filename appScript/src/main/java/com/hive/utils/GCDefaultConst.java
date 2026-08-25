// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import com.hive.config.BuildConfigHelper;
import com.hive.global.GlobalConfig;
import com.hive.net.ApiDnsManager;
import com.hive.net.data.HomeTabs;
import com.hive.utils.utils.GsonHelper;
import java.util.ArrayList;
import java.util.List;

public class GCDefaultConst {
    public static int CONFIG_API_CACHE_TIME_VALUE = 64000;//缓存时间秒
    public static String CONFIG_DOMAIN_POOLS_VALUE = "[\"" + ApiDnsManager.getResDomain() + "\"]";
    private static int sAppStyle = -1;

    /**
     * 获取默认tab栏
     * 优先从 Gradle 配置读取 mainTabs，支持自定义顺序和显隐
     *
     * @return
     */
    public static List<HomeTabs> getDefaultTabs() {
        List<HomeTabs> defaultTabs = new ArrayList<>();

        // 开源版只开放本地资源、Agent 与设置页面，忽略历史配置中的未知页面。
        String mainTabsConfig = BuildConfigHelper.getMapString("mainTabs");
        if (mainTabsConfig != null && !mainTabsConfig.isEmpty()) {
            String[] tags = mainTabsConfig.split(",");
            for (String tag : tags) {
                String normalizedTag = tag.trim();
                if ("f1".equals(normalizedTag)) {
                    continue;
                }
                HomeTabs tab = createTabByTag(normalizedTag);
                if (tab != null) {
                    defaultTabs.add(tab);
                }
            }
        }

        // 如果配置为空，使用默认配置
        if (defaultTabs.isEmpty()) {
            // 开源版主 Tab：智能体 → 工作流 → 个人。
            defaultTabs.add(createTabByTag("f2"));
            defaultTabs.add(createTabByTag("f3"));
            defaultTabs.add(createTabByTag("f4"));
        }

        return defaultTabs;
    }

    /**
     * 根据 tag 创建 HomeTabs
     */
    private static HomeTabs createTabByTag(String tag) {
        HomeTabs tab = new HomeTabs();
        tab.setTag(tag);
        tab.setOpen(false);
        tab.setEnable(true);

        // 根据 tag 设置名称
        String name;
        switch (tag) {
            case "f2":
                name = GlobalApp.getString(com.hive.i8n.R.string.design_nav_agent);
                break;
            case "f3":
                name = GlobalApp.getString(com.hive.i8n.R.string.design_nav_workflow);
                break;
            case "f4":
                name = GlobalApp.getString(com.hive.i8n.R.string.design_nav_profile);
                break;
            default:
                return null;  // 未知的 tag，返回 null
        }
        tab.setName(name);
        return tab;
    }


    /**
     * 横版风格
     *
     * @return
     */
    public static boolean isHorStyle() {
        if (sAppStyle == -1)
            sAppStyle = GlobalConfig.getInstance().getInt(GCConst.CONFIG_APP_STYLE, -1);
        if (sAppStyle == -1) {
            sAppStyle = BuildConfigHelper.getMapInteger("appStyle");
        }
        return sAppStyle == 1;
    }

    public static List<String> getDomainPools() {
        return GsonHelper.getInstance().fromListJson(CONFIG_DOMAIN_POOLS_VALUE, String.class);
    }


}

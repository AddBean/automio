// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.config;

import com.google.gson.annotations.SerializedName;
import com.hive.global.GlobalConfig;

import java.util.List;
import com.hive.annotation.NotProguard;
@NotProguard
public class ConfigWebBlockConfig {

    @SerializedName("urls")
    private List<String> urls;
    @SerializedName("ids")
    private List<String> ids;

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public static ConfigWebBlockConfig read() {
        ConfigWebBlockConfig ConfigWebBlockConfig = GlobalConfig.getInstance().getObject(GlobalConfig.CONFIG_WEB_BLOCKS, ConfigWebBlockConfig.class, null);
        return ConfigWebBlockConfig;
    }
}

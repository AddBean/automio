// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.config;

import com.google.gson.annotations.SerializedName;
import com.hive.annotation.NotProguard;
import com.hive.global.GlobalConfig;

import java.util.List;
import com.hive.annotation.NotProguard;
@NotProguard
public class ConfigAppBase {

    @SerializedName("crashEnable")
    private boolean crashEnable=false;

    public boolean isCrashEnable() {
        return crashEnable;
    }

    public void setCrashEnable(boolean crashEnable) {
        this.crashEnable = crashEnable;
    }

    public static ConfigAppBase read() {
        ConfigAppBase configAppBase = GlobalConfig.getInstance().getObject(GlobalConfig.CONFIG_APP_BASE, ConfigAppBase.class, new ConfigAppBase());
        return configAppBase;
    }
}

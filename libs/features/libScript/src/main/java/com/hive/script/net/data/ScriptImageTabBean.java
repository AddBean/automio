// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.net.data;

import androidx.annotation.Keep;

import com.hive.annotation.NotProguard;

import java.util.List;

@Keep
@NotProguard
public class ScriptImageTabBean {
    String tabName;

    int type = 0;//0:最近，1：收藏，2：自定义
    List<ScriptImageBean> images;

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public List<ScriptImageBean> getImages() {
        return images;
    }

    public void setImages(List<ScriptImageBean> images) {
        this.images = images;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

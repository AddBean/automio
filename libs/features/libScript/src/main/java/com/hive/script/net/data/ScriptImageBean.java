// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.script.net.data;

import androidx.annotation.Keep;

import com.hive.annotation.NotProguard;

@Keep
@NotProguard
public class ScriptImageBean {
    String path;

    String tabName;

    int type = 0;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public ScriptImageBean copy() {
        ScriptImageBean bean = new ScriptImageBean();
        bean.setPath(path);
        bean.setTabName(tabName);
        bean.setType(type);
        return bean;
    }
}

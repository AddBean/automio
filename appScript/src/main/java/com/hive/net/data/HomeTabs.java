// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.data;

import com.google.gson.annotations.SerializedName;

public class HomeTabs {


    /**
     * name :
     * tag :
     * icon :
     * url :
     */
    @SerializedName("name")
    private String name;
    @SerializedName("tag")
    private String tag;
    @SerializedName("view")
    private String view;
    @SerializedName("des")
    private String des;
    @SerializedName("plugin")
    private String plugin;
    @SerializedName("icon")
    private String icon;
    @SerializedName("enable")
    private boolean enable;
    @SerializedName("open")
    private boolean open;

    @SerializedName("obj")
    private String obj;

    public HomeTabs() {
    }

    public String getObj() {
        return obj;
    }

    public void setObj(String obj) {
        this.obj = obj;
    }

    public HomeTabs(String tag) {
        this.tag = tag;
    }

    public String getView() {
        return view;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public void setView(String view) {
        this.view = view;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }
}

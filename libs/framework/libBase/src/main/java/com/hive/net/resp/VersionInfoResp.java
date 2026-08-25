// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.resp;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import com.hive.annotation.NotProguard;
@NotProguard
public class VersionInfoResp implements Serializable {

    @SerializedName("id")
    private int id;
    @SerializedName("downloadUrl")
    private String downloadUrl;
    @SerializedName("md5")
    private String md5;
    @SerializedName("name")
    private String name;
    @SerializedName("detail")
    private String detail;
    @SerializedName("type")
    private int type;
    @SerializedName("enable")
    private boolean enable=false;
    @SerializedName("verCode")
    private String verCode;
    @SerializedName("verName")
    private String verName;
    @SerializedName("ex")
    private String ex;
    @SerializedName("channel")
    private String channel;
    @SerializedName("upgradeType")
    private String upgradeType;
    @SerializedName("upgradeLink")
    private String upgradeLink;
    @SerializedName("addTime")
    private long addTime;
    @SerializedName("updateTime")
    private long updateTime;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getVerCode() {
        return verCode;
    }

    public void setVerCode(String verCode) {
        this.verCode = verCode;
    }

    public String getVerName() {
        return verName;
    }

    public void setVerName(String verName) {
        this.verName = verName;
    }

    public String getEx() {
        return ex;
    }

    public void setEx(String ex) {
        this.ex = ex;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUpgradeType() {
        return upgradeType;
    }

    public void setUpgradeType(String upgradeType) {
        this.upgradeType = upgradeType;
    }

    public String getUpgradeLink() {
        return upgradeLink;
    }

    public void setUpgradeLink(String upgradeLink) {
        this.upgradeLink = upgradeLink;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}


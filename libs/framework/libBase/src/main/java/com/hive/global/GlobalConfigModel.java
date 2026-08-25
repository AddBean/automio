// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.global;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.hive.annotation.NotProguard;
import com.hive.utils.GlobalApp;
import com.hive.utils.GzipUtil;
import com.hive.utils.debug.DLog;
import com.hive.utils.utils.GsonHelper;
import com.hive.utils.utils.PreferencesUtils;

import java.util.List;
@NotProguard
public class GlobalConfigModel {
    private static GlobalConfigModel sConfigCache;
    private static final String SAVE_NAME = "GlobalConfigModel";

    @SerializedName("code")
    @Expose
    private int code;

    @SerializedName("msg")
    @Expose
    private String msg;

    @SerializedName("data")
    public List<ConfigListBean> data;

    @SerializedName("encodeData")
    public String dataEncode;

    public synchronized void save() {
        PreferencesUtils.saveObj(GlobalApp.sContext, SAVE_NAME, this, null);
        sConfigCache = this;
        DLog.d(this);
    }

    public synchronized static GlobalConfigModel restore() {
        GlobalConfigModel config = new GlobalConfigModel();
        config.save();
        return sConfigCache;
    }

    public synchronized static GlobalConfigModel read() {
        if (sConfigCache != null) return sConfigCache;
        GlobalConfigModel config = PreferencesUtils.getObj(GlobalApp.sContext, SAVE_NAME, GlobalConfigModel.class, null);
        if (config == null) {
            config = new GlobalConfigModel();
        }
        config.save();
        return sConfigCache;
    }

    public String get(String key) {
        if (data == null || key == null) return null;
        for (int i = 0; i < data.size(); i++) {
            if (key.equals(data.get(i).key)) {
                return data.get(i).value;
            }
        }
        return null;
    }

    public void decode() {
        if (dataEncode != null) {
            String json = GzipUtil.uncompress(dataEncode);
            data = GsonHelper.getInstance().fromListJson(json, ConfigListBean.class);
        }
    }

    public List<ConfigListBean> getData() {
        return data;
    }

    public void setData(List<ConfigListBean> data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
    @NotProguard
    public static class ConfigListBean {
        @SerializedName("keyName")
        private String key;
        @SerializedName("keyValue")
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    @Override
    public String toString() {
        return GsonHelper.getInstance().toFormatJson(data);
    }
}

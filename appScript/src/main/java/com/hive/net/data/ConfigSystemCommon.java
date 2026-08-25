// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.data;

import com.google.gson.annotations.SerializedName;
import com.hive.global.GlobalConfig;
import com.hive.utils.GCConst;

public class ConfigSystemCommon {
    /**
     * freeWatch : {"freeEnable":true,"freeTime":60}
     */
    @SerializedName("freeWatch")
    private FreeWatchBean freeWatch;

    @SerializedName("isCompatPad")
    private boolean isCompatPad = true;

    @SerializedName("downloadPlayType")
    private int downloadPlayType = 0;

    public int getDownloadPlayType() {
        return downloadPlayType;
    }

    public void setDownloadPlayType(int downloadPlayType) {
        this.downloadPlayType = downloadPlayType;
    }

    public FreeWatchBean getFreeWatch() {
        return freeWatch;
    }

    public void setFreeWatch(FreeWatchBean freeWatch) {
        this.freeWatch = freeWatch;
    }


    public boolean isCompatPad() {
        return isCompatPad;
    }

    public void setCompatPad(boolean compatPad) {
        isCompatPad = compatPad;
    }

    public static class FreeWatchBean {
        /**
         * freeEnable : true
         * freeTime : 60
         */
        @SerializedName("freeEnable")
        private boolean freeEnable;
        @SerializedName("freeTime")
        private long freeTime;
        @SerializedName("freeClose")
        private int freeClose;
        @SerializedName("freeType")
        private int freeType;

        public int getFreeClose() {
            return freeClose;
        }

        public void setFreeClose(int freeClose) {
            this.freeClose = freeClose;
        }

        public int getFreeType() {
            return freeType;
        }

        public void setFreeType(int freeType) {
            this.freeType = freeType;
        }

        public boolean isFreeEnable() {
            return freeEnable;
        }

        public void setFreeEnable(boolean freeEnable) {
            this.freeEnable = freeEnable;
        }

        public long getFreeTime() {
            return freeTime;
        }

        public void setFreeTime(long freeTime) {
            this.freeTime = freeTime;
        }
    }

    public static ConfigSystemCommon getConfig() {
        return GlobalConfig.getInstance().getObject(GCConst.CONFIG_SYSTEM_COMMON, ConfigSystemCommon.class, new ConfigSystemCommon());
    }
}

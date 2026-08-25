// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.resp;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import com.hive.annotation.NotProguard;
@NotProguard
public class UploadResp {

    @SerializedName("code")
    @Expose
    private int code;

    @SerializedName("msg")
    @Expose
    private String msg;

    @SerializedName("data")
    @Expose
    private List<UploadRespBean> data;

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

    public List<UploadRespBean> getData() {
        return data;
    }

    public void setData(List<UploadRespBean> data) {
        this.data = data;
    }

    public static class UploadRespBean {
        @SerializedName("name")
        String name;
        @SerializedName("path")
        String path;
        @SerializedName("md5")
        String md5;
        @SerializedName("ex")
        String ex;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getEx() {
            return ex;
        }

        public void setEx(String ex) {
            this.ex = ex;
        }
    }
}

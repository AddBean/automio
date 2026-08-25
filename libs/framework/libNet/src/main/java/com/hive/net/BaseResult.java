// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.hive.annotation.NotProguard;

/**
 * 后端返回数据
 * Created by gzg on 2016/8/26.
 */
@NotProguard
public class BaseResult<T> {

    @SerializedName("code")
    @Expose
    private int code;

    @SerializedName("msg")
    @Expose
    private String msg;

    @SerializedName("data")
    @Expose
    private T data;

    @SerializedName("_t")
    @Expose
    private long time;



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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

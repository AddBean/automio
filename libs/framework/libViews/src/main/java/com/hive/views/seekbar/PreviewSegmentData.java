// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.seekbar;

import androidx.annotation.Keep;

import com.google.gson.Gson;


/**
 * @author jiadou
 * @date 2022/9/23
 */
@Keep
public class PreviewSegmentData {
    private boolean isCanReplace;
    private float inPoint;
    private float outPoint;
    private Object obj;
    private String msg;

    public PreviewSegmentData(boolean isCanReplace, float inPoint, float outPoint, String msg, Object obj) {
        this.isCanReplace = isCanReplace;
        this.inPoint = inPoint;
        this.outPoint = outPoint;
        this.msg = msg;
        this.obj = obj;
    }

    public boolean isCanReplace() {
        return isCanReplace;
    }

    public void setCanReplace(boolean canReplace) {
        isCanReplace = canReplace;
    }

    public float getInPoint() {
        return inPoint;
    }

    public void setInPoint(float inPoint) {
        this.inPoint = inPoint;
    }

    public float getOutPoint() {
        return outPoint;
    }

    public void setOutPoint(float outPoint) {
        this.outPoint = outPoint;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public PreviewSegmentData copy() {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(this), PreviewSegmentData.class);
    }

    public boolean equals(PreviewSegmentData data) {
        Gson gson = new Gson();
        return gson.toJson(data).equals(gson.toJson(this));
    }
}

// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.fragment;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import com.hive.annotation.NotProguard;
@NotProguard
public class PagerTag implements Serializable {
    @SerializedName("name")
    public String name;
    @SerializedName("position")
    public int position;
    @SerializedName("obj")
    public Object obj;
    public Object tag;

    public PagerTag(String name, Object obj) {
        this.name = name;
        this.obj = obj;
    }
    public PagerTag(String name, int position, Object obj) {
        this.name = name;
        this.position = position;
        this.obj = obj;
    }

    public PagerTag(String name, int position, Object obj, Object tag) {
        this.name = name;
        this.position = position;
        this.obj = obj;
        this.tag = tag;
    }
}

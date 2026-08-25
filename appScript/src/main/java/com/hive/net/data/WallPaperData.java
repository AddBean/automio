// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.data;

import androidx.annotation.Keep;

import com.hive.annotation.NotProguard;

@Keep
@NotProguard
public class WallPaperData {
    public String path;

    public int type = 0;//-1:加号，0:本地
    public Boolean isSelected;

    public WallPaperData(String path) {
        this.path = path;
    }

    public WallPaperData(String path, int type, Boolean isSelected) {
        this.path = path;
        this.type = type;
        this.isSelected = isSelected;
    }

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

    public Boolean getSelected() {
        return isSelected;
    }

    public void setSelected(Boolean selected) {
        isSelected = selected;
    }
}

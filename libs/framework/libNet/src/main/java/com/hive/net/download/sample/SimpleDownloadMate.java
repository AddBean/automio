// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.download.sample;

/**
 * Created by Admin on 2018/8/25.
 */
public class SimpleDownloadMate {

    public long completeSize;
    public long startPos;
    public long endPos;
    public long dlTime;
    public int state;//0取消，1下载中，2暂停；
    public String name;

    public SimpleDownloadMate() {
    }

    public SimpleDownloadMate(String name, int state, long mCompleteSize, long mStartPos, long mEndPos, long dlTime) {
        this.name = name;
        this.state = state;
        this.completeSize = mCompleteSize;
        this.startPos = mStartPos;
        this.endPos = mEndPos;
        this.dlTime = dlTime;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDlTime() {
        return dlTime;
    }

    public void setDlTime(long dlTime) {
        this.dlTime = dlTime;
    }

    public long getCompleteSize() {
        return completeSize;
    }

    public void setCompleteSize(long completeSize) {
        this.completeSize = completeSize;
    }

    public long getStartPos() {
        return startPos;
    }

    public void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    public void setEndPos(long endPos) {
        this.endPos = endPos;
    }
}

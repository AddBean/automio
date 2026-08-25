// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.upload;

/**
 * Created by Admin on 2016/6/25.
 */
public abstract class IUploadListener {
    public void onAllUploadProgress(int index, String fileName, long fileCurLen, long fileLen) {

    }

    public void onSingleUploadSuccess(int index,String fileName) {
    }

    public void onSingleUploadProgress(int index, String fileName, long fileCurLen, long fileLen) {

    }

    public abstract void onAllUploadSuccess(String content);

    public void onAllUploadFailed(String msg) {
    }
}

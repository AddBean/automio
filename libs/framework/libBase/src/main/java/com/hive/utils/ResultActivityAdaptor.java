// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.SparseArray;

public class ResultActivityAdaptor {
    private static final int REQUEST_CODE_START = 20000;
    private SparseArray requests = new SparseArray();
    private Activity mActivity;
    private int currentReqCode = 20000;

    public ResultActivityAdaptor(Activity activity) {
        this.mActivity = activity;
    }

    public void startActivityForResult(Intent i, ResultActivityListener listener) {
        ++this.currentReqCode;
        this.requests.put(this.currentReqCode, listener);
        this.mActivity.startActivityForResult(i, this.currentReqCode);
    }

    public boolean onResult(int requestCode, int resultCode, Intent data) {
        ResultActivityListener listener = (ResultActivityListener) this.requests.get(requestCode);
        if (listener != null) {
            listener.onResult(requestCode, resultCode, data);
            this.requests.remove(requestCode);
            return true;
        } else {
            return false;
        }
    }

    public interface ResultActivityListener {
        void onResult(int requestCode, int resultCode, Intent data);
    }
}

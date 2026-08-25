// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.download.sample;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by Admin on 2016/6/2.
 */
public class SimpleDownloadTask extends AsyncTask<Void, Float, Boolean> implements SimpleDownloader.OnDownloadListener {
    protected Context mContext;
    protected String mUrl;
    protected String mPath;

    public SimpleDownloadTask(Context context, String mUrl, String mPath) {
        super();
        this.mPath = mPath;
        this.mUrl = mUrl;
        this.mContext = context;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        SimpleDownloader sampleDownloader = new SimpleDownloader(mContext);
        try {
            sampleDownloader.downloadToFile(mUrl, mPath, this);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    @Override
    public void onDownloadUpdate(String saveName,long var1, long var3) {
        this.publishProgress(((float) var1 / (float) var3));
    }

    @Override
    public boolean onFileExist(String fileName,boolean isExist) {
        return false;
    }

    public String getFileName() {
        return mPath;
    }
}

// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.download.sample;

import android.content.Context;
import com.hive.utils.file.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Admin on 2016/2/13.
 */
public class SimpleDownloader {
    private Context mContext;
    private Boolean isStoped = false;

    private static SimpleDownloader sInstance;

    public static SimpleDownloader getInstance(Context context) {
        if (null == sInstance) {
            synchronized (SimpleDownloader.class) {
                if (null == sInstance) {
                    sInstance = new SimpleDownloader(context);
                }
            }
        }
        return sInstance;
    }

    public static SimpleDownloader newInstance(Context context) {
        return new SimpleDownloader(context);
    }

    public SimpleDownloader(Context mContext) {
        this.mContext = mContext;
    }

    public long downloadToFile(String uri, String path, OnDownloadListener downloadListener) throws Exception {
        isStoped = false;
        URLConnection urlConnection = null;
        BufferedInputStream bis = null;
        long fileLen = 0;
        long currCount = 0;
        try {
            File file = new File(path);
            if (downloadListener != null && downloadListener.onFileExist(path, file.exists()))
                return 0;
            FileUtils.makeDirs(path);
            if (uri.startsWith("/")) {
                FileInputStream fileInputStream = new FileInputStream(uri);
                fileLen = fileInputStream.available();
                bis = new BufferedInputStream(fileInputStream);
            } else if (uri.startsWith("assets/")) {
                InputStream inputStream = mContext.getAssets().open(uri.substring(7, uri.length()));
                fileLen = inputStream.available();
                bis = new BufferedInputStream(inputStream);

            } else {
                final URL url = new URL(uri);
                urlConnection = url.openConnection();
                urlConnection.setConnectTimeout(this.getDefaultConnectTimeout());
                urlConnection.setReadTimeout(this.getDefaultReadTimeout());
                bis = new BufferedInputStream(urlConnection.getInputStream());
                fileLen = urlConnection.getContentLength();
            }
            byte[] buffer = new byte[4096];
            int len = 0;
            FileOutputStream fo = new FileOutputStream(path);
            BufferedOutputStream out = new BufferedOutputStream(fo);
            while ((len = bis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                currCount += len;
                if (isStoped) {
                    throw new InterruptedException();
                }
                if (downloadListener != null) {
                    downloadListener.onDownloadUpdate(path, currCount, fileLen);
                }
            }
            out.flush();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Exception(mContext.getString(com.hive.i8n.R.string.download_error));
        } finally {
            try {
                if (bis != null)
                    bis.close();
            } catch (IOException e) {
                throw new Exception(mContext.getString(com.hive.i8n.R.string.download_error));
            }
            isStoped = true;
        }
        return 1;
    }

    private int getDefaultReadTimeout() {
        return 200 * 1000;
    }


    private int getDefaultConnectTimeout() {
        return 200 * 1000;
    }

    public synchronized void stop() {
        synchronized (isStoped) {
            isStoped = true;
        }
    }

    public interface OnDownloadListener {
        void onDownloadUpdate(String saveName, long var1, long var3);

        boolean onFileExist(String saveName, boolean isExist);

    }

}

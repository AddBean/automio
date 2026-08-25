// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.views.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import androidx.core.app.NotificationCompat;

import com.hive.utils.GlobalApp;
import com.hive.utils.utils.NotificationUtils;
import com.hive.views.R;

public class NotificationProgressHelper {
    private static NotificationManager sManager;
    private NotificationCompat.Builder mBuilder;
    private int NOTIFY_ID = 10001;

    private String mDownloadingContentTitle = "";
    private String mDownloadingContentText = "";
    private PendingIntent mPenddingIntent;
    private Object mData;

    public static NotificationProgressHelper newInstance(int id) {
        NotificationProgressHelper helper = new NotificationProgressHelper();
        helper.NOTIFY_ID = id;
        return helper;
    }

    public NotificationProgressHelper setDownloadingContentTitle(String title) {
        mDownloadingContentTitle = title;
        return this;
    }

    public NotificationProgressHelper setDownloadingContentText(String text) {
        mDownloadingContentText = text;
        return this;
    }

    public NotificationProgressHelper setPendingIntent(PendingIntent mPenddingIntent) {
        this.mPenddingIntent = mPenddingIntent;
        return this;
    }

    public Object getData() {
        return mData;
    }

    public void setData(Object mData) {
        this.mData = mData;
    }

    /**
     * 初始化通知栏;
     */
    public NotificationProgressHelper build() {
        mBuilder = NotificationUtils.createNotificationBuilder(GlobalApp.sContext, NotificationUtils.Notification_TIPS_Category);
        mBuilder.setSmallIcon(com.hive.i8n.R.drawable.logo);
        mBuilder.setContentTitle(mDownloadingContentTitle);
        mBuilder.setContentText(mDownloadingContentText);
        if (mPenddingIntent != null)
            mBuilder.setContentIntent(mPenddingIntent);
        if (sManager == null) {
            sManager = (NotificationManager) GlobalApp.sContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        mBuilder.setProgress(100, 0, false);
        return this;
    }


    /**
     * 通知进度条更改；
     */
    public void notifyDownloadProgress(float process) {
        if (mBuilder != null) {
            mBuilder.setProgress(100, (int) (process * 100), false);
            sManager.notify(NOTIFY_ID, mBuilder.build());
        }
    }

    /**
     * 通知进度条更改；
     */
    public void notifyDownloadComplete() {
        if (sManager != null) {
            sManager.cancel(NOTIFY_ID);
        }
    }

}

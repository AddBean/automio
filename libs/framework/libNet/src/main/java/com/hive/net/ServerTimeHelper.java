// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import android.text.format.Time;

import com.hive.utils.debug.DLog;
import com.hive.utils.global.SPTools;

import java.util.Date;

public class ServerTimeHelper {

    private static final String SP_KEY_TIME_GAP = "spKeyTimeSyncGap";
    private static final String SP_KEY_SERVER_TIME = "spKeyTimeSyncServerTime";
    private static final String SP_KEY_TIME_MILLI_SECOND_GAP = "spKeyTimeMilliSecondSyncGap";

    private static volatile long mServerTimeStamp;
    private static volatile long mTimeGap;

    private static volatile long mTimeMillisecondGap;

    private static volatile boolean hasSyncTime = false;
    private static volatile boolean isWaitingForSync = false;

    public static void syncTimeFromLocal() {
        mServerTimeStamp = SPTools.getInstance().getLong(SP_KEY_SERVER_TIME, System.currentTimeMillis() / 1000);
        mTimeGap = SPTools.getInstance().getLong(SP_KEY_TIME_GAP, 0);
        mTimeMillisecondGap = SPTools.getInstance().getLong(SP_KEY_TIME_MILLI_SECOND_GAP, 0);
    }

    public static void onDestroy() {
        SPTools.getInstance().putLong(SP_KEY_TIME_GAP, mTimeGap);
        SPTools.getInstance().putLong(SP_KEY_SERVER_TIME, mServerTimeStamp);
        SPTools.getInstance().putLong(SP_KEY_TIME_MILLI_SECOND_GAP, mTimeMillisecondGap);
    }

    /**
     * 同步服务器时间
     *
     * @param timeStamp 秒
     */
    public static void updateServerTime(long timeStamp) {
        if (timeStamp < 1000000000) {
            return;
        }
        mServerTimeStamp = timeStamp / 1000;
        mTimeGap = System.currentTimeMillis() / 1000 - mServerTimeStamp;

        mTimeMillisecondGap = System.currentTimeMillis() - mServerTimeStamp * 1000;

        if (!hasSyncTime) {
            hasSyncTime = true;

            SPTools.getInstance().putLong(SP_KEY_TIME_GAP, mTimeGap);
            SPTools.getInstance().putLong(SP_KEY_SERVER_TIME, mServerTimeStamp);
            SPTools.getInstance().putLong(SP_KEY_TIME_MILLI_SECOND_GAP, mTimeMillisecondGap);
        }

        isWaitingForSync = false;
        if (DLog.isDebug()) {
            DLog.e("updateServerTime", new Date(mServerTimeStamp * 1000).toString());
        }
    }

    public static boolean isWaitingForSync() {
        return isWaitingForSync;

    }

    public static void setWrongClockStatus() {
        ServerTimeHelper.isWaitingForSync = true;
    }

    public static long getServerTime() {
        return System.currentTimeMillis() / 1000 - mTimeGap;
    }


    public static long getServerTimeMillis() {
        return System.currentTimeMillis() - mTimeMillisecondGap;
    }

    public static boolean isToday(long when) {
        Time time = new Time();
        time.set(when);

        int thenYear = time.year;
        int thenMonth = time.month;
        int thenMonthDay = time.monthDay;

        time.set(getServerTimeMillis());
        return (thenYear == time.year)
                && (thenMonth == time.month)
                && (thenMonthDay == time.monthDay);
    }
}

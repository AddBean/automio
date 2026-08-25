// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import com.hive.utils.GlobalApp;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class FormatUtils {
    public static String getFormatFileSize(long fileSize) {
        long kbSize = fileSize / 1024;
        if (kbSize > 0)
            fileSize = fileSize % 1024;

        long mbSize = kbSize / 1024;
        if (mbSize > 0)
            kbSize = kbSize % 1024;

        String str = "";
        if (kbSize == 0) {
            str += String.valueOf(fileSize) + "B";
            return str;
        }
        if (mbSize == 0) {
            str += String.valueOf(kbSize) + "KB";
            return str;
        }

        str += String.valueOf(mbSize) + "M" + String.valueOf(kbSize) + "KB";
        return str;
    }

    public static String getFormatSecondStr(long secondTimes) {
        long minutes = secondTimes / 60;
        if (minutes > 0)
            secondTimes = secondTimes % 60;

        long hours = minutes / 60;
        if (hours > 0)
            minutes = minutes % 60;

        String secUnit = GlobalApp.getString(com.hive.i8n.R.string.utils_time_format_sec);
        String minUnit = GlobalApp.getString(com.hive.i8n.R.string.utils_time_format_min);
        String hourUnit = GlobalApp.getString(com.hive.i8n.R.string.utils_time_format_hour);

        String str = "";
        if (minutes == 0) {
            str += String.valueOf(secondTimes) + secUnit;
            return str;
        }
        if (hours == 0) {
            str += String.valueOf(minutes) + minUnit + String.valueOf(secondTimes) + secUnit;
            return str;
        }

        str += String.valueOf(hours) + hourUnit + String.valueOf(minutes) + minUnit;
        return str;
    }

    public static String getFormatFloatStr(double f) {
        DecimalFormat format = new DecimalFormat("0.00");
        String str = format.format(f);
        return str;
    }


    private static SimpleDateFormat mSimpleDateFormatYMDHMS;

    /**
     * 格式化时间
     *
     * @param inTimeInMillis 时间戳
     * @return yyyy-MM-dd HH:mm:ss
     */
    public static String formatDateTimeYMDHMS(long inTimeInMillis) {
        if (null == mSimpleDateFormatYMDHMS) {
            mSimpleDateFormatYMDHMS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            mSimpleDateFormatYMDHMS.setTimeZone(TimeZone.getDefault());
        }

        return mSimpleDateFormatYMDHMS.format(inTimeInMillis);
    }


    private static SimpleDateFormat mSimpleDateFormatYMD;

    /**
     * 格式化时间
     *
     * @param inTimeInMillis 时间戳
     * @return yy-M-d
     */
    public static String formatDateTimeYMD(long inTimeInMillis) {
        if (null == mSimpleDateFormatYMD) {
            mSimpleDateFormatYMD = new SimpleDateFormat("yy-M-d", Locale.getDefault());
            mSimpleDateFormatYMD.setTimeZone(TimeZone.getDefault());
        }

        return mSimpleDateFormatYMD.format(inTimeInMillis);
    }

    /**
     * 获取今天日期
     *
     * @return yyyyMMdd
     */
    public static String getTodayDate() {
        Locale locale = Locale.getDefault();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", locale);
        simpleDateFormat.setTimeZone(TimeZone.getDefault());

        return simpleDateFormat.format(System.currentTimeMillis());
    }

    /**
     * 获取日期
     *
     * @return yyyyMMdd
     */
    public static String formatDateTimeYYYYMMddHHmm(long inTimeInMillis) {
        SimpleDateFormat mSimpleDateFormatYMD = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        mSimpleDateFormatYMD.setTimeZone(TimeZone.getDefault());
        return mSimpleDateFormatYMD.format(inTimeInMillis);
    }
}

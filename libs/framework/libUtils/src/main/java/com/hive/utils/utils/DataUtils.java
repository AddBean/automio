// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;

import android.text.format.Time;

/**
 * Created by lxl on 2016/10/25 0025.
 */

public class DataUtils {

//    //工具类方法，并且会频繁调用，所以尽量减少中间类的产生
//    private static SimpleDateFormat formatterForYear;
//    private static SimpleDateFormat formatterForMoth;
//
//    public static String getDataUtil2(long now, long d) {
//        try {
//            if (StringUtils.isEmpty(String.valueOf(now)) || StringUtils.isEmpty(String.valueOf(d)) || 0 == now || 0 == d) {
//                return "";
//            }
//            if (String.valueOf(now).length() < "1395905391000".length()) {
//                now = now * 1000;
//            }
//            if (String.valueOf(d).length() < "1395905391000".length()) {
//                d = d * 1000;
//            }
//            if (null == formatterForYear) {
//                formatterForYear = new SimpleDateFormat("yyyy-MM", Locale.US);
//            }
//            Date curDate1 = new Date(now);//获取当前时间
//            Date curDate2 = new Date(d);//更新时间
//
//            if (curDate1.getYear() > curDate2.getYear()) {//y
//                return formatterForYear.format(curDate2);
//            } else if (curDate1.getMonth() > curDate2.getMonth()) {//m
//                if (null == formatterForMoth) {
//                    formatterForMoth = new SimpleDateFormat("MM-dd", Locale.US);
//                }
//                String str = formatterForMoth.format(curDate2);
//                return str == null ? "" : str;
//            } else if (curDate1.getDate() > curDate2.getDate()) {
//                if (curDate1.getDate() - curDate2.getDate() == 1) {
//                    return VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.yesterday1);
//                } else {
//                    return String.format(VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.day_before1), (curDate1.getDate() - curDate2.getDate()));
//                }
//            } else if (curDate1.getHours() > curDate2.getHours()) {
//
//                return String.format(VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.hour_before1), (curDate1.getHours() - curDate2.getHours()));
//            } else if (curDate1.getMinutes() > curDate2.getMinutes()) {
//
//                return String.format(VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.minute_before1), (curDate1.getMinutes() - curDate2.getMinutes()));
//            } else {
//                return VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.just_now);
//            }
//
////            else if (curDate1.getSeconds() >= curDate2.getSeconds()) {
////
////                return String.format(Global.getGlobalContext().getString(com.hive.i8n.R.string.minute_before1), 1);
////            } else {
////                return "";
////            }
//        } catch (Exception e) {
//            return "";
//        }
//    }

//    /**
//     * 动态时间处理
//     *
//     * @param now
//     * @param d
//     * @return
//     */
//    public static String getDataUtil(long now, long d) {
////		long ld1= Long.parseLong("1395905391000");
////		long ld2= Long.parseLong("1395514448000");
//        try {
//            if (StringUtils.isEmpty(String.valueOf(now)) || StringUtils.isEmpty(String.valueOf(d)) || 0 == now || 0 == d) {
//                return "";
//            }
//            if (String.valueOf(now).length() < "1395905391000".length()) {
//                now = now * 1000;
//            }
//            if (String.valueOf(d).length() < "1395905391000".length()) {
//                d = d * 1000;
//            }
////            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");
//            SimpleDateFormat formatteryear = new SimpleDateFormat("yyyy-MM-dd");
//            Date curDate1 = new Date(now);//获取当前时间
//            Date curDate2 = new Date(d);//更新时间
//
//            if (curDate1.getYear() > curDate2.getYear()) {//y
//                return formatteryear.format(curDate2);
//            } else if (curDate1.getMonth() > curDate2.getMonth()) {//m
////                String str = formatteryear.format(curDate2);
////                if (!StringUtils.isEmpty(str) && str.indexOf("月") > 0) {
////                    return str.substring(str.indexOf("年") + 1);
////                }
//                SimpleDateFormat format = new SimpleDateFormat("MM-dd");
//                String str = format.format(curDate2);
//                return str == null ? "" : str;
////                return "";
//            } else if (curDate1.getDate() > curDate2.getDate()) {
//                if (curDate1.getDate() - curDate2.getDate() == 1) {
//                    return VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.yesterday);
////                    return "昨天";
//                } else {
////                    String str = formatteryear.format(curDate2);
////                    if (!StringUtils.isEmpty(str) && str.indexOf("月") > 0) {
////                        return str.substring(str.indexOf("年") + 1);
////                    }
//                    return String.format(VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.day_before), (curDate1.getDate() - curDate2.getDate()));
//                }
////                return "";
//            } else if (curDate1.getHours() > curDate2.getHours()) {
//                return String.format(VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.hour_before), (curDate1.getHours() - curDate2.getHours()));
////                return (curDate1.getHours() - curDate2.getHours()) + "小时前";
//            } else if (curDate1.getMinutes() > curDate2.getMinutes()) {
//
//                return String.format(VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.minute_before), (curDate1.getMinutes() - curDate2.getMinutes()));
////                return (curDate1.getMinutes() - curDate2.getMinutes()) + "分钟前";
//            } else if (curDate1.getSeconds() >= curDate2.getSeconds()) {
//                return String.format(VolleyGlobal.getGlobalContext().getString(com.hive.i8n.R.string.minute_before), 1);
////                return "1分钟前";
//            } else {
//                return "";
//            }
//        } catch (Exception e) {
//            return "";
//        }
//    }

    /**
     * second to HH:MM:ss
     *
     * @param seconds
     * @return
     */
    public static String convertSecondsToTime(long seconds) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (seconds <= 0) return "00:00";
        else {
            minute = (int) seconds / 60;
            if (minute < 60) {
                second = (int) seconds % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99) return "99:59:59";
                minute = minute % 60;
                second = (int) (seconds - hour * 3600 - minute * 60);
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    private static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10) retStr = "0" + Integer.toString(i);
        else retStr = "" + i;
        return retStr;
    }

    /**
     * 判断当前系统时间是否在指定时间的范围内
     *
     * @param beginHour 开始小时，例如22
     * @param beginMin  开始小时的分钟数，例如30
     * @param endHour   结束小时，例如 8
     * @param endMin    结束小时的分钟数，例如0
     * @return true表示在范围内，否则false
     */
    public static boolean isCurrentInTimeScope(int beginHour, int beginMin, int endHour, int endMin, long currentTime) {
        boolean result = false;
        final long currentTimeMillis = currentTime == 0 ? System.currentTimeMillis() : currentTime;

        Time now = new Time();
        now.set(currentTimeMillis);

        Time startTime = new Time();
        startTime.set(currentTimeMillis);
        startTime.hour = beginHour;
        startTime.minute = beginMin;

        Time endTime = new Time();
        endTime.set(currentTimeMillis);
        endTime.hour = endHour;
        endTime.minute = endMin;

        if (!startTime.before(endTime)) {
            // 跨天的特殊情况（比如22:00-8:00）
            startTime.set(startTime.toMillis(true) - aDayInMillis);
            result = !now.before(startTime) && !now.after(endTime); // startTime <= now <= endTime
            Time startTimeInThisDay = new Time();
            startTimeInThisDay.set(startTime.toMillis(true) + aDayInMillis);

            if (!now.before(startTimeInThisDay)) {
                result = true;
            }
        } else {
            // 普通情况(比如 8:00 - 14:00)
            result = !now.before(startTime) && !now.after(endTime); // startTime <= now <= endTime
        }
        return result;
    }

    public static boolean isDisturbMsgTimeScope() {
        return isCurrentInTimeScope(DISTURB_MESSAGE_BEGIN_HOUR, DISTURB_MESSAGE_BEGIN_MIN, DISTURB_MESSAGE_END_HOUR, DISTURB_MESSAGE_END_MIN, 0);
    }

    //免打扰时间-----start--
    public static final int DISTURB_MESSAGE_BEGIN_HOUR = 22;
    public static final int DISTURB_MESSAGE_BEGIN_MIN = 0;
    public static final int DISTURB_MESSAGE_END_HOUR = 8;
    public static final int DISTURB_MESSAGE_END_MIN = 0;
    public static final long aDayInMillis = 1000 * 60 * 60 * 24;
    //免打扰时间-----end--

    /**
     * @param positionTime 递增间隔时间
     * @return
     */
    public static long getDisturbMsgTimeAtMillis(long positionTime) {
        final long currentTimeMillis = System.currentTimeMillis();
        if (isCurrentInTimeScope(22, 0, 8, 0, currentTimeMillis)) {
            Time now = new Time();
            now.set(currentTimeMillis);

            Time endTime = new Time();
            endTime.set(currentTimeMillis);
            endTime.hour = DISTURB_MESSAGE_END_HOUR;
            endTime.minute = DISTURB_MESSAGE_END_MIN;

            if (!now.before(endTime)) {
                // 跨天的特殊情况（比如22:00-8:00）
                Time endTimeInThisDay = new Time();
                endTimeInThisDay.set(endTime.toMillis(true) + aDayInMillis);
                return endTimeInThisDay.toMillis(true) + positionTime;
            } else {
                return endTime.toMillis(true) + positionTime;
            }
        }
        return currentTimeMillis + positionTime;
    }
}

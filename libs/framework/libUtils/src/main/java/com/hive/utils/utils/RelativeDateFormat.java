// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;


import com.hive.utils.GlobalApp;
import com.hive.i8n.R;
import java.text.ParseException;

import java.text.SimpleDateFormat;

import java.util.Date;


public class RelativeDateFormat {


    private static final long ONE_MINUTE = 60000L;

    private static final long ONE_HOUR = 3600000L;

    private static final long ONE_DAY = 86400000L;

    private static final long ONE_WEEK = 604800000L;


    private static final String ONE_SECOND_AGO = GlobalApp.getString(R.string.date_to_sec);

    private static final String ONE_MINUTE_AGO = GlobalApp.getString(R.string.date_to_min);

    private static final String ONE_HOUR_AGO = GlobalApp.getString(R.string.date_to_hour);

    private static final String ONE_DAY_AGO = GlobalApp.getString(R.string.date_to_day);

    private static final String ONE_MONTH_AGO = GlobalApp.getString(R.string.date_to_month);

    private static final String ONE_YEAR_AGO = GlobalApp.getString(R.string.date_to_year);


    public static void main(String[] args) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:m:s");

        Date date = format.parse("2013-11-11 18:35:35");

        System.out.println(format(date));

    }


    public static String format(Date date) {

        long delta = new Date().getTime() - date.getTime();

        if (delta < 1L * ONE_MINUTE) {

            long seconds = toSeconds(delta);

            return String.format(ONE_SECOND_AGO, seconds <= 0 ? 1 : seconds);

        }

        if (delta < 45L * ONE_MINUTE) {

            long minutes = toMinutes(delta);

            return String.format(ONE_MINUTE_AGO, minutes <= 0 ? 1 : minutes);

        }

        if (delta < 24L * ONE_HOUR) {

            long hours = toHours(delta);

            return String.format(ONE_HOUR_AGO, hours <= 0 ? 1 : hours);

        }

        if (delta < 48L * ONE_HOUR) {

            return GlobalApp.getString(R.string.date_yestoday);

        }

        if (delta < 30L * ONE_DAY) {

            long days = toDays(delta);

            return String.format(ONE_DAY_AGO, days <= 0 ? 1 : days);

        }

        if (delta < 12L * 4L * ONE_WEEK) {

            long months = toMonths(delta);

            return String.format(ONE_MONTH_AGO, months <= 0 ? 1 : months);

        } else {

            long years = toYears(delta);

            return String.format(ONE_YEAR_AGO, years <= 0 ? 1 : years);

        }

    }


    private static long toSeconds(long date) {

        return date / 1000L;

    }


    private static long toMinutes(long date) {

        return toSeconds(date) / 60L;

    }


    private static long toHours(long date) {

        return toMinutes(date) / 60L;

    }


    private static long toDays(long date) {

        return toHours(date) / 24L;

    }


    private static long toMonths(long date) {

        return toDays(date) / 30L;

    }


    private static long toYears(long date) {

        return toMonths(date) / 365L;

    }



}
